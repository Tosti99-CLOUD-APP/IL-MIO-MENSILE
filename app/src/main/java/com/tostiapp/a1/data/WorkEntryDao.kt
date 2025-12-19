package com.tostiapp.a1.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface WorkEntryDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(workEntry: WorkEntry)

    @Update
    suspend fun update(workEntry: WorkEntry)

    @Delete
    suspend fun delete(workEntry: WorkEntry)

    @Query("SELECT * FROM work_entries ORDER BY date DESC")
    fun getAllWorkEntries(): LiveData<List<WorkEntry>>

    @Query("SELECT * FROM work_entries WHERE id = :id")
    fun getWorkEntryById(id: Int): LiveData<WorkEntry>
}