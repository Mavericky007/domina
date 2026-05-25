package com.domina.cycle.domain.update

/** Compares dotted version strings (e.g. "1.2", "v1.10-beta") segment-by-segment, numerically. */
object VersionCompare {

    /** True when [latest] represents a strictly newer version than [current]. */
    fun isNewer(current: String, latest: String): Boolean {
        val c = parse(current)
        val l = parse(latest)
        for (i in 0 until maxOf(c.size, l.size)) {
            val cv = c.getOrElse(i) { 0 }
            val lv = l.getOrElse(i) { 0 }
            if (lv != cv) return lv > cv
        }
        return false
    }

    /** "v1.10-beta" -> [1, 10]; ignores non-numeric suffixes. */
    private fun parse(v: String): List<Int> =
        v.trim().removePrefix("v").removePrefix("V")
            .split('.', '-', '+', '_')
            .mapNotNull { seg -> seg.takeWhile(Char::isDigit).toIntOrNull() }
}
