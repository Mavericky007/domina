package com.domina.cycle.domain.reminders

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object MedAppointmentScheduler {
    const val CHANNEL = "meds_appointments"

    data class MedInput(val id: Long, val name: String, val timeMinutes: Int, val enabled: Boolean)
    data class ApptInput(val id: Long, val title: String, val atEpochMillis: Long, val leadHours: Int, val enabled: Boolean)

    fun compute(meds: List<MedInput>, appts: List<ApptInput>, now: LocalDateTime, zone: ZoneId): List<ScheduledItem> {
        val out = mutableListOf<ScheduledItem>()
        meds.filter { it.enabled }.forEach { m ->
            val today = now.toLocalDate().atTime(m.timeMinutes / 60, m.timeMinutes % 60)
            val fireAt = if (today.isAfter(now)) today else today.plusDays(1)
            out += ScheduledItem("med_${m.id}", fireAt, CHANNEL, "Time for ${m.name} 💊", "A gentle reminder to take ${m.name}.")
        }
        appts.filter { it.enabled }.forEach { a ->
            val at = LocalDateTime.ofInstant(Instant.ofEpochMilli(a.atEpochMillis), zone)
            val fireAt = at.minusHours(a.leadHours.toLong())
            if (fireAt.isAfter(now)) {
                out += ScheduledItem("appt_${a.id}", fireAt, CHANNEL, "Upcoming: ${a.title} 📅",
                    "You have \"${a.title}\" in about ${a.leadHours} hours.")
            }
        }
        return out
    }
}
