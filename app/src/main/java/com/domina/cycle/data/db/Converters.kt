package com.domina.cycle.data.db

import androidx.room.TypeConverter

class Converters {
    @TypeConverter fun fromSymptomList(list: List<String>): String = list.joinToString("")
    @TypeConverter fun toSymptomList(s: String): List<String> =
        if (s.isEmpty()) emptyList() else s.split("")
}
