package com.federated.hospital.logging

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentLinkedQueue

data class LogEntry(
    val timestamp: Long = System.currentTimeMillis(),
    val level: String,
    val tag: String,
    val message: String
)

@Component
class LogBuffer {
    private val buffer = ConcurrentLinkedQueue<LogEntry>()
    private val maxCapacity = 200

    fun addLog(level: String, tag: String, message: String) {
        val entry = LogEntry(level = level, tag = tag, message = message)
        buffer.add(entry)
        while (buffer.size > maxCapacity) {
            buffer.poll()
        }
    }

    fun getRecentLogs(limit: Int = 50): List<LogEntry> {
        return buffer.toList().takeLast(limit.coerceAtMost(maxCapacity)).reversed()
    }

    fun clear() {
        buffer.clear()
    }
}
