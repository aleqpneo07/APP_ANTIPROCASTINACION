package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.NightTaskEntity
import com.example.model.SyncState
import com.example.model.TaskCategory
import com.example.model.TaskPriority
import com.example.util.DateTimeHelper

@Database(
    entities = [NightTaskEntity::class],
    version = 1,
    exportSchema = false
)
abstract class NightPlannerDatabase : RoomDatabase() {

    abstract fun nightTaskDao(): NightTaskDao

    companion object {
        @Volatile
        private var INSTANCE: NightPlannerDatabase? = null

        fun getInstance(context: Context): NightPlannerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NightPlannerDatabase::class.java,
                    "night_planner.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedInitialDataIfEmpty(dao: NightTaskDao) {
            if (dao.countTasks() > 0) return

            val today = DateTimeHelper.todayIso()
            val tomorrow = DateTimeHelper.tomorrowIso()

            val starterTasks = listOf(
                // Tareas de ejemplo para Mañana
                NightTaskEntity(
                    title = "Práctica Metodología de la Programación (MP)",
                    notes = "Completar ejercicios del boletín de laboratorio (Grupo L2).",
                    targetDateIso = tomorrow,
                    category = TaskCategory.UNIVERSIDAD.name,
                    priority = TaskPriority.ALTA.name,
                    startTime = "08:30",
                    endTime = "09:45",
                    durationMinutes = 75,
                    syncWithCalendar = true,
                    syncState = SyncState.PENDING_SYNC.name,
                    completed = false
                ),
                NightTaskEntity(
                    title = "Actividad Académica Dirigida (AAD) · Bases de Datos",
                    notes = "Subir diseño relacional y consultas SQL al campus virtual.",
                    targetDateIso = tomorrow,
                    category = TaskCategory.UNIVERSIDAD.name,
                    priority = TaskPriority.ALTA.name,
                    startTime = "18:00",
                    endTime = "19:30",
                    durationMinutes = 90,
                    syncWithCalendar = true,
                    syncState = SyncState.PENDING_SYNC.name,
                    completed = false
                ),
                NightTaskEntity(
                    title = "Entrenamiento de Fuerza en Gimnasio",
                    notes = "Rutina tren superior + 15 min de movilidad.",
                    targetDateIso = tomorrow,
                    category = TaskCategory.GIMNASIO.name,
                    priority = TaskPriority.MEDIA.name,
                    startTime = "20:00",
                    endTime = "21:15",
                    durationMinutes = 75,
                    syncWithCalendar = true,
                    syncState = SyncState.PENDING_SYNC.name,
                    completed = false
                ),

                // Tareas de ejemplo para Hoy
                NightTaskEntity(
                    title = "Práctica de Algorítmica y Modelos (AMC)",
                    notes = "Implementar algoritmos voraces y analizar complejidad.",
                    targetDateIso = today,
                    category = TaskCategory.UNIVERSIDAD.name,
                    priority = TaskPriority.ALTA.name,
                    startTime = "08:30",
                    endTime = "09:45",
                    durationMinutes = 75,
                    syncWithCalendar = true,
                    syncState = SyncState.PENDING_SYNC.name,
                    completed = true,
                    completedAtMillis = System.currentTimeMillis() - 7_200_000L
                ),
                NightTaskEntity(
                    title = "Repasar apuntes de Redes Avanzadas (RA)",
                    notes = "Revisar esquemas de encaminamiento y subredes.",
                    targetDateIso = today,
                    category = TaskCategory.UNIVERSIDAD.name,
                    priority = TaskPriority.MEDIA.name,
                    startTime = "15:00",
                    endTime = "16:00",
                    durationMinutes = 60,
                    syncWithCalendar = true,
                    syncState = SyncState.PENDING_SYNC.name,
                    completed = false
                ),
                NightTaskEntity(
                    title = "Organizar carpeta del proyecto Android",
                    notes = "Limpiar pendientes de esta semana.",
                    targetDateIso = today,
                    category = TaskCategory.PROYECTOS.name,
                    priority = TaskPriority.BAJA.name,
                    startTime = null,
                    endTime = null,
                    durationMinutes = 30,
                    syncWithCalendar = false,
                    syncState = SyncState.LOCAL_ONLY.name,
                    completed = false
                )
            )

            dao.insertAll(starterTasks)
        }
    }
}
