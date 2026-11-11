package com.example

import com.example.model.ExternalCalendarEvent
import com.example.model.LiveStatusKind
import com.example.model.NightTaskEntity
import com.example.model.QuickInputParser
import com.example.model.ScheduleEntry
import com.example.model.TaskCategory
import com.example.model.TaskPriority
import com.example.model.UniDay
import com.example.model.UniversityScheduleRepository
import com.example.util.DateTimeHelper
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {
  @Test
  fun quickInputParser_extractsCategoryPriorityAndTimeRange() {
    val parsed = QuickInputParser.parse("Estudiar Cálculo #uni !alta @09:00-11:00")
    assertEquals("Estudiar Cálculo", parsed.cleanTitle)
    assertEquals(TaskCategory.UNIVERSIDAD, parsed.detectedCategory)
    assertEquals(TaskPriority.ALTA, parsed.detectedPriority)
    assertEquals("09:00", parsed.detectedStartTime)
    assertEquals("11:00", parsed.detectedEndTime)
    assertEquals(120, parsed.detectedDurationMinutes)
  }

  @Test
  fun quickInputParser_calculatesEndTimeFromDuration() {
    val parsed = QuickInputParser.parse("Entrenar pierna #gym !media @18:00 +45m")
    assertEquals("Entrenar pierna", parsed.cleanTitle)
    assertEquals(TaskCategory.GIMNASIO, parsed.detectedCategory)
    assertEquals(TaskPriority.MEDIA, parsed.detectedPriority)
    assertEquals("18:00", parsed.detectedStartTime)
    assertEquals("18:45", parsed.detectedEndTime)
    assertEquals(45, parsed.detectedDurationMinutes)
  }

  @Test
  fun dateTimeHelper_detectsOverlapsAndIgnoresSyncedOwnEvent() {
    val tasks = listOf(
      NightTaskEntity(
        id = 1L,
        title = "Tarea Sincronizada",
        targetDateIso = "2026-11-11",
        startTime = "09:00",
        endTime = "10:30",
        googleEventId = "gcal_1"
      ),
      NightTaskEntity(
        id = 2L,
        title = "Tarea en Conflicto",
        targetDateIso = "2026-11-11",
        startTime = "10:00",
        endTime = "11:00"
      )
    )
    val externalEvents = listOf(
      ExternalCalendarEvent(
        id = "gcal_1",
        title = "Tarea Sincronizada",
        dateIso = "2026-11-11",
        startTimeHHmm = "09:00",
        endTimeHHmm = "10:30",
        isAllDay = false,
        isFromNightPlanner = true
      )
    )

    val overlaps = DateTimeHelper.detectScheduleOverlaps(tasks, externalEvents)
    assertEquals(1, overlaps.size)
    assertEquals("09:00–10:30".substring(6), overlaps.first().overlapWindow.substring(6))
  }

  @Test
  fun universitySchedule_hasExact16ClassesAndPractices() {
    assertEquals(16, UniversityScheduleRepository.allSessions.size)
    assertEquals(5, UniversityScheduleRepository.sessionsForDay(UniDay.LUNES).size)
    assertEquals(3, UniversityScheduleRepository.sessionsForDay(UniDay.MARTES).size)
    assertEquals(3, UniversityScheduleRepository.sessionsForDay(UniDay.MIERCOLES).size)
    assertEquals(3, UniversityScheduleRepository.sessionsForDay(UniDay.JUEVES).size)
    assertEquals(2, UniversityScheduleRepository.sessionsForDay(UniDay.VIERNES).size)

    val practiceCards = UniversityScheduleRepository.entriesForSelection(
      selectedDay = null,
      onlyPractices = true
    ).filterIsInstance<ScheduleEntry.ClassCard>()
    assertEquals(4, practiceCards.size)
  }

  @Test
  fun universitySchedule_liveStatusDetectsActiveClassAndWeekend() {
    val mondayActive = Calendar.getInstance().apply {
      set(2026, Calendar.NOVEMBER, 9, 10, 30, 0) // Monday 10:30 (MP Prácticas 10:00-11:30)
    }
    val statusActive = UniversityScheduleRepository.resolveLiveStatus(mondayActive)
    assertEquals(LiveStatusKind.ACTIVE_NOW, statusActive.kind)
    assertEquals(UniDay.LUNES, statusActive.targetDay)
    assertEquals(600, statusActive.activeWindowStartMinutes)

    val sundayWeekend = Calendar.getInstance().apply {
      set(2026, Calendar.NOVEMBER, 8, 12, 0, 0) // Sunday
    }
    val statusWeekend = UniversityScheduleRepository.resolveLiveStatus(sundayWeekend)
    assertEquals(LiveStatusKind.WEEKEND, statusWeekend.kind)
    assertEquals(UniDay.LUNES, statusWeekend.targetDay)
  }

  @Test
  fun universitySchedule_virtualEventsDetectTaskOverlap() {
    // 2026-11-11 is Wednesday: AMC 10:00-11:30, RA 13:00-14:30, PHVIM Prac 16:00-17:30
    val uniEvents = UniversityScheduleRepository.toVirtualExternalEvents("2026-11-11")
    assertEquals(3, uniEvents.size)

    val collidingTask = NightTaskEntity(
      id = 10L,
      title = "Gimnasio",
      targetDateIso = "2026-11-11",
      startTime = "16:30",
      endTime = "17:30"
    )
    val overlaps = DateTimeHelper.detectScheduleOverlaps(listOf(collidingTask), uniEvents)
    assertEquals(1, overlaps.size)
    assertTrue(overlaps.first().conflictingWithTitle.contains("PHVIM"))
  }
}
