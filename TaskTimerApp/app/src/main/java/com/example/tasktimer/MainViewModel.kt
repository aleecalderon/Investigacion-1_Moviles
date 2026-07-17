package com.example.tasktimer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * =============================================================================
 *  MainViewModel
 * =============================================================================
 *
 * POR QUÉ SE USA ViewModel
 * -------------------------
 * La Activity se destruye y se vuelve a crear en muchas situaciones (rotar la
 * pantalla, cambio de idioma, modo oscuro, etc). Si guardáramos la lista de
 * tareas, la tarea activa y el temporizador como variables de la Activity,
 * todo se perdería cada vez que Android la recree.
 *
 * El ViewModel vive en un "scope" atado al ciclo de vida del contenedor
 * (la Activity/Fragment) pero SOBREVIVE a la recreación por cambios de
 * configuración. Android internamente guarda la instancia del ViewModel en
 * un objeto separado (ViewModelStore) que no se destruye durante la rotación,
 * y se la vuelve a entregar a la nueva instancia de la Activity.
 *
 * Además, usamos SavedStateHandle: esto añade una segunda capa de seguridad.
 * Si el sistema operativo mata el PROCESO completo de la app (por ejemplo,
 * porque el usuario minimizó la app y el sistema necesitaba liberar memoria),
 * el ViewModel normal SÍ se pierde. Pero SavedStateHandle persiste esos datos
 * en el Bundle de estado de la Activity (el mismo mecanismo de
 * onSaveInstanceState/onCreate) y AndroidX se encarga de reconstruir el
 * ViewModel con esos valores automáticamente la próxima vez que se cree.
 *
 * En resumen:
 *   - Rotación de pantalla            -> el ViewModel en sí sobrevive.
 *   - Proceso muerto en background    -> SavedStateHandle lo reconstruye.
 */
class MainViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {

    companion object {
        private const val KEY_TASKS = "key_tasks"
        private const val KEY_ACTIVE_ID = "key_active_id"
        private const val KEY_ACCUMULATED_MILLIS = "key_accumulated_millis"
        private const val KEY_START_TIMESTAMP = "key_start_timestamp"
        private const val KEY_IS_RUNNING = "key_is_running"
        private const val KEY_NEXT_ID = "key_next_id"
    }

    // ------------------------------------------------------------------
    // LISTA DE TAREAS (pendientes + historial combinados; se filtran en UI)
    // ------------------------------------------------------------------
    private val _tasks = MutableStateFlow<List<Task>>(
        savedStateHandle.get<ArrayList<Task>>(KEY_TASKS) ?: arrayListOf()
    )
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    // ------------------------------------------------------------------
    // TAREA ACTIVA
    // ------------------------------------------------------------------
    private val _activeTaskId = MutableStateFlow(savedStateHandle.get<Long>(KEY_ACTIVE_ID))
    val activeTaskId: StateFlow<Long?> = _activeTaskId.asStateFlow()

    val activeTask: Task?
        get() = _activeTaskId.value?.let { id -> _tasks.value.find { it.id == id } }

    // ------------------------------------------------------------------
    // TEMPORIZADOR
    // ------------------------------------------------------------------
    // accumulatedMillis: tiempo ya sumado de tramos anteriores (cuando el
    //   cronómetro estuvo corriendo y se pausó).
    // startTimestamp: momento REAL (System.currentTimeMillis) en que arrancó
    //   el tramo actual, SOLO válido si isRunning == true.
    // isRunning: si el cronómetro está corriendo en este momento.
    private var accumulatedMillis: Long =
        savedStateHandle.get<Long>(KEY_ACCUMULATED_MILLIS) ?: 0L
    private var startTimestamp: Long =
        savedStateHandle.get<Long>(KEY_START_TIMESTAMP) ?: 0L
    private var isRunning: Boolean =
        savedStateHandle.get<Boolean>(KEY_IS_RUNNING) ?: false

    private var nextId: Long = savedStateHandle.get<Long>(KEY_NEXT_ID) ?: 1L

    private val _elapsedMillis = MutableStateFlow(calculateElapsedMillis())
    val elapsedMillis: StateFlow<Long> = _elapsedMillis.asStateFlow()

    val isTimerRunning: Boolean
        get() = isRunning

    private var tickerJob: Job? = null

    /**
     * CÓMO SE CALCULA EL TIEMPO REAL AUNQUE LA APP HAYA ESTADO CERRADA
     * ------------------------------------------------------------------
     * En vez de llevar la cuenta con un contador que suma "+1 segundo" en un
     * bucle (lo cual se detiene si la app se cierra/mata), guardamos DOS
     * cosas: cuánto tiempo ya se había acumulado (accumulatedMillis) y el
     * timestamp real de reloj (epoch, en milisegundos) en que arrancó el
     * tramo actual (startTimestamp).
     *
     * El tiempo transcurrido siempre se recalcula así:
     *
     *     elapsed = accumulatedMillis + (ahora - startTimestamp)     [si corre]
     *     elapsed = accumulatedMillis                                [si está pausado]
     *
     * Como "ahora" siempre es el reloj real del sistema, no importa si la
     * Activity estuvo destruida, la app en background, o el proceso murió y
     * volvió a abrirse 10 minutos después: en cuanto se reconstruye el
     * ViewModel con los valores guardados, este cálculo entrega el tiempo
     * correcto de inmediato, sin depender de que un Handler/Timer haya
     * seguido "corriendo" en memoria (porque no puede).
     */
    private fun calculateElapsedMillis(): Long {
        return if (isRunning) {
            accumulatedMillis + (System.currentTimeMillis() - startTimestamp)
        } else {
            accumulatedMillis
        }
    }

    /** Refresca el StateFlow visible por la UI y persiste el estado actual. */
    private fun refreshElapsed() {
        _elapsedMillis.value = calculateElapsedMillis()
    }

    private fun persistTimerState() {
        savedStateHandle[KEY_ACCUMULATED_MILLIS] = accumulatedMillis
        savedStateHandle[KEY_START_TIMESTAMP] = startTimestamp
        savedStateHandle[KEY_IS_RUNNING] = isRunning
    }

    private fun persistTasks() {
        savedStateHandle[KEY_TASKS] = ArrayList(_tasks.value)
        savedStateHandle[KEY_NEXT_ID] = nextId
    }

    private fun persistActiveId() {
        savedStateHandle[KEY_ACTIVE_ID] = _activeTaskId.value
    }

    // ------------------------------------------------------------------
    // TICKER: solo actualiza la UI cada 500ms MIENTRAS la pantalla es
    // visible. Se arranca desde Activity.onResume() y se detiene en
    // Activity.onPause(). Detenerlo NO pausa el cronómetro real: el
    // cronómetro sigue avanzando en base al timestamp; el ticker solo deja
    // de "refrescar el texto" en pantalla para ahorrar batería/CPU cuando
    // nadie lo está viendo.
    // ------------------------------------------------------------------
    fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = viewModelScope.launch {
            while (true) {
                refreshElapsed()
                delay(500)
            }
        }
    }

    fun stopTicker() {
        tickerJob?.cancel()
        tickerJob = null
    }

    // ------------------------------------------------------------------
    // ACCIONES PÚBLICAS
    // ------------------------------------------------------------------

    fun addTask(name: String) {
        if (name.isBlank()) return
        val newTask = Task(id = nextId, name = name.trim())
        nextId += 1
        _tasks.value = _tasks.value + newTask
        persistTasks()
    }

    /** Inicia (o reanuda) el cronómetro para la tarea indicada. */
    fun startTask(taskId: Long) {
        // Si había otra tarea corriendo, la pausamos y guardamos su progreso.
        if (isRunning && _activeTaskId.value != taskId) {
            pauseActiveTask()
        }

        val wasAlreadyActive = _activeTaskId.value == taskId
        _activeTaskId.value = taskId
        persistActiveId()

        val current = _tasks.value.find { it.id == taskId } ?: return
        accumulatedMillis = if (wasAlreadyActive) accumulatedMillis else current.elapsedMillis
        startTimestamp = System.currentTimeMillis()
        isRunning = true
        persistTimerState()
        refreshElapsed()
    }

    /** Pausa la tarea activa, guardando el tiempo acumulado real. */
    fun pauseActiveTask() {
        if (!isRunning) return
        val id = _activeTaskId.value ?: return

        accumulatedMillis = calculateElapsedMillis()
        isRunning = false
        persistTimerState()
        refreshElapsed()

        _tasks.value = _tasks.value.map { task ->
            if (task.id == id) task.copy(elapsedMillis = accumulatedMillis) else task
        }
        persistTasks()
    }

    /** Marca la tarea activa como completada y la envía al historial. */
    fun completeActiveTask() {
        val id = _activeTaskId.value ?: return
        val finalElapsed = calculateElapsedMillis()

        _tasks.value = _tasks.value.map { task ->
            if (task.id == id) task.copy(elapsedMillis = finalElapsed, isCompleted = true) else task
        }
        persistTasks()

        isRunning = false
        accumulatedMillis = 0L
        startTimestamp = 0L
        _activeTaskId.value = null
        persistTimerState()
        persistActiveId()
        refreshElapsed()
    }

    /**
     * Se llama desde Activity.onPause(). El cronómetro sigue corriendo
     * "en el tiempo real" (no lo detenemos), pero forzamos a guardar el
     * estado actual por si el sistema decide matar el proceso mientras la
     * app está en segundo plano.
     */
    fun onAppBackgrounded() {
        persistTimerState()
        persistTasks()
        persistActiveId()
    }

    /**
     * Se llama desde Activity.onResume(). Recalcula inmediatamente el
     * tiempo transcurrido usando el timestamp real guardado, por si pasó
     * mucho tiempo mientras la app estuvo en segundo plano (o si el
     * proceso murió y se reconstruyó todo desde SavedStateHandle).
     */
    fun onAppForegrounded() {
        refreshElapsed()
    }

    override fun onCleared() {
        super.onCleared()
        stopTicker()
    }
}
