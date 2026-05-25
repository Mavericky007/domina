package com.domina.cycle.backup

import com.domina.cycle.data.db.AppDatabase
import com.domina.cycle.data.db.entity.AppointmentEntity
import com.domina.cycle.data.db.entity.ChecklistItemEntity
import com.domina.cycle.data.db.entity.ContractionEntity
import com.domina.cycle.data.db.entity.CycleEventEntity
import com.domina.cycle.data.db.entity.DayLogEntity
import com.domina.cycle.data.db.entity.KickSessionEntity
import com.domina.cycle.data.db.entity.MedicationEntity
import com.domina.cycle.data.db.entity.WeightEntryEntity
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

/** Gathers all Room data, seals it with a passphrase, and restores it. Streams come from SAF Uris in the UI. */
class BackupManager @Inject constructor(private val db: AppDatabase) {

    // U+001F unit separator — same delimiter used by Converters.kt for the symptoms list.
    private val SEP = ""

    suspend fun export(out: OutputStream, passphrase: CharArray) {
        val tables = linkedMapOf(
            "day_logs" to db.dayLogDao().getAll().map {
                listOf(
                    it.date,
                    it.mood ?: "",
                    it.energy ?: "",
                    it.flow,
                    it.symptoms.joinToString(SEP),
                    it.bbt?.toString() ?: "",
                    it.cervicalMucus ?: "",
                    it.lhResult,
                    it.libido?.toString() ?: "",
                    it.sleepHours?.toString() ?: "",
                    it.weight?.toString() ?: "",
                    it.note,
                    it.intimacy,
                    it.emergencyContraception.toString(),
                    it.pregnancyTest,
                )
            },
            "cycle_events" to db.cycleEventDao().getAll().map {
                listOf(it.id.toString(), it.date, it.type)
            },
            "medications" to db.medicationDao().getAll().map {
                listOf(it.id.toString(), it.name, it.timeMinutes.toString(), it.enabled.toString())
            },
            "appointments" to db.appointmentDao().getAll().map {
                listOf(it.id.toString(), it.title, it.atEpochMillis.toString(), it.note, it.leadHours.toString(), it.enabled.toString())
            },
            "weight_entries" to db.weightEntryDao().getAll().map {
                listOf(it.id.toString(), it.dateEpochDay.toString(), it.weightKg.toString())
            },
            "checklist_items" to db.checklistItemDao().getAll().map {
                listOf(it.id.toString(), it.category, it.text, it.checked.toString(), it.sortOrder.toString())
            },
            "kick_sessions" to db.kickSessionDao().getAll().map {
                listOf(it.id.toString(), it.startMillis.toString(), it.endMillis.toString(), it.count.toString())
            },
            "contractions" to db.contractionDao().getAll().map {
                listOf(it.id.toString(), it.startMillis.toString(), it.endMillis.toString())
            },
        )
        val sealed = BackupCrypto.seal(BackupCodec.encode(tables).toByteArray(Charsets.UTF_8), passphrase)
        out.use { it.write(sealed) }
    }

    suspend fun import(input: InputStream, passphrase: CharArray) {
        val sealed = input.use { it.readBytes() }
        val tables = BackupCodec.decode(String(BackupCrypto.open(sealed, passphrase), Charsets.UTF_8))

        // Replace all data with the backup's contents.
        db.dayLogDao().clearAll()
        db.cycleEventDao().clearAll()
        db.medicationDao().clearAll()
        db.appointmentDao().clearAll()
        db.weightEntryDao().clearAll()
        db.checklistItemDao().clearAll()
        db.kickSessionDao().clearAll()
        db.contractionDao().clearAll()

        tables["day_logs"]?.forEach { r ->
            db.dayLogDao().upsert(
                DayLogEntity(
                    date = r[0],
                    mood = r[1].ifEmpty { null },
                    energy = r[2].ifEmpty { null },
                    flow = r[3],
                    symptoms = if (r[4].isEmpty()) emptyList() else r[4].split(SEP),
                    bbt = r[5].toDoubleOrNull(),
                    cervicalMucus = r[6].ifEmpty { null },
                    lhResult = r[7],
                    libido = r[8].toIntOrNull(),
                    sleepHours = r[9].toDoubleOrNull(),
                    weight = r[10].toDoubleOrNull(),
                    note = r[11],
                    intimacy = r.getOrNull(12)?.ifEmpty { null } ?: "NONE",
                    emergencyContraception = r.getOrNull(13).toBoolean(),
                    pregnancyTest = r.getOrNull(14)?.ifEmpty { null } ?: "NOT_TESTED",
                )
            )
        }
        tables["cycle_events"]?.forEach { r ->
            db.cycleEventDao().insert(CycleEventEntity(date = r[1], type = r[2]))
        }
        tables["medications"]?.forEach { r ->
            db.medicationDao().insert(MedicationEntity(name = r[1], timeMinutes = r[2].toInt(), enabled = r[3].toBoolean()))
        }
        tables["appointments"]?.forEach { r ->
            db.appointmentDao().insert(AppointmentEntity(title = r[1], atEpochMillis = r[2].toLong(), note = r[3], leadHours = r[4].toInt(), enabled = r[5].toBoolean()))
        }
        tables["weight_entries"]?.forEach { r ->
            db.weightEntryDao().insert(WeightEntryEntity(dateEpochDay = r[1].toLong(), weightKg = r[2].toDouble()))
        }
        tables["checklist_items"]?.forEach { r ->
            db.checklistItemDao().insert(ChecklistItemEntity(category = r[1], text = r[2], checked = r[3].toBoolean(), sortOrder = r[4].toInt()))
        }
        tables["kick_sessions"]?.forEach { r ->
            db.kickSessionDao().insert(KickSessionEntity(startMillis = r[1].toLong(), endMillis = r[2].toLong(), count = r[3].toInt()))
        }
        tables["contractions"]?.forEach { r ->
            db.contractionDao().insert(ContractionEntity(startMillis = r[1].toLong(), endMillis = r[2].toLong()))
        }
    }
}
