package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.UniBuildingEtp
import com.example.ui.theme.UniBuildingGalilei
import com.example.ui.theme.UniBuildingJim
import com.example.ui.theme.UniBuildingMorales
import com.example.ui.theme.UniSubjectAmc
import com.example.ui.theme.UniSubjectBd
import com.example.ui.theme.UniSubjectDeso
import com.example.ui.theme.UniSubjectMp
import com.example.ui.theme.UniSubjectPhv
import com.example.ui.theme.UniSubjectRa
import com.example.util.DateTimeHelper
import java.util.Calendar

enum class UniBuilding(
    val displayName: String,
    val color: Color
) {
    GALILEI("Galileo Galilei", UniBuildingGalilei),
    MORALES("Isidoro Morales", UniBuildingMorales),
    JIM("JIM", UniBuildingJim),
    ETP("ETP", UniBuildingEtp)
}

enum class UniSubject(
    val abbr: String,
    val fullName: String,
    val color: Color
) {
    PHV(
        abbr = "PHVIM",
        fullName = "Procesamiento del Habla, Visión e Interacción Multimodal",
        color = UniSubjectPhv
    ),
    DESO(
        abbr = "DESO",
        fullName = "Diseño y Estructura de los Sistemas Operativos",
        color = UniSubjectDeso
    ),
    MP(
        abbr = "MP",
        fullName = "Metodología de la Programación",
        color = UniSubjectMp
    ),
    BD(
        abbr = "BD",
        fullName = "Bases de Datos",
        color = UniSubjectBd
    ),
    AMC(
        abbr = "AMC",
        fullName = "Algorítmica y Modelos de Computación",
        color = UniSubjectAmc
    ),
    RA(
        abbr = "RA",
        fullName = "Redes Avanzadas",
        color = UniSubjectRa
    )
}

enum class UniDay(
    val calendarDayOfWeek: Int,
    val shortLabel: String,
    val fullLabel: String,
    val timeRangeLabel: String
) {
    LUNES(Calendar.MONDAY, "Lun", "Lunes", "10:00–18:00"),
    MARTES(Calendar.TUESDAY, "Mar", "Martes", "10:00–15:00"),
    MIERCOLES(Calendar.WEDNESDAY, "Mié", "Miércoles", "10:00–17:30"),
    JUEVES(Calendar.THURSDAY, "Jue", "Jueves", "10:00–19:30"),
    VIERNES(Calendar.FRIDAY, "Vie", "Viernes", "10:00–13:30")
}

data class UniClassSession(
    val id: String,
    val day: UniDay,
    val subject: UniSubject,
    val startMinutes: Int,
    val endMinutes: Int,
    val isPractice: Boolean,
    val groupLabel: String? = null,
    val room: String,
    val building: UniBuilding,
    val durationLabel: String
) {
    val startHHmm: String get() = DateTimeHelper.formatMin(startMinutes)
    val endHHmm: String get() = DateTimeHelper.formatMin(endMinutes)

    val liveTitle: String
        get() = when {
            isPractice && groupLabel != null -> "${subject.abbr} (Prácticas $groupLabel)"
            subject == UniSubject.RA -> "${subject.fullName} (${subject.abbr})"
            subject == UniSubject.MP || subject == UniSubject.BD || subject == UniSubject.AMC ->
                "${subject.abbr} (Teoría)"
            else -> subject.abbr
        }

    val liveRoomLabel: String
        get() = when (building) {
            UniBuilding.GALILEI -> if (room.contains("Galileo")) room else "$room (Galileo)"
            UniBuilding.MORALES -> if (room.contains("Morales")) room else "$room (Morales)"
            UniBuilding.JIM, UniBuilding.ETP -> room
        }
}

sealed interface ScheduleEntry {
    val key: String

    data class DayDivider(
        val day: UniDay,
        val classCount: Int,
        val timeRangeLabel: String
    ) : ScheduleEntry {
        override val key: String = "divider_${day.name}"
    }

    data class DaySummary(
        val day: UniDay,
        val classCount: Int,
        val timeRangeLabel: String
    ) : ScheduleEntry {
        override val key: String = "summary_${day.name}"
    }

    data class ClassCard(
        val session: UniClassSession
    ) : ScheduleEntry {
        override val key: String = "class_${session.id}"
    }

    data class BreakIndicator(
        val id: String,
        val day: UniDay,
        val label: String,
        val timeRange: String
    ) : ScheduleEntry {
        override val key: String = "break_$id"
    }

    data class OverlapNotice(
        val id: String,
        val day: UniDay,
        val message: String
    ) : ScheduleEntry {
        override val key: String = "overlap_$id"
    }
}

enum class LiveStatusKind {
    ACTIVE_NOW,
    NEXT_TODAY,
    WEEKEND,
    DAY_FINISHED
}

data class UniLiveStatus(
    val kind: LiveStatusKind,
    val badgeText: String,
    val mainText: String,
    val subText: String,
    val targetDay: UniDay,
    val highlightedStartMinutes: Int? = null,
    val activeWindowStartMinutes: Int? = null,
    val nextStartMinutes: Int? = null
)

object UniversityScheduleRepository {

    const val COURSE_SUBTITLE = "Curso 2026–27 · 1er Cuatrimestre · Grado Ing. Informática"

    val allSessions: List<UniClassSession> = listOf(
        // LUNES (5 clases)
        UniClassSession(
            id = "lun_mp_prac",
            day = UniDay.LUNES,
            subject = UniSubject.MP,
            startMinutes = 600,
            endMinutes = 690,
            isPractice = true,
            groupLabel = "Grupo L2",
            room = "JIM B.1",
            building = UniBuilding.JIM,
            durationLabel = "1h 30min"
        ),
        UniClassSession(
            id = "lun_phv_teo",
            day = UniDay.LUNES,
            subject = UniSubject.PHV,
            startMinutes = 690,
            endMinutes = 780,
            isPractice = false,
            room = "Aula G.2.11",
            building = UniBuilding.GALILEI,
            durationLabel = "1h 30min"
        ),
        UniClassSession(
            id = "lun_mp_teo",
            day = UniDay.LUNES,
            subject = UniSubject.MP,
            startMinutes = 690,
            endMinutes = 810,
            isPractice = false,
            room = "Aula IM.2.6",
            building = UniBuilding.MORALES,
            durationLabel = "2h"
        ),
        UniClassSession(
            id = "lun_bd_prac",
            day = UniDay.LUNES,
            subject = UniSubject.BD,
            startMinutes = 870,
            endMinutes = 960,
            isPractice = true,
            groupLabel = "Grupo L4",
            room = "JIM 1.1",
            building = UniBuilding.JIM,
            durationLabel = "1h 30min"
        ),
        UniClassSession(
            id = "lun_deso_teo",
            day = UniDay.LUNES,
            subject = UniSubject.DESO,
            startMinutes = 960,
            endMinutes = 1080,
            isPractice = false,
            room = "Aula G1.4",
            building = UniBuilding.GALILEI,
            durationLabel = "2h"
        ),

        // MARTES (3 clases)
        UniClassSession(
            id = "mar_phv_teo",
            day = UniDay.MARTES,
            subject = UniSubject.PHV,
            startMinutes = 600,
            endMinutes = 690,
            isPractice = false,
            room = "Aula G.2.11",
            building = UniBuilding.GALILEI,
            durationLabel = "1h 30min"
        ),
        UniClassSession(
            id = "mar_amc_prac",
            day = UniDay.MARTES,
            subject = UniSubject.AMC,
            startMinutes = 690,
            endMinutes = 780,
            isPractice = true,
            groupLabel = "Grupo L2",
            room = "ETP106",
            building = UniBuilding.ETP,
            durationLabel = "1h 30min"
        ),
        UniClassSession(
            id = "mar_bd_teo",
            day = UniDay.MARTES,
            subject = UniSubject.BD,
            startMinutes = 810,
            endMinutes = 900,
            isPractice = false,
            room = "Aula IM.2.6",
            building = UniBuilding.MORALES,
            durationLabel = "1h 30min"
        ),

        // MIÉRCOLES (3 clases)
        UniClassSession(
            id = "mie_amc_teo",
            day = UniDay.MIERCOLES,
            subject = UniSubject.AMC,
            startMinutes = 600,
            endMinutes = 690,
            isPractice = false,
            room = "Aula IM.1.6",
            building = UniBuilding.MORALES,
            durationLabel = "1h 30min"
        ),
        UniClassSession(
            id = "mie_ra_teo",
            day = UniDay.MIERCOLES,
            subject = UniSubject.RA,
            startMinutes = 780,
            endMinutes = 870,
            isPractice = false,
            room = "Aula G.2.11",
            building = UniBuilding.GALILEI,
            durationLabel = "1h 30min"
        ),
        UniClassSession(
            id = "mie_phv_prac",
            day = UniDay.MIERCOLES,
            subject = UniSubject.PHV,
            startMinutes = 960,
            endMinutes = 1050,
            isPractice = true,
            groupLabel = "Grupo L1",
            room = "ETP104",
            building = UniBuilding.ETP,
            durationLabel = "1h 30min"
        ),

        // JUEVES (3 clases)
        UniClassSession(
            id = "jue_ra_teo",
            day = UniDay.JUEVES,
            subject = UniSubject.RA,
            startMinutes = 600,
            endMinutes = 690,
            isPractice = false,
            room = "Aula G.2.11",
            building = UniBuilding.GALILEI,
            durationLabel = "1h 30min"
        ),
        UniClassSession(
            id = "jue_mp_teo",
            day = UniDay.JUEVES,
            subject = UniSubject.MP,
            startMinutes = 810,
            endMinutes = 900,
            isPractice = false,
            room = "Aula IM.2.6",
            building = UniBuilding.MORALES,
            durationLabel = "1h 30min"
        ),
        UniClassSession(
            id = "jue_deso_teo",
            day = UniDay.JUEVES,
            subject = UniSubject.DESO,
            startMinutes = 1050,
            endMinutes = 1170,
            isPractice = false,
            room = "Aula G1.4",
            building = UniBuilding.GALILEI,
            durationLabel = "2h"
        ),

        // VIERNES (2 clases)
        UniClassSession(
            id = "vie_amc_teo",
            day = UniDay.VIERNES,
            subject = UniSubject.AMC,
            startMinutes = 600,
            endMinutes = 690,
            isPractice = false,
            room = "Aula IM.1.6",
            building = UniBuilding.MORALES,
            durationLabel = "1h 30min"
        ),
        UniClassSession(
            id = "vie_bd_teo",
            day = UniDay.VIERNES,
            subject = UniSubject.BD,
            startMinutes = 690,
            endMinutes = 810,
            isPractice = false,
            room = "Aula IM.2.6",
            building = UniBuilding.MORALES,
            durationLabel = "2h"
        )
    )

    fun dayFromCalendarDayOfWeek(calendarDayOfWeek: Int): UniDay? =
        UniDay.entries.find { it.calendarDayOfWeek == calendarDayOfWeek }

    fun sessionsForDay(day: UniDay): List<UniClassSession> =
        allSessions.filter { it.day == day }

    fun sessionsForDateIso(dateIso: String): List<UniClassSession> {
        val dow = DateTimeHelper.dayOfWeekFromIso(dateIso)
        val uniDay = dayFromCalendarDayOfWeek(dow) ?: return emptyList()
        return sessionsForDay(uniDay)
    }

    /**
     * Converts university class sessions of a specific date into read-only ExternalCalendarEvent
     * items so free-slot calculations and task overlap warnings automatically avoid class hours.
     */
    fun toVirtualExternalEvents(dateIso: String): List<ExternalCalendarEvent> =
        sessionsForDateIso(dateIso).map { session ->
            val pracTag = if (session.isPractice) " Prácticas" else ""
            ExternalCalendarEvent(
                id = "uni_class_${session.id}",
                title = "Clase ${session.subject.abbr}$pracTag (${session.room})",
                dateIso = dateIso,
                startTimeHHmm = session.startHHmm,
                endTimeHHmm = session.endHHmm,
                isAllDay = false,
                location = "${session.room} · ${session.building.displayName}",
                isFromNightPlanner = false
            )
        }

    fun entriesForSelection(selectedDay: UniDay?, onlyPractices: Boolean): List<ScheduleEntry> {
        val daysToInclude = if (selectedDay == null) UniDay.entries else listOf(selectedDay)
        val result = mutableListOf<ScheduleEntry>()

        for (day in daysToInclude) {
            val daySessions = sessionsForDay(day).filter { !onlyPractices || it.isPractice }
            if (selectedDay == null) {
                if (onlyPractices && daySessions.isEmpty()) continue
                result.add(ScheduleEntry.DayDivider(day, daySessions.size, day.timeRangeLabel))
            } else {
                result.add(ScheduleEntry.DaySummary(day, daySessions.size, day.timeRangeLabel))
            }

            if (onlyPractices) {
                daySessions.forEach { result.add(ScheduleEntry.ClassCard(it)) }
            } else {
                when (day) {
                    UniDay.LUNES -> {
                        val s = sessionsForDay(day)
                        result.add(ScheduleEntry.ClassCard(s[0]))
                        result.add(
                            ScheduleEntry.OverlapNotice(
                                id = "lun_overlap_1130",
                                day = day,
                                message = "Solapamiento 11:30–13:00: PHVIM (Galileo G.2.11) coincide con MP Teoría (Morales IM.2.6)"
                            )
                        )
                        result.add(ScheduleEntry.ClassCard(s[1]))
                        result.add(ScheduleEntry.ClassCard(s[2]))
                        result.add(
                            ScheduleEntry.BreakIndicator(
                                id = "lun_break_1330",
                                day = day,
                                label = "Descanso / Almuerzo · 1h",
                                timeRange = "13:30 → 14:30"
                            )
                        )
                        result.add(ScheduleEntry.ClassCard(s[3]))
                        result.add(ScheduleEntry.ClassCard(s[4]))
                    }

                    UniDay.MARTES -> {
                        val s = sessionsForDay(day)
                        result.add(ScheduleEntry.ClassCard(s[0]))
                        result.add(ScheduleEntry.ClassCard(s[1]))
                        result.add(
                            ScheduleEntry.BreakIndicator(
                                id = "mar_break_1300",
                                day = day,
                                label = "Descanso · 30 min",
                                timeRange = "13:00 → 13:30"
                            )
                        )
                        result.add(ScheduleEntry.ClassCard(s[2]))
                    }

                    UniDay.MIERCOLES -> {
                        val s = sessionsForDay(day)
                        result.add(ScheduleEntry.ClassCard(s[0]))
                        result.add(
                            ScheduleEntry.BreakIndicator(
                                id = "mie_break_1130",
                                day = day,
                                label = "Descanso · 1h 30min",
                                timeRange = "11:30 → 13:00"
                            )
                        )
                        result.add(ScheduleEntry.ClassCard(s[1]))
                        result.add(
                            ScheduleEntry.BreakIndicator(
                                id = "mie_break_1430",
                                day = day,
                                label = "Descanso / Almuerzo · 1h 30min",
                                timeRange = "14:30 → 16:00"
                            )
                        )
                        result.add(ScheduleEntry.ClassCard(s[2]))
                    }

                    UniDay.JUEVES -> {
                        val s = sessionsForDay(day)
                        result.add(ScheduleEntry.ClassCard(s[0]))
                        result.add(
                            ScheduleEntry.BreakIndicator(
                                id = "jue_break_1130",
                                day = day,
                                label = "Descanso · 2h",
                                timeRange = "11:30 → 13:30"
                            )
                        )
                        result.add(ScheduleEntry.ClassCard(s[1]))
                        result.add(
                            ScheduleEntry.BreakIndicator(
                                id = "jue_break_1500",
                                day = day,
                                label = "Descanso / Estudio · 2h 30min",
                                timeRange = "15:00 → 17:30"
                            )
                        )
                        result.add(ScheduleEntry.ClassCard(s[2]))
                    }

                    UniDay.VIERNES -> {
                        sessionsForDay(day).forEach { result.add(ScheduleEntry.ClassCard(it)) }
                    }
                }
            }
        }
        return result
    }

    fun resolveLiveStatus(now: Calendar = Calendar.getInstance()): UniLiveStatus {
        val dow = now.get(Calendar.DAY_OF_WEEK)
        val currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
        val todayUniDay = dayFromCalendarDayOfWeek(dow)

        if (todayUniDay == null) {
            return UniLiveStatus(
                kind = LiveStatusKind.WEEKEND,
                badgeText = "Fin de Semana",
                mainText = "Sin clases hasta el lunes",
                subText = "Lunes 10:00 · MP (Prácticas Grupo L2) · JIM B.1",
                targetDay = UniDay.LUNES
            )
        }

        val todayClasses = sessionsForDay(todayUniDay)
        val activeClass = todayClasses.find { currentMinutes >= it.startMinutes && currentMinutes < it.endMinutes }
        val nextClass = todayClasses.find { it.startMinutes > currentMinutes }

        return when {
            activeClass != null -> {
                val minLeft = activeClass.endMinutes - currentMinutes
                UniLiveStatus(
                    kind = LiveStatusKind.ACTIVE_NOW,
                    badgeText = "EN CURSO AHORA",
                    mainText = activeClass.liveTitle,
                    subText = "${activeClass.liveRoomLabel} · Termina a las ${activeClass.endHHmm} (quedan $minLeft min)",
                    targetDay = todayUniDay,
                    highlightedStartMinutes = activeClass.startMinutes,
                    activeWindowStartMinutes = activeClass.startMinutes
                )
            }

            nextClass != null -> {
                val minUntil = nextClass.startMinutes - currentMinutes
                val hoursUntil = minUntil / 60
                val remMin = minUntil % 60
                val waitStr = if (hoursUntil > 0) "${hoursUntil}h ${remMin}min" else "$minUntil min"
                UniLiveStatus(
                    kind = LiveStatusKind.NEXT_TODAY,
                    badgeText = "SIGUIENTE CLASE",
                    mainText = "${nextClass.liveTitle} a las ${nextClass.startHHmm}",
                    subText = "${nextClass.liveRoomLabel} · Empieza en $waitStr",
                    targetDay = todayUniDay,
                    highlightedStartMinutes = nextClass.startMinutes,
                    nextStartMinutes = nextClass.startMinutes
                )
            }

            else -> {
                val daysOrder = UniDay.entries
                val nextIndex = (daysOrder.indexOf(todayUniDay) + 1) % daysOrder.size
                val nextDay = daysOrder[nextIndex]
                val nextDayFirst = sessionsForDay(nextDay).first()
                UniLiveStatus(
                    kind = LiveStatusKind.DAY_FINISHED,
                    badgeText = "Jornada Finalizada",
                    mainText = "Has completado todas las clases de hoy",
                    subText = "Próxima: ${nextDay.fullLabel} ${nextDayFirst.startHHmm} (${nextDayFirst.liveTitle})",
                    targetDay = todayUniDay
                )
            }
        }
    }
}
