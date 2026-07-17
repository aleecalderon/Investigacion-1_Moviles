package com.example.tasktimer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tasktimer.databinding.ItemTaskBinding
import java.util.concurrent.TimeUnit

/**
 * Adaptador simple para mostrar tareas pendientes o el historial.
 *
 * [onTaskClick] se usa solo en la lista de pendientes, para permitir
 * seleccionar/iniciar una tarea directamente desde la lista.
 */
class TaskAdapter(
    private val onTaskClick: ((Task) -> Unit)? = null
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private var items: List<Task> = emptyList()

    fun submitList(newItems: List<Task>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(items[position], onTaskClick)
    }

    override fun getItemCount(): Int = items.size

    class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task, onTaskClick: ((Task) -> Unit)?) {
            binding.tvItemName.text = task.name
            binding.tvItemTime.text = formatMillis(task.elapsedMillis)
            binding.root.setOnClickListener {
                onTaskClick?.invoke(task)
            }
        }

        private fun formatMillis(millis: Long): String {
            val h = TimeUnit.MILLISECONDS.toHours(millis)
            val m = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
            val s = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
            return String.format("%02d:%02d:%02d", h, m, s)
        }
    }
}
