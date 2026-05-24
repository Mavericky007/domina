package com.domina.cycle.backup

import java.util.Base64

object BackupCodec {
    private const val HEADER = "DOMINA-BACKUP-1"
    private val enc = Base64.getUrlEncoder().withoutPadding()
    private val dec = Base64.getUrlDecoder()

    fun encode(tables: Map<String, List<List<String>>>): String {
        val sb = StringBuilder(HEADER).append('\n')
        tables.forEach { (table, rows) ->
            sb.append('#').append(table).append('\n')
            rows.forEach { row ->
                sb.append(row.joinToString("|") { enc.encodeToString(it.toByteArray(Charsets.UTF_8)) }).append('\n')
            }
        }
        return sb.toString()
    }

    fun decode(text: String): Map<String, List<List<String>>> {
        val lines = text.split('\n')
        require(lines.isNotEmpty() && lines[0] == HEADER) { "Unrecognized backup header" }
        val out = LinkedHashMap<String, MutableList<List<String>>>()
        var current: MutableList<List<String>>? = null
        for (i in 1 until lines.size) {
            val line = lines[i]
            when {
                line.isEmpty() -> {} // trailing/blank lines
                line.startsWith('#') -> { current = mutableListOf(); out[line.substring(1)] = current }
                else -> current?.add(line.split("|").map { String(dec.decode(it), Charsets.UTF_8) })
            }
        }
        return out
    }
}
