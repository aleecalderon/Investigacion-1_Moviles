package com.example.tasktimer

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tasktimer.databinding.ActivityMainBinding
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LIFECYCLE_DEMO"
        private const val KEY_SCROLL_DEMO = "key_scroll_demo" // solo para demostrar onSaveInstanceState
    }

    private lateinit var binding: ActivityMainBinding

    // "by viewModels()" pide el ViewModel al ViewModelProvider de la Activity.
    // La primera vez lo CREA. En cualquier recreación posterior por cambio de
    // configuración (rotación, etc.) devuelve LA MISMA instancia en memoria.
    // Como el constructor de MainViewModel recibe un SavedStateHandle, el
    // factory por defecto (SavedStateViewModelFactory) se lo entrega solo,
    // reconstruyéndolo con los valores guardados si el proceso fue matado.
    private val viewModel: MainViewModel by viewModels()

    private lateinit var pendingAdapter: TaskAdapter
    private lateinit var historyAdapter: TaskAdapter

    // ==================================================================
    // onCreate(): se ejecuta SIEMPRE que la Activity se crea desde cero,
    // ya sea la primera vez que se abre la app, o después de que Android
    // la destruyó (rotación de pantalla, o el sistema mató el proceso en
    // background y el usuario vuelve a abrirla).
    //
    // Aquí NO recreamos el estado manualmente: solo conectamos la UI a los
    // StateFlow del ViewModel. Si el ViewModel ya existía (rotación), sus
    // valores ya están ahí. Si el proceso murió, SavedStateHandle ya
    // reconstruyó el ViewModel con los últimos valores guardados ANTES de
    // que este onCreate() se ejecute. En ambos casos la UI simplemente
    // "pinta" lo que el ViewModel ya tiene.
    // ==================================================================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "onCreate() -> UI creada. ViewModel activo: $viewModel")

        // Este valor NO viene del ViewModel, es solo para mostrar que
        // onSaveInstanceState/onCreate(Bundle) también se puede usar
        // directamente en la Activity para datos puramente de UI.
        val restoredScroll = savedInstanceState?.getInt(KEY_SCROLL_DEMO, 0) ?: 0
        Log.d(TAG, "onCreate() -> valor de ejemplo restaurado desde Bundle: $restoredScroll")

        setupRecyclerViews()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerViews() {
        pendingAdapter = TaskAdapter { task -> viewModel.startTask(task.id) }
        historyAdapter = TaskAdapter()

        binding.rvPendingTasks.layoutManager = LinearLayoutManager(this)
        binding.rvPendingTasks.adapter = pendingAdapter

        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = historyAdapter
    }

    private fun setupListeners() {
        binding.btnAddTask.setOnClickListener {
            val name = binding.etTaskName.text?.toString().orEmpty()
            viewModel.addTask(name)
            binding.etTaskName.text?.clear()
        }

        binding.btnStartPause.setOnClickListener {
            val active = viewModel.activeTask
            when {
                active == null -> { /* No hay tarea seleccionada; el usuario debe tocar una de la lista */ }
                viewModel.isTimerRunning -> viewModel.pauseActiveTask()
                else -> viewModel.startTask(active.id)
            }
        }

        binding.btnComplete.setOnClickListener {
            viewModel.completeActiveTask()
        }
    }

    // Recolecta los StateFlow del ViewModel de forma segura respecto al
    // ciclo de vida: repeatOnLifecycle(STARTED) pausa la recolección
    // automáticamente cuando la Activity no está visible.
    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.tasks.collect { tasks ->
                        pendingAdapter.submitList(tasks.filter { !it.isCompleted })
                        historyAdapter.submitList(tasks.filter { it.isCompleted })
                    }
                }
                launch {
                    viewModel.activeTaskId.collect { activeId ->
                        val task = viewModel.tasks.value.find { it.id == activeId }
                        binding.tvActiveTaskLabel.text =
                            task?.name ?: getString(R.string.label_no_active_task)
                        binding.btnStartPause.text =
                            if (viewModel.isTimerRunning) getString(R.string.btn_pause)
                            else getString(R.string.btn_start)
                    }
                }
                launch {
                    viewModel.elapsedMillis.collect { millis ->
                        binding.tvTimer.text = formatMillis(millis)
                        binding.btnStartPause.text =
                            if (viewModel.isTimerRunning) getString(R.string.btn_pause)
                            else getString(R.string.btn_start)
                    }
                }
            }
        }
    }

    private fun formatMillis(millis: Long): String {
        val h = TimeUnit.MILLISECONDS.toHours(millis)
        val m = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        val s = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    // ==================================================================
    // onResume(): la Activity vuelve a primer plano y es totalmente
    // visible e interactiva. Esto pasa tanto al abrir la app por primera
    // vez como al volver de background (el usuario la minimizó y regresó)
    // o al volver de otra pantalla.
    //
    // Aquí:
    //  1) Reanudamos el "ticker" que refresca el texto del cronómetro cada
    //     500ms. No es necesario tenerlo corriendo cuando nadie ve la
    //     pantalla, así que solo vive entre onResume() y onPause().
    //  2) Le pedimos al ViewModel que recalcule el tiempo transcurrido
    //     usando el reloj real, para que si pasaron, por ejemplo, 3 minutos
    //     con la app en background, el cronómetro muestre esos 3 minutos
    //     de inmediato en vez de "saltar" o quedar atrasado.
    // ==================================================================
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume() -> app visible de nuevo, reanudando ticker y recalculando tiempo real")
        viewModel.onAppForegrounded()
        viewModel.startTicker()
    }

    // ==================================================================
    // onPause(): la Activity deja de estar en primer plano (el usuario la
    // minimiza, apaga la pantalla, o navega a otra app/pantalla). Es la
    // última llamada garantizada antes de que el proceso pueda ser matado
    // por el sistema.
    //
    // Aquí:
    //  1) Detenemos el ticker de UI (ya no hay pantalla visible que
    //     refrescar), pero OJO: el cronómetro real NO se detiene, porque
    //     su cálculo depende del timestamp guardado, no de este bucle.
    //  2) Forzamos a persistir el estado actual en el SavedStateHandle,
    //     como respaldo extra por si el sistema mata el proceso mientras
    //     estamos en background.
    // ==================================================================
    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause() -> app en segundo plano, deteniendo ticker de UI y guardando estado")
        viewModel.stopTicker()
        viewModel.onAppBackgrounded()
    }

    // ==================================================================
    // onSaveInstanceState(): se llama ANTES de que la Activity pueda ser
    // destruida por una causa que el sistema controla (rotación, o el
    // sistema decide matar el proceso para liberar memoria). El Bundle
    // que armamos aquí se le entrega de vuelta a onCreate(Bundle) si la
    // Activity se vuelve a crear.
    //
    // La mayoría de nuestros datos importantes (tareas, tarea activa,
    // temporizador) YA están seguros gracias al ViewModel + SavedStateHandle,
    // así que no es necesario duplicarlos aquí. Dejamos un ejemplo de un
    // valor puramente de UI (no de negocio) para mostrar cómo se usaría
    // este callback de forma clásica.
    // ==================================================================
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val scrollPosition = (binding.rvPendingTasks.layoutManager as? LinearLayoutManager)
            ?.findFirstVisibleItemPosition() ?: 0
        outState.putInt(KEY_SCROLL_DEMO, scrollPosition)
        Log.d(TAG, "onSaveInstanceState() -> guardando estado de UI de respaldo (scroll=$scrollPosition)")
    }
}
