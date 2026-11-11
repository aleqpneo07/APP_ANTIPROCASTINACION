package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.NightTaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NightTaskDao {

    @Query("SELECT * FROM night_tasks ORDER BY targetDateIso ASC, completed ASC, COALESCE(startTime, '99:99') ASC, createdAtMillis DESC")
    fun observeAllTasks(): Flow<List<NightTaskEntity>>

    @Query("SELECT * FROM night_tasks WHERE targetDateIso = :dateIso ORDER BY completed ASC, COALESCE(startTime, '99:99') ASC, createdAtMillis DESC")
    fun observeTasksForDate(dateIso: String): Flow<List<NightTaskEntity>>

    @Query("SELECT * FROM night_tasks WHERE targetDateIso = :dateIso")
    suspend fun getTasksForDateOnce(dateIso: String): List<NightTaskEntity>

    @Query("SELECT * FROM night_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): NightTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: NightTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<NightTaskEntity>)

    @Update
    suspend fun updateTask(task: NightTaskEntity)

    @Delete
    suspend fun deleteTask(task: NightTaskEntity)

    @Query("DELETE FROM night_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("SELECT COUNT(*) FROM night_tasks")
    suspend fun countTasks(): Int

    @Query(
        """
        UPDATE night_tasks 
        SET targetDateIso = :toDateIso,
            syncState = CASE WHEN syncWithCalendar = 1 THEN 'PENDING_SYNC' ELSE 'LOCAL_ONLY' END
        WHERE targetDateIso = :fromDateIso AND completed = 0
        """
    )
    suspend fun migrateIncompleteTasks(fromDateIso: String, toDateIso: String): Int
}
