package com.tostiapp.a1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tostiapp.a1.data.WorkEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskListAdapter(
    private val onDeleteClicked: (WorkEntry) -> Unit,
    private val onEditClicked: (WorkEntry) -> Unit
) : ListAdapter<WorkEntry, TaskListAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        return TaskViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current, onDeleteClicked, onEditClicked)
    }

    class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val taskDateView: TextView = itemView.findViewById(R.id.task_item_date)
        private val taskDetailsView: TextView = itemView.findViewById(R.id.task_item_details)
        private val editButton: ImageButton = itemView.findViewById(R.id.edit_task_button)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.delete_task_button)

        fun bind(workEntry: WorkEntry, onDeleteClicked: (WorkEntry) -> Unit, onEditClicked: (WorkEntry) -> Unit) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateSdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            taskDateView.text = dateSdf.format(Date(workEntry.date))
            try {
                val startTime = sdf.parse(workEntry.startTime)
                val endTime = sdf.parse(workEntry.endTime)
                val diff = endTime.time - startTime.time
                val hours = diff / (1000 * 60 * 60)
                val minutes = (diff / (1000 * 60)) % 60
                val duration = String.format("%d:%02d", hours, minutes)

                taskDetailsView.text = "${workEntry.entryType}: ${duration} ore - ${workEntry.task}"
            } catch (e: Exception) {
                taskDetailsView.text = "Errore nel formato orario"
                e.printStackTrace()
            }
            editButton.setOnClickListener { onEditClicked(workEntry) }
            deleteButton.setOnClickListener { onDeleteClicked(workEntry) }
        }

        companion object {
            fun create(parent: ViewGroup): TaskViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.task_list_item, parent, false)
                return TaskViewHolder(view)
            }
        }
    }

    class TaskDiffCallback : DiffUtil.ItemCallback<WorkEntry>() {
        override fun areItemsTheSame(oldItem: WorkEntry, newItem: WorkEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WorkEntry, newItem: WorkEntry): Boolean {
            return oldItem == newItem
        }
    }
}