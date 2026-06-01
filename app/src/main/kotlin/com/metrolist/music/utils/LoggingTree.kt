package com.metrolist.music.utils

import android.util.Log
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: String,
    val priority: Int,
    val tag: String?,
    val message: String
)

object LoggingTree : Timber.Tree() {
    private const val MAX_LOG_ENTRIES = 1000
    private val logs = mutableListOf<LogEntry>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    
    @Volatile
    var isEnabled: Boolean = false
    private var cacheDir: File? = null

    fun initialize(cacheDir: File) {
        this.cacheDir = cacheDir
        val logFile = File(cacheDir, "app_logs.txt")
        try {
            logFile.appendText("\n--- New Session: ${Date()} ---\n" +
                    "App Version: ${com.metrolist.music.BuildConfig.VERSION_NAME} (${com.metrolist.music.BuildConfig.VERSION_CODE})\n" +
                    "Device: ${android.os.Build.MODEL} (${android.os.Build.MANUFACTURER})\n" +
                    "OS: Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})\n" +
                    "--------------------------------------------------\n")
        } catch (e: Exception) {
            // Ignore
        }
    }

    @Synchronized
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (!isEnabled) return
        
        val entry = LogEntry(
            timestamp = dateFormat.format(Date()),
            priority = priority,
            tag = tag,
            message = message + (t?.let { "\n" + Log.getStackTraceString(it) } ?: "")
        )
        logs.add(entry)
        if (logs.size > MAX_LOG_ENTRIES) {
            logs.removeAt(0)
        }
        
        // Persist to file
        cacheDir?.let { dir ->
            val logFile = File(dir, "app_logs.txt")
            try {
                logFile.appendText("[${entry.timestamp}] ${entry.tag ?: "APP"}: ${entry.message}\n")
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    @Synchronized
    fun getLogs(): List<LogEntry> = logs.toList()

    @Synchronized
    fun clearLogs() {
        logs.clear()
    }

    fun getLogsAsString(): String {
        return getLogs().joinToString("\n") { 
            "[${it.timestamp}] ${it.tag ?: "APP"}: ${it.message}" 
        }
    }
}
