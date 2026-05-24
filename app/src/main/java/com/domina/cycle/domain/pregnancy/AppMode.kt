package com.domina.cycle.domain.pregnancy

enum class AppMode { CYCLE, TTC, PREGNANCY;
    companion object {
        fun fromName(name: String?): AppMode = entries.firstOrNull { it.name == name } ?: CYCLE
    }
}
