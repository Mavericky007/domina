package com.domina.cycle.data.prefs

enum class ThemePreference(val key: String, val displayName: String) {
    SOFT_SWEET("soft_sweet", "Soft & Sweet"),
    BRIGHT_JOYFUL("bright_joyful", "Bright & Joyful"),
    WARM_COZY("warm_cozy", "Warm & Cozy");

    companion object {
        fun fromKey(key: String?): ThemePreference =
            entries.firstOrNull { it.key == key } ?: SOFT_SWEET
    }
}
