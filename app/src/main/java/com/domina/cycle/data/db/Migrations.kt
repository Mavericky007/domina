package com.domina.cycle.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `day_logs` ADD COLUMN `intimacy` TEXT NOT NULL DEFAULT 'NONE'")
        db.execSQL("ALTER TABLE `day_logs` ADD COLUMN `emergencyContraception` INTEGER NOT NULL DEFAULT 0")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `day_logs` ADD COLUMN `pregnancyTest` TEXT NOT NULL DEFAULT 'NOT_TESTED'")
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `medications` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, `timeMinutes` INTEGER NOT NULL, `enabled` INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `appointments` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`title` TEXT NOT NULL, `atEpochMillis` INTEGER NOT NULL, " +
                "`note` TEXT NOT NULL, `leadHours` INTEGER NOT NULL, `enabled` INTEGER NOT NULL)"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `kick_sessions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startMillis` INTEGER NOT NULL, `endMillis` INTEGER NOT NULL, `count` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `contractions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `startMillis` INTEGER NOT NULL, `endMillis` INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `weight_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `dateEpochDay` INTEGER NOT NULL, `weightKg` REAL NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `checklist_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category` TEXT NOT NULL, `text` TEXT NOT NULL, `checked` INTEGER NOT NULL, `sortOrder` INTEGER NOT NULL)")
    }
}
