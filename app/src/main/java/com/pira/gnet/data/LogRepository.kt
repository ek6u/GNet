package com.pira.gnet.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRepository @Inject constructor() {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs
    
    fun addLog(message: String, level: LogLevel = LogLevel.INFO) {
        val newLogs = _logs.value.toMutableList()
        newLogs.add(LogEntry(System.currentTimeMillis(), message, level))
        
        // Keep only the last 100 logs
        if (newLogs.size > 100) {
            newLogs.removeAt(0)
        }
        
        _logs.value = newLogs
    }
    
    fun clearLogs() {
        _logs.value = emptyList()
    }
}

data class LogEntry(
    val timestamp: Long,
    val message: String,
    val level: LogLevel
)

enum class LogLevel {
    INFO,
    WARNING,
    ERROR
}