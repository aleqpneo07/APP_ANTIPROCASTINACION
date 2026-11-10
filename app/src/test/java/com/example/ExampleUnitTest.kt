package com.example

import com.example.model.ExternalCalendarEvent
import com.example.model.NightTaskEntity
import com.example.model.QuickInputParser
import com.example.model.TaskCategory
import com.example.model.TaskPriority
import com.example.util.DateTimeHelper
import org.junit.Assert.*
import org.junit.Test

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
}
