package com.tostiapp.a1

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tostiapp.a1.data.WorkEntry
import java.text.SimpleDateFormat
import java.util.Locale

class WorkListAdapter : ListAdapter<WorkEntry, WorkListAdapter.WorkViewHolder>(WorkDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkViewHolder {
        return WorkViewHolder.create(parent)
    }

    override fun onBindViewHolder(holder: WorkViewHolder, position: Int) {
        val current = getItem(position)
        holder.bind(current)
    }

    class WorkViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateItemView: TextView = itemView.findViewById(R.id.item_date)
        private val entryTypeItemView: TextView = itemView.findViewById(R.id.item_entry_type)
        private val hoursItemView: TextView = itemView.findViewById(R.id.item_hours)
        private val travelKmsItemView: TextView = itemView.findViewById(R.id.item_travel_kms)
        private val travelHoursItemView: TextView = itemView.findViewById(R.id.item_travel_hours)
        private val taskItemView: TextView = itemView.findViewById(R.id.item_task)

        fun bind(workEntry: WorkEntry) {
            dateItemView.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(workEntry.date)
            entryTypeItemView.text = workEntry.entryType

            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val startTime = sdf.parse(workEntry.startTime)
            val endTime = sdf.parse(workEntry.endTime)
            val breakTime = workEntry.breakTime ?: 0
            val diff = endTime.time - startTime.time - (breakTime * 60 * 1000)
            val hours = diff / (1000 * 60 * 60)
            val minutes = (diff / (1000 * 60)) % 60
            hoursItemView.text = "Ore: $hours:$minutes"

            workEntry.travelKms?.let { travelKmsItemView.text = "Km trasferta: $it" }
            workEntry.travelHours?.let { travelHoursItemView.text = "H viaggio: $it" }
            taskItemView.text = "Mansione: ${workEntry.task}"
        }

        companion object {
            fun create(parent: ViewGroup): WorkViewHolder {
                val view: View = LayoutInflater.from(parent.context)
                    .inflate(R.layout.recyclerview_item, parent, false)
                return WorkViewHolder(view)
            }
        }
    }

    class WorkDiffCallback : DiffUtil.ItemCallback<WorkEntry>() {
        override fun areItemsTheSame(oldItem: WorkEntry, newItem: WorkEntry): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WorkEntry, newItem: WorkEntry): Boolean {
            return oldItem == newItem
        }
    }
}