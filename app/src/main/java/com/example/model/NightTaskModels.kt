package com.example.model

import androidx.compose.ui.graphics.Color
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.ui.theme.CategoryGymColor
import com.example.ui.theme.CategoryPersonalColor
import com.example.ui.theme.CategoryProjectsColor
import com.example.ui.theme.CategoryRestColor
import com.example.ui.theme.CategoryUniColor
import com.example.ui.theme.PriorityHighColor
import com.example.ui.theme.PriorityLowColor
import com.example.ui.theme.PriorityMediumColor

enum class TaskCategory(
    val displayName: String,
    val shortcutTag: String,
    val color: Color,
    val googleColorId: String
) {
    UNIVERSIDAD(
        displayName = "Universidad",
        shortcutTag = "#uni",
        color = CategoryUniColor,
        googleColorId = "9" // Blueberry / Indigo
    ),
    GIMNASIO(
        displayName = "Gimnasio",
        shortcutTag = "#gym",
        color = CategoryGymColor,
        googleColorId = "2" // Sage / Emerald
    ),
    PERSONAL(
        displayName = "Personal",
        shortcutTag = "#personal",
        color = CategoryPersonalColor,
        googleColorId = "5" // Banana / Amber
    ),
    PROYECTOS(
        displayName = "Proyectos",
        shortcutTag = "#proyectos",
        color = CategoryProjectsColor,
        googleColorId = "7" // Peacock / Cyan
    ),
    DESCANSO(
        displayName = "Hábitos & Salud",
        shortcutTag = "#salud",
        color = CategoryRestColor,
        googleColorId = "4" // Flamingo / Rose
    );

    companion object {
        fun fromName(name: String): TaskCategory =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: PROYECTOS
    }
}

enum class TaskPriority(
    val displayName: String,
    val shortcutTag: String,
    val weight: Int,
    val color: Color
) {
    ALTA(
        displayName = "Alta",
        shortcutTag = "!alta",
        weight = 3,
        color = PriorityHighColor
    ),
    MEDIA(
        displayName = "Media",
        shortcutTag = "!media",
        weight = 2,
        color = PriorityMediumColor
    ),
    BAJA(
        displayName = "Baja",
        shortcutTag = "!baja",
        weight = 1,
        color = PriorityLowColor
    );

    companion object {
        fun fromName(name: String): TaskPriority =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: MEDIA
    }
}

enum class SyncState(val labelEs: String) {
    LOCAL_ONLY("Solo local"),
    PENDING_SYNC("Pendiente de sincronizar"),
    SYNCED("Sincronizada en Calendar"),
    SYNC_ERROR("Error de sincronización");

    companion object {
        fun fromName(name: String): SyncState =
            entries.find { it.name.equals(name, ignoreCase = true) } ?: PENDING_SYNC
    }
}

@Entity(tableName = "night_tasks")
data class NightTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val notes: String = "",
    val targetDateIso: String, // Format: yyyy-MM-dd
    val category: String = TaskCategory.UNIVERSIDAD.name,
    val priority: String = TaskPriority.MEDIA.name,
    val startTime: String? = null, // Format: HH:mm (24h), e.g. "09:00"
    val endTime: String? = null,   // Format: HH:mm (24h), e.g. "10:30"
    val durationMinutes: Int? = null, // Optional duration in minutes if no explicit endTime
    val syncWithCalendar: Boolean = true,
    val googleEventId: String? = null,
    val googleHtmlLink: String? = null,
    val syncState: String = SyncState.PENDING_SYNC.name,
    val completed: Boolean = false,
    val completedAtMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    val categoryEnum: TaskCategory
        get() = TaskCategory.fromName(category)

    val priorityEnum: TaskPriority
        get() = TaskPriority.fromName(priority)

    val syncStateEnum: SyncState
        get() = SyncState.fromName(syncState)

    val hasTimeBlock: Boolean
        get() = !startTime.isNullOrBlank() && !endTime.isNullOrBlank()

    val formattedTimeBlock: String
        get() = when {
            !startTime.isNullOrBlank() && !endTime.isNullOrBlank() -> "$startTime – $endTime"
            !startTime.isNullOrBlank() && durationMinutes != null -> "$startTime (${durationMinutes}m)"
            durationMinutes != null && durationMinutes > 0 -> "${durationMinutes} min est."
            else -> "Todo el día / Flexible"
        }
}

data class ExternalCalendarEvent(
    val id: String,
    val title: String,
    val dateIso: String,
    val startTimeHHmm: String?, // null if all-day event
    val endTimeHHmm: String?,
    val isAllDay: Boolean,
    val location: String? = null,
    val htmlLink: String? = null,
    val isFromNightPlanner: Boolean = false
)

data class FreeTimeSlot(
    val startTimeHHmm: String,
    val endTimeHHmm: String,
    val durationMinutes: Int,
    val periodLabel: String // e.g. "Mañana", "Tarde", "Noche"
)

data class OverlapWarning(
    val taskId: Long,
    val taskTitle: String,
    val conflictingWithTitle: String,
    val overlapWindow: String,
    val isExternalGoogleEvent: Boolean
)

data class ParsedQuickTask(
    val cleanTitle: String,
    val detectedCategory: TaskCategory?,
    val detectedPriority: TaskPriority?,
    val detectedStartTime: String?,
    val detectedEndTime: String?,
    val detectedDurationMinutes: Int?
)

object QuickInputParser {
    private val timeRangeRegex = Regex("""@(\d{1,2}:\d{2})\s*-\s*(\d{1,2}:\d{2})""")
    private val singleStartRegex = Regex("""@(\d{1,2}:\d{2})""")
    private val durationRegex = Regex("""\+(\d{1,3})\s*m(?:in)?""", RegexOption.IGNORE_CASE)

    fun parse(rawInput: String): ParsedQuickTask {
        var working = rawInput

        var detectedCat: TaskCategory? = null
        val categoryMap = mapOf(
            "#uni" to TaskCategory.UNIVERSIDAD,
            "#universidad" to TaskCategory.UNIVERSIDAD,
            "#gym" to TaskCategory.GIMNASIO,
            "#gimnasio" to TaskCategory.GIMNASIO,
            "#personal" to TaskCategory.PERSONAL,
            "#proyectos" to TaskCategory.PROYECTOS,
            "#proyecto" to TaskCategory.PROYECTOS,
            "#salud" to TaskCategory.DESCANSO,
            "#descanso" to TaskCategory.DESCANSO
        )
        for ((tag, cat) in categoryMap) {
            if (working.contains(tag, ignoreCase = true)) {
                detectedCat = cat
                working = working.replace(Regex(Regex.escape(tag), RegexOption.IGNORE_CASE), "")
                break
            }
        }

        var detectedPrio: TaskPriority? = null
        val priorityMap = mapOf(
            "!alta" to TaskPriority.ALTA,
            "!high" to TaskPriority.ALTA,
            "!media" to TaskPriority.MEDIA,
            "!med" to TaskPriority.MEDIA,
            "!baja" to TaskPriority.BAJA,
            "!low" to TaskPriority.BAJA
        )
        for ((tag, prio) in priorityMap) {
            if (working.contains(tag, ignoreCase = true)) {
                detectedPrio = prio
                working = working.replace(Regex(Regex.escape(tag), RegexOption.IGNORE_CASE), "")
                break
            }
        }

        var startHHmm: String? = null
        var endHHmm: String? = null
        var durationMin: Int? = null

        val rangeMatch = timeRangeRegex.find(working)
        if (rangeMatch != null) {
            startHHmm = normalizeHHmm(rangeMatch.groupValues[1])
            endHHmm = normalizeHHmm(rangeMatch.groupValues[2])
            working = working.replace(rangeMatch.value, "")
        } else {
            val singleMatch = singleStartRegex.find(working)
            if (singleMatch != null) {
                startHHmm = normalizeHHmm(singleMatch.groupValues[1])
                working = working.replace(singleMatch.value, "")
            }
        }

        val durationMatch = durationRegex.find(working)
        if (durationMatch != null) {
            durationMin = durationMatch.groupValues[1].toIntOrNull()
            working = working.replace(durationMatch.value, "")
        }

        if (startHHmm != null && endHHmm == null && durationMin != null) {
            endHHmm = addMinutesToHHmm(startHHmm, durationMin)
        } else if (startHHmm != null && endHHmm != null && durationMin == null) {
            val diff = minutesBetween(startHHmm, endHHmm)
            if (diff > 0) durationMin = diff
        }

        val clean = working.replace(Regex("""\s+"""), " ").trim()
        return ParsedQuickTask(
            cleanTitle = clean,
            detectedCategory = detectedCat,
            detectedPriority = detectedPrio,
            detectedStartTime = startHHmm,
            detectedEndTime = endHHmm,
            detectedDurationMinutes = durationMin
        )
    }

    fun normalizeHHmm(raw: String): String {
        val parts = raw.trim().split(":")
        if (parts.size != 2) return raw
        val h = (parts[0].toIntOrNull() ?: 0).coerceIn(0, 23)
        val m = (parts[1].toIntOrNull() ?: 0).coerceIn(0, 59)
        return "%02d:%02d".format(h, m)
    }

    fun addMinutesToHHmm(startHHmm: String, addMinutes: Int): String {
        val total = toMinutesOfDay(startHHmm) + addMinutes
        val clamped = total.coerceIn(0, 23 * 60 + 59)
        return "%02d:%02d".format(clamped / 60, clamped % 60)
    }

    fun toMinutesOfDay(hhMm: String): Int {
        val parts = hhMm.split(":")
        if (parts.size != 2) return 0
        val h = parts[0].toIntOrNull() ?: 0
        val m = parts[1].toIntOrNull() ?: 0
        return h * 60 + m
    }

    fun minutesBetween(startHHmm: String, endHHmm: String): Int {
        return (toMinutesOfDay(endHHmm) - toMinutesOfDay(startHHmm)).coerceAtLeast(0)
    }
}
