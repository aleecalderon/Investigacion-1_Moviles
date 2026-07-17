package com.example.tasktimer

import java.io.Serializable

/**
 * Modelo de una tarea.
 *
 * Implementa [Serializable] a propósito: así podemos guardar la LISTA COMPLETA
 * de tareas dentro de un [androidx.lifecycle.SavedStateHandle] (que por dentro
 * usa un Bundle). Esto es lo que permite que la lista sobreviva no solo a la
 * rotación de pantalla, sino también a que el sistema mate el proceso mientras
 * la app está en segundo plano (algo que un ViewModel "normal" NO puede evitar
 * por sí solo).
 *
 * @param elapsedMillis  tiempo acumulado en milisegundos, mientras la tarea
 *                       NO está corriendo (pausada o completada).
 * @param isCompleted    true cuando el usuario presionó "Completar" y la tarea
 *                       pasó al historial.
 */
data class Task(
    val id: Long,
    val name: String,
    val elapsedMillis: Long = 0L,
    val isCompleted: Boolean = false
) : Serializable
