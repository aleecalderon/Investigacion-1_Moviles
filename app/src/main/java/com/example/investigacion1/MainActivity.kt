package com.example.investigacion1

import android.graphics.Paint
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.investigacion1.databinding.ActivityMainBinding
import com.example.investigacion1.databinding.ItemTaskBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    //==============================
    // TEMPORIZADOR
    //==============================

    private var countDownTimer: CountDownTimer? = null

    private val START_TIME_IN_MILLIS: Long =
        25 * 60 * 1000

    private var timeLeftInMillis =
        START_TIME_IN_MILLIS

    private var timerRunning = false

    //==============================
    // RESUMEN
    //==============================

    private var tareasPendientes = 0

    private var sesionesCompletadas = 0

    //==============================
    // LISTA DE TAREAS
    //==============================

    private val taskList =
        mutableListOf<Task>()

    private var selectedTask = -1

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding =
            ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        updateCountText()

        //--------------------------
        // BOTONES TEMPORIZADOR
        //--------------------------

        binding.btnStart.setOnClickListener {

            if (!timerRunning)
                startTimer()

        }

        binding.btnPause.setOnClickListener {

            if (timerRunning)
                pauseTimer()

        }

        binding.btnResume.setOnClickListener {

            if (!timerRunning)
                startTimer()

        }

        binding.btnReset.setOnClickListener {

            resetTimer()

        }

        //--------------------------
        // AGREGAR TAREA
        //--------------------------

        binding.btnAddTask.setOnClickListener {

            val taskName =
                binding.inputTask.text.toString().trim()

            if (taskName.isEmpty()) {

                Toast.makeText(
                    this,
                    "Escribe una tarea",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (taskList.any {
                    it.title.equals(taskName, true)
                }) {

                Toast.makeText(
                    this,
                    "La tarea ya existe",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            taskList.add(
                Task(taskName)
            )

            binding.inputTask.text.clear()

            refreshTaskList()

        }

    }

    // =======================================================
    // FUNCIONES DEL TEMPORIZADOR
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

                sesionesCompletadas++

                updateTimerInterface()

                updateCountText()

                addHistoryItem()

                Toast.makeText(
                    this@MainActivity,
                    "¡Pomodoro terminado!",
                    Toast.LENGTH_LONG
                ).show()

            }

        }.start()

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

        val minutes =
            (timeLeftInMillis / 1000) / 60

        val seconds =
            (timeLeftInMillis / 1000) % 60

        val timeFormatted =
            String.format("%02d:%02d", minutes, seconds)

        binding.textTimer.text = timeFormatted

        val progress = (
                timeLeftInMillis.toFloat()
                        / START_TIME_IN_MILLIS.toFloat()
                        * 100
                ).toInt()

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

            val taskBinding =
                ItemTaskBinding.inflate(layoutInflater)

            taskBinding.tvTaskTitle.text =
                task.title

            if (selectedTask == index) {

                taskBinding.root.setBackgroundColor(
                    android.graphics.Color.parseColor("#FFF59D")
                )

            } else {

                taskBinding.root.setBackgroundColor(
                    android.graphics.Color.WHITE
                )

            }

            taskBinding.cbTask.isChecked =
                task.completed

            taskBinding.cbTask.setOnCheckedChangeListener { _, isChecked ->

                task.completed = isChecked

                refreshTaskList()

            }

            taskBinding.root.setOnClickListener {

                selectedTask = index

                refreshTaskList()

            }

            taskBinding.btnDeleteTask.setOnClickListener {

                taskList.removeAt(index)

                if (selectedTask == index) {

                    selectedTask = -1

                }

                refreshTaskList()

            }

            if (task.completed) {

                taskBinding.tvTaskTitle.paintFlags =
                    taskBinding.tvTaskTitle.paintFlags or
                            Paint.STRIKE_THRU_TEXT_FLAG

            } else {

                tareasPendientes++

            }

            binding.containerTasks.addView(taskBinding.root)

        }

        updateCountText()

    }

    private fun addHistoryItem() {
        binding.textEmptyHistory.visibility = View.GONE

        val historyView = layoutInflater.inflate(R.layout.item_history, null)
        val tvHistoryItem = historyView.findViewById<TextView>(R.id.tv_history_item)

        tvHistoryItem.text = "Sesión $sesionesCompletadas: Completada - 25 min"

        // Agrega el historial (el 0 hace que se ponga al principio de la lista)
        binding.containerHistory.addView(historyView, 0)
    }

    private fun updateCountText() {

        val completadas = taskList.count { it.completed }

        val pendientes = taskList.count { !it.completed }

        binding.textSummary.text =
            """
Pendientes: $pendientes

Completadas: $completadas

Pomodoros: $sesionesCompletadas
        """.trimIndent()

    }
}