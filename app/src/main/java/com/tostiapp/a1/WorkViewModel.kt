package com.tostiapp.a1

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.tostiapp.a1.data.AppDatabase
import com.tostiapp.a1.data.WorkEntry
import com.tostiapp.a1.data.WorkDao
import kotlinx.coroutines.launch

class WorkViewModel(application: Application) : AndroidViewModel(application) {

    private val workDao: WorkDao
    val allWorkEntries: LiveData<List<WorkEntry>>

    init {
        val database = AppDatabase.getDatabase(application)
        workDao = database.workDao()
        allWorkEntries = workDao.getAllWorkEntries()
    }

    fun insert(workEntry: WorkEntry) = viewModelScope.launch {
        workDao.insert(workEntry)
    }

    fun update(workEntry: WorkEntry) = viewModelScope.launch {
        workDao.update(workEntry)
    }

    fun delete(workEntry: WorkEntry) = viewModelScope.launch {
        workDao.delete(workEntry)
    }

    fun getWorkEntryById(id: Int): LiveData<WorkEntry> {
        return workDao.getWorkEntryById(id)
    }
}