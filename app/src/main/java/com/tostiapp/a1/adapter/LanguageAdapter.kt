package com.tostiapp.a1.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.tostiapp.a1.R
import com.tostiapp.a1.model.LanguageItem

class LanguageAdapter(
    private val languages: List<LanguageItem>,
    private val onItemClick: (LanguageItem) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.LanguageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_language, parent, false)
        return LanguageViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
        val language = languages[position]
        holder.bind(language)
    }

    override fun getItemCount(): Int = languages.size

    inner class LanguageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.language_name)
        private val downloadIcon: ImageView = itemView.findViewById(R.id.icon_download)
        private val installedIcon: ImageView = itemView.findViewById(R.id.icon_installed)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progress_downloading)

        fun bind(language: LanguageItem) {
            nameTextView.text = language.name

            when {
                language.isDownloading -> {
                    progressBar.visibility = View.VISIBLE
                    downloadIcon.visibility = View.GONE
                    installedIcon.visibility = View.GONE
                }
                language.isDownloaded -> {
                    progressBar.visibility = View.GONE
                    downloadIcon.visibility = View.GONE
                    installedIcon.visibility = View.VISIBLE
                }
                else -> {
                    progressBar.visibility = View.GONE
                    downloadIcon.visibility = View.VISIBLE
                    installedIcon.visibility = View.GONE
                }
            }

            itemView.setOnClickListener {
                onItemClick(language)
            }
        }
    }
}
