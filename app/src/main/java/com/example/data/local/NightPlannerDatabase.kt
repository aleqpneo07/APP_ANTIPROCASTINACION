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
                // Tasks planned for Tomorrow ("Planificar Mañana")
                NightTaskEntity(
                    title = "Estudio profundo: Arquitectura de Software & Sistemas Distribuidos",
                    notes = "Repasar patrones de resiliencia, OAuth 2.0 y ejercicios del módulo 4.",
                    targetDateIso = tomorrow,
                    category = TaskCategory.UNIVERSIDAD.name,
                    priority = TaskPriority.ALTA.name,
                    startTime = "09:00",
                    endTime = "11:00",
                    durationMinutes = 120,
                    syncWithCalendar = true,
                    syncState = SyncState.PENDING_SYNC.name,
                    completed = false
                ),
                NightTaskEntity(
                    title = "Sprint de código: Integración API Google Calendar v3",
                    notes = "Validar endpoints events.insert, events.patch y detección de solapamientos.",
                    targetDateIso = tomorrow,
                    category = TaskCategory.PROYECTOS.name,
                    priority = TaskPriority.ALTA.name,
                    startTime = "11:30",
                    endTime = "13:00",
                    durationMinutes = 90,
                    syncWithCalendar = true,
                    syncState = SyncState.PENDING_SYNC.name,
                    completed = false
                ),
                NightTaskEntity(
                    title = "Entrenamiento de Fuerza + Movilidad (Tren Superior)",
                    notes = "Press banca, dominadas lastradas y 15 min de estiramientos.",
                    targetDateIso = tomorrow,
                    category = TaskCategory.GIMNASIO.name,
                    priority = TaskPriority.MEDIA.name,
                    startTime = "18:00",
                    endTime = "19:15",
                    durationMinutes = 75,
                    syncWithCalendar = true,
                    syncState = SyncState.PENDING_SYNC.name,
                    completed = false
                ),
                NightTaskEntity(
                    title = "Lectura nocturna sin pantallas (30 págs) & Journaling",
                    notes = "Desconexión digital 45 min antes de dormir.",
                    targetDateIso = tomorrow,
                    category = TaskCategory.DESCANSO.name,
                    priority = TaskPriority.BAJA.name,
                    startTime = null,
                    endTime = null,
                    durationMinutes = 35,
                    syncWithCalendar = true,
                    syncState = SyncState.PENDING_SYNC.name,
                    completed = false
                ),

                // Tasks planned for Today ("Modo Ejecución Diaria - Hoy")
                NightTaskEntity(
                    title = "Entregar práctica de Laboratorio de Bases de Datos",
                    notes = "Subir script SQL y diagrama entidad-relación al campus virtual.",
                    targetDateIso = today,
                    category = TaskCategory.UNIVERSIDAD.name,
                    priority = TaskPriority.ALTA.name,
                    startTime = "08:30",
                    endTime = "10:00",
                    durationMinutes = 90,
                    syncWithCalendar = true,
                    syncState = SyncState.PENDING_SYNC.name,
                    completed = true,
                    completedAtMillis = System.currentTimeMillis() - 7_200_000L
                ),
                NightTaskEntity(
                    title = "Revisión de Pull Requests del proyecto final",
                    notes = "Comentar arquitectura limpia y pruebas unitarias.",
                    targetDateIso = today,
                    category = TaskCategory.PROYECTOS.name,
                    priority = TaskPriority.ALTA.name,
                    startTime = "11:00",
                    endTime = "12:15",
                    durationMinutes = 75,
                    syncWithCalendar = true,
                    syncState = SyncState.PENDING_SYNC.name,
                    completed = true,
                    completedAtMillis = System.currentTimeMillis() - 3_600_000L
                ),
                NightTaskEntity(
                    title = "Sesión HIIT & Core en gimnasio",
                    notes = "45 minutos de circuito metabólico.",
                    targetDateIso = today,
                    category = TaskCategory.GIMNASIO.name,
                    priority = TaskPriority.MEDIA.name,
                    startTime = "17:30",
                    endTime = "18:15",
                    durationMinutes = 45,
                    syncWithCalendar = true,
                    syncState = SyncState.PENDING_SYNC.name,
                    completed = false
                ),
                NightTaskEntity(
                    title = "Organizar presupuesto mensual y compras de la semana",
                    notes = "Si no da tiempo hoy, usar 'Migrar pendientes a mañana' al cerrar el día.",
                    targetDateIso = today,
                    category = TaskCategory.PERSONAL.name,
                    priority = TaskPriority.BAJA.name,
                    startTime = "20:00",
                    endTime = "20:45",
                    durationMinutes = 45,
                    syncWithCalendar = false,
                    syncState = SyncState.LOCAL_ONLY.name,
                    completed = false
                )
            )

            dao.insertAll(starterTasks)
        }
    }
}
