package com.example.investigacion1

import android.graphics.Paint
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.investigacion1.databinding.ActivityMainBinding
import com.example.investigacion1.databinding.ItemTaskBinding

import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    //==============================
    // TEMPORIZADOR
    //==============================
    private var countDownTimer: CountDownTimer? = null
    private val START_TIME_IN_MILLIS: Long = 10 * 1000
    //private val START_TIME_IN_MILLIS: Long = 25 * 60 * 1000 // 25 Minutos
    private var timeLeftInMillis = START_TIME_IN_MILLIS
    private var timerRunning = false
    private var endTime: Long = 0

    //==============================
    // RESUMEN Y SESIONES
    //==============================
    private var tareasPendientes = 0
    private var sesionesCompletadas = 0

    //==============================
    // LISTA DE TAREAS
    //==============================
    private val taskList = mutableListOf<Task>()
    private var selectedTask = -1
    private var currentTaskName = "Sin tarea seleccionada"

    private val historyList = mutableListOf<String>()

    private var textoTemporal = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar interfaces
        updateTimerInterface()
        updateCountText()

        loadTasks()
        loadHistory()

        val preferences =
            getSharedPreferences("pomodoro_data", MODE_PRIVATE)

        endTime =
            preferences.getLong("endTime", 0)


        //--------------------------
        // BOTONES TEMPORIZADOR
        //--------------------------
        binding.btnStart.setOnClickListener {
            if (!timerRunning) {
                startTimer()
            }
        }

        binding.btnPause.setOnClickListener {
            if (timerRunning) {
                pauseTimer()
            }
        }

        binding.btnResume.setOnClickListener {
            if (!timerRunning) {
                startTimer()
            }
        }

        binding.btnReset.setOnClickListener {
            resetTimer()
        }

        //--------------------------
        // AGREGAR TAREA
        //--------------------------
        binding.btnAddTask.setOnClickListener {
            val taskName = binding.inputTask.text.toString().trim()

            if (taskName.isEmpty()) {
                Toast.makeText(this, "Escribe una tarea", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (taskList.any { it.title.equals(taskName, true) }) {
                Toast.makeText(this, "La tarea ya existe", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            taskList.add(Task(taskName))

            // ÚNICO CAMBIO AGREGADO: Mensaje flotante al ingresar una nueva tarea
            Toast.makeText(this, "Se agregó una nueva tarea: $taskName", Toast.LENGTH_SHORT).show()

            binding.inputTask.text.clear()
            refreshTaskList()
        }

        binding.btnClearHistory.setOnClickListener {

            historyList.clear()

            refreshHistory()

            Toast.makeText(
                this,
                "Historial eliminado",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {

        super.onRestoreInstanceState(savedInstanceState)

        binding.inputTask.setText(

            savedInstanceState.getString("texto","")

        )

        selectedTask =
            savedInstanceState.getInt("selectedTask", -1)

        timeLeftInMillis =
            savedInstanceState.getLong(
                "timeLeft",
                START_TIME_IN_MILLIS
            )

        timerRunning =
            savedInstanceState.getBoolean(
                "timerRunning",
                false
            )

        updateTimerInterface()

        if (timerRunning) {
            startTimer()
        }

        refreshTaskList()

    }

    // =======================================================
    // FUNCIONES DEL TEMPORIZADOR (CountDownTimer)
    // =======================================================
    private fun startTimer() {
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerInterface()
            }

            override fun onFinish() {
                timerRunning = false
                timeLeftInMillis = START_TIME_IN_MILLIS

                // Objetivo: Crear sesión completada y actualizar resumen
                sesionesCompletadas++

                updateTimerInterface()
                updateCountText()
                addHistoryItem()

                // Objetivo: Mostrar mensaje al terminar
                Toast.makeText(
                    this@MainActivity,
                    "¡Pomodoro terminado! Descansa un poco.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }.start()

        endTime = System.currentTimeMillis() + timeLeftInMillis
        timerRunning = true
    }

    private fun pauseTimer() {
        countDownTimer?.cancel()
        timerRunning = false
    }

    private fun resetTimer() {
        countDownTimer?.cancel()
        timerRunning = false
        timeLeftInMillis = START_TIME_IN_MILLIS
        updateTimerInterface()
    }

    private fun updateTimerInterface() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        val timeFormatted = String.format("%02d:%02d", minutes, seconds)

        // Mostrar tiempo
        binding.textTimer.text = timeFormatted

        // Actualizar ProgressBar (Porcentaje de 0 a 100)
        val progress = ((timeLeftInMillis.toFloat() / START_TIME_IN_MILLIS.toFloat()) * 100).toInt()
        binding.progressTimer.progress = progress
    }

    private fun refreshTaskList() {
        binding.containerTasks.removeAllViews()

        if (taskList.isEmpty()) {
            binding.textEmptyTasks.visibility = View.VISIBLE
        } else {
            binding.textEmptyTasks.visibility = View.GONE
        }

        tareasPendientes = 0

        taskList.forEachIndexed { index, task ->
            val taskBinding = ItemTaskBinding.inflate(layoutInflater)
            taskBinding.tvTaskTitle.text = task.title

            if (selectedTask == index) {
                taskBinding.root.setBackgroundColor(android.graphics.Color.parseColor("#FFF59D"))
            } else {
                taskBinding.root.setBackgroundColor(android.graphics.Color.WHITE)
            }

            taskBinding.cbTask.isChecked = task.completed

            taskBinding.cbTask.setOnCheckedChangeListener { _, isChecked ->
                task.completed = isChecked
                refreshTaskList()
            }

            taskBinding.root.setOnClickListener {

                selectedTask = index

                currentTaskName = task.title

                task.selected = true

                taskList.forEachIndexed { i, t ->
                    if (i != index) {
                        t.selected = false
                    }
                }

                Toast.makeText(
                    this,
                    "Tarea activa: ${task.title}",
                    Toast.LENGTH_SHORT
                ).show()

                refreshTaskList()
            }

            taskBinding.btnDeleteTask.setOnClickListener {
                taskList.removeAt(index)
                if (selectedTask == index) { selectedTask = -1 }
                refreshTaskList()
            }

            if (task.completed) {
                taskBinding.tvTaskTitle.paintFlags = taskBinding.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                tareasPendientes++
            }

            binding.containerTasks.addView(taskBinding.root)
        }
        updateCountText()
        saveTasks()
    }

    // Agregar al historial de forma segura
    private fun addHistoryItem() {

        val taskName = if (currentTaskName.isBlank())
            "Sin tarea seleccionada"
        else
            currentTaskName

        val session = """
Sesión $sesionesCompletadas

Tarea:
$taskName

Duración:
25 minutos
""".trimIndent()

        // Guardamos la sesión en la lista
        historyList.add(0, session)

        saveHistory()

        // Redibujamos el historial
        refreshHistory()

    }

    private fun refreshHistory() {

        binding.containerHistory.removeAllViews()

        if (historyList.isEmpty()) {

            binding.textEmptyHistory.visibility = View.VISIBLE
            return
        }

        binding.textEmptyHistory.visibility = View.GONE

        historyList.forEach { history ->

            val historyView =
                layoutInflater.inflate(R.layout.item_history, null)

            val tvHistory =
                historyView.findViewById<TextView>(R.id.tv_history_item)

            tvHistory.text = history

            binding.containerHistory.addView(historyView)

        }

    }

    // Actualizar Resumen de Pomodoros
    private fun updateCountText() {
        val completadas = taskList.count { it.completed }
        val pendientes = taskList.count { !it.completed }

        binding.textSummary.text = """
            Pendientes: $pendientes
            Completadas: $completadas
            Pomodoros: $sesionesCompletadas
        """.trimIndent()
    }

    //======================================================
// GUARDAR TAREAS
//======================================================
    private fun saveTasks() {

        val sharedPreferences =
            getSharedPreferences("pomodoro_data", MODE_PRIVATE)

        val editor = sharedPreferences.edit()

        val jsonArray = JSONArray()

        taskList.forEach { task ->

            val jsonObject = JSONObject()

            jsonObject.put("title", task.title)
            jsonObject.put("completed", task.completed)
            jsonObject.put("selected", task.selected)

            jsonArray.put(jsonObject)
        }

        editor.putString("tasks", jsonArray.toString())

        editor.apply()

        Toast.makeText(
            this,
            "Se guardaron ${taskList.size} tareas",
            Toast.LENGTH_SHORT
        ).show()
    }

    //======================================================
// GUARDAR HISTORIAL
//======================================================
    private fun saveHistory() {

        val sharedPreferences =
            getSharedPreferences("pomodoro_data", MODE_PRIVATE)

        val editor = sharedPreferences.edit()

        val jsonArray = JSONArray()

        historyList.forEach {

            jsonArray.put(it)

        }

        editor.putString("history", jsonArray.toString())

        editor.apply()

    }

    //======================================================
// CARGAR TAREAS
//======================================================
    private fun loadTasks() {

        val sharedPreferences =
            getSharedPreferences("pomodoro_data", MODE_PRIVATE)

        val data =
            sharedPreferences.getString("tasks", null)

        if (data != null) {

            val jsonArray = JSONArray(data)

            taskList.clear()

            for (i in 0 until jsonArray.length()) {

                val jsonObject = jsonArray.getJSONObject(i)

                taskList.add(

                    Task(

                        title = jsonObject.getString("title"),

                        completed = jsonObject.getBoolean("completed"),

                        selected = jsonObject.getBoolean("selected")

                    )

                )

            }

            refreshTaskList()

        }

    }

    //======================================================
// CARGAR HISTORIAL
//======================================================
    private fun loadHistory() {

        val sharedPreferences =
            getSharedPreferences("pomodoro_data", MODE_PRIVATE)

        val data =
            sharedPreferences.getString("history", null)

        if (data != null) {

            val jsonArray = JSONArray(data)

            historyList.clear()

            binding.containerHistory.removeAllViews()

            binding.textEmptyHistory.visibility = View.GONE

            for (i in 0 until jsonArray.length()) {

                val texto = jsonArray.getString(i)

                historyList.add(texto)

                val historyView =
                    layoutInflater.inflate(R.layout.item_history, null)

                val tvHistory =
                    historyView.findViewById<TextView>(R.id.tv_history_item)

                tvHistory.text = texto

                binding.containerHistory.addView(historyView)

            }

        }

    }

    override fun onSaveInstanceState(outState: Bundle) {

        super.onSaveInstanceState(outState)

        outState.putString(
            "texto",
            binding.inputTask.text.toString()
        )

        outState.putInt(
            "selectedTask",
            selectedTask
        )

        outState.putLong(
            "timeLeft",
            timeLeftInMillis
        )

        outState.putBoolean(
            "timerRunning",
            timerRunning
        )

    }

    override fun onPause() {
        super.onPause()

        saveTasks()
        saveHistory()

        val preferences =
            getSharedPreferences("pomodoro_data", MODE_PRIVATE)

        preferences.edit()
            .putLong("endTime", endTime)
            .apply()
    }
}