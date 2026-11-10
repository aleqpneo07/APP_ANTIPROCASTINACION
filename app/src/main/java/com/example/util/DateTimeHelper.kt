package com.example.util

import com.example.model.ExternalCalendarEvent
import com.example.model.FreeTimeSlot
import com.example.model.NightTaskEntity
import com.example.model.OverlapWarning
import com.example.model.QuickInputParser
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

object DateTimeHelper {

    private val isoFormat: SimpleDateFormat
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun todayIso(): String {
        val cal = Calendar.getInstance()
        return isoFormat.format(cal.time)
    }

    fun tomorrowIso(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return isoFormat.format(cal.time)
    }

    fun addDaysToIso(baseIso: String, days: Int): String {
        val parsed = runCatching { isoFormat.parse(baseIso) }.getOrNull() ?: return tomorrowIso()
        val cal = Calendar.getInstance()
        cal.time = parsed
        cal.add(Calendar.DAY_OF_YEAR, days)
        return isoFormat.format(cal.time)
    }

    fun formatHumanDateEs(dateIso: String): String {
        val parsed = runCatching { isoFormat.parse(dateIso) }.getOrNull() ?: return dateIso
        val formatter = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("es", "ES"))
        val raw = formatter.format(parsed)
        return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "ES")) else it.toString() }
    }

    fun currentTimeGreetingEs(): Pair<String, Boolean> {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isNight = hour >= 19 || hour < 5
        val label = when {
            hour >= 20 || hour < 5 -> "Modo Planificación Nocturna Activo"
            hour in 5..11 -> "Buenos días · Modo Ejecución Diaria"
            hour in 12..18 -> "Buenas tardes · Foco del Día"
            else -> "Atardecer · Prepara tu Mañana"
        }
        return label to isNight
    }

    /**
     * Converts a dateIso ("2026-11-11") and timeHHmm ("09:30") into an RFC3339 timestamp
     * with local timezone offset required by Google Calendar API v3, e.g. "2026-11-11T09:30:00-08:00".
     */
    fun toRfc3339DateTime(dateIso: String, timeHHmm: String): String {
        val dateParts = dateIso.split("-")
        val timeParts = timeHHmm.split(":")
        val cal = Calendar.getInstance()
        if (dateParts.size == 3 && timeParts.size == 2) {
            cal.set(Calendar.YEAR, dateParts[0].toIntOrNull() ?: 2026)
            cal.set(Calendar.MONTH, (dateParts[1].toIntOrNull() ?: 1) - 1)
            cal.set(Calendar.DAY_OF_MONTH, dateParts[2].toIntOrNull() ?: 1)
            cal.set(Calendar.HOUR_OF_DAY, timeParts[0].toIntOrNull() ?: 9)
            cal.set(Calendar.MINUTE, timeParts[1].toIntOrNull() ?: 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }
        val rfcFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US)
        return rfcFormat.format(cal.time)
    }

    /**
     * Returns (timeMin, timeMax) in RFC3339 for an entire local day [00:00:00 to 23:59:59].
     */
    fun getDayRfc3339Bounds(dateIso: String): Pair<String, String> {
        val min = toRfc3339DateTime(dateIso, "00:00")
        val max = toRfc3339DateTime(dateIso, "23:59")
        return min to max
    }

    fun getLocalTimeZoneId(): String = TimeZone.getDefault().id

    /**
     * Extracts "HH:mm" from a Google Calendar RFC3339 dateTime string such as "2026-11-11T14:30:00+01:00"
     */
    fun extractHHmmFromRfc3339(rfc3339: String?): String? {
        if (rfc3339.isNullOrBlank()) return null
        val tIndex = rfc3339.indexOf('T')
        if (tIndex == -1 || rfc3339.length < tIndex + 6) return null
        return rfc3339.substring(tIndex + 1, tIndex + 6)
    }

    /**
     * Detects any overlaps between tomorrow's planned tasks and:
     * 1) Existing Google Calendar events
     * 2) Other planned tasks with time blocks
     */
    fun detectScheduleOverlaps(
        tasks: List<NightTaskEntity>,
        externalEvents: List<ExternalCalendarEvent>
    ): List<OverlapWarning> {
        val warnings = mutableListOf<OverlapWarning>()
        val scheduledTasks = tasks.filter { !it.startTime.isNullOrBlank() && !it.endTime.isNullOrBlank() }

        for (task in scheduledTasks) {
            val tStart = QuickInputParser.toMinutesOfDay(task.startTime!!)
            val tEnd = QuickInputParser.toMinutesOfDay(task.endTime!!)
            if (tEnd <= tStart) continue

            // Check against external Google Calendar events (excluding events created by this exact task)
            for (ext in externalEvents) {
                if (ext.isAllDay || ext.startTimeHHmm == null || ext.endTimeHHmm == null) continue
                if (task.googleEventId != null && task.googleEventId == ext.id) continue

                val eStart = QuickInputParser.toMinutesOfDay(ext.startTimeHHmm)
                val eEnd = QuickInputParser.toMinutesOfDay(ext.endTimeHHmm)
                if (tStart < eEnd && tEnd > eStart) {
                    val overlapStart = maxOf(tStart, eStart)
                    val overlapEnd = minOf(tEnd, eEnd)
                    warnings.add(
                        OverlapWarning(
                            taskId = task.id,
                            taskTitle = task.title,
                            conflictingWithTitle = ext.title,
                            overlapWindow = "${formatMin(overlapStart)}–${formatMin(overlapEnd)}",
                            isExternalGoogleEvent = true
                        )
                    )
                }
            }

            // Check against other scheduled tasks
            for (other in scheduledTasks) {
                if (other.id == task.id || other.id < task.id) continue
                val oStart = QuickInputParser.toMinutesOfDay(other.startTime!!)
                val oEnd = QuickInputParser.toMinutesOfDay(other.endTime!!)
                if (tStart < oEnd && tEnd > oStart) {
                    val overlapStart = maxOf(tStart, oStart)
                    val overlapEnd = minOf(tEnd, oEnd)
                    warnings.add(
                        OverlapWarning(
                            taskId = task.id,
                            taskTitle = task.title,
                            conflictingWithTitle = other.title,
                            overlapWindow = "${formatMin(overlapStart)}–${formatMin(overlapEnd)}",
                            isExternalGoogleEvent = false
                        )
                    )
                }
            }
        }
        return warnings
    }

    /**
     * Computes available Free Time Slots ("Huecos Libres") between 07:30 and 22:30
     * taking into account both imported Google Calendar events and already time-blocked tasks.
     */
    fun calculateFreeTimeSlots(
        tasks: List<NightTaskEntity>,
        externalEvents: List<ExternalCalendarEvent>,
        dayStartMinutes: Int = 8 * 60, // 08:00
        dayEndMinutes: Int = 22 * 60   // 22:00
    ): List<FreeTimeSlot> {
        val busyIntervals = mutableListOf<Pair<Int, Int>>()

        tasks.forEach { task ->
            if (!task.startTime.isNullOrBlank() && !task.endTime.isNullOrBlank()) {
                val s = QuickInputParser.toMinutesOfDay(task.startTime)
                val e = QuickInputParser.toMinutesOfDay(task.endTime)
                if (e > s) busyIntervals.add(s to e)
            }
        }

        externalEvents.forEach { ext ->
            if (!ext.isAllDay && ext.startTimeHHmm != null && ext.endTimeHHmm != null) {
                val s = QuickInputParser.toMinutesOfDay(ext.startTimeHHmm)
                val e = QuickInputParser.toMinutesOfDay(ext.endTimeHHmm)
                if (e > s) busyIntervals.add(s to e)
            }
        }

        val sorted = busyIntervals
            .map { maxOf(it.first, dayStartMinutes) to minOf(it.second, dayEndMinutes) }
            .filter { it.second > it.first }
            .sortedBy { it.first }

        val merged = mutableListOf<Pair<Int, Int>>()
        for (interval in sorted) {
            if (merged.isEmpty() || merged.last().second < interval.first) {
                merged.add(interval)
            } else {
                val last = merged.removeAt(merged.lastIndex)
                merged.add(last.first to maxOf(last.second, interval.second))
            }
        }

        val freeSlots = mutableListOf<FreeTimeSlot>()
        var cursor = dayStartMinutes
        for (busy in merged) {
            if (busy.first - cursor >= 30) {
                freeSlots.add(buildSlot(cursor, busy.first))
            }
            cursor = maxOf(cursor, busy.second)
        }
        if (dayEndMinutes - cursor >= 30) {
            freeSlots.add(buildSlot(cursor, dayEndMinutes))
        }
        return freeSlots
    }

    private fun buildSlot(startMin: Int, endMin: Int): FreeTimeSlot {
        val duration = endMin - startMin
        val period = when {
            startMin < 12 * 60 -> "Mañana"
            startMin < 18 * 60 -> "Tarde"
            else -> "Noche"
        }
        return FreeTimeSlot(
            startTimeHHmm = formatMin(startMin),
            endTimeHHmm = formatMin(endMin),
            durationMinutes = duration,
            periodLabel = period
        )
    }

    fun formatMin(totalMinutes: Int): String {
        val h = (totalMinutes / 60).coerceIn(0, 23)
        val m = (totalMinutes % 60).coerceIn(0, 59)
        return "%02d:%02d".format(h, m)
    }
}
