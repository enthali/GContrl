package de.drachenfels.gcontrl.utils

import de.drachenfels.gcontrl.BuildConfig

/**
 * Central configuration for the logging system.
 * All debug flags and component tags are managed here.
 */
object LogConfig {
    // Debug flags
    var ENABLE_DEBUG = BuildConfig.DEBUG      // Master switch for debug
    var ENABLE_DEBUG_MAIN = true              // App logging
    var ENABLE_DEBUG_MQTT = true            // MQTT logging
    var ENABLE_DEBUG_SETTINGS = false        // Settings debug flag
    var ENABLE_DEBUG_LOCATION = false        // Location debug flag
    var ENABLE_DEBUG_NOTIFICATION = false     // Notification debug flag

    // Standard tags for components
    const val TAG_MAIN = "GPLog: MainActivity"           // General app tag
    const val TAG_MQTT = "GPLog: MQTT"              // MQTT specific
    const val TAG_SETTINGS = "GPLog: Settings"      // Settings related
    const val TAG_LOCATION = "GPLog: LocAuto"       // LocationAutomationService
    const val TAG_NOTIFICATION = "GPLog: Notify"    // Notification related

    // Computed property for debug status
    val isDebuggingEnabled: Boolean
        get() = ENABLE_DEBUG && (
                ENABLE_DEBUG_MAIN ||
                ENABLE_DEBUG_MQTT || 
                ENABLE_DEBUG_SETTINGS ||
                ENABLE_DEBUG_LOCATION ||
                ENABLE_DEBUG_NOTIFICATION)
}

/**
 * Interface for the logging system.
 * Allows different implementations for Android and tests.
 */
interface Logger {
    fun d(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable? = null)
    fun i(tag: String, message: String)
    fun w(tag: String, message: String)
}

/**
 * Android-specific logger implementation.
 * Uses Android's Log system and considers debug flags.
 */
class AndroidLogger : Logger {
    override fun d(tag: String, message: String) {
        if (LogConfig.isDebuggingEnabled && shouldLog(tag)) {
            android.util.Log.d(tag, message)
        }
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        if (LogConfig.isDebuggingEnabled && shouldLog(tag)) {
            android.util.Log.e(tag, message, throwable)
        }
    }

    override fun i(tag: String, message: String) {
        if (LogConfig.isDebuggingEnabled && shouldLog(tag)) {
            android.util.Log.i(tag, message)
        }
    }

    override fun w(tag: String, message: String) {
        if (LogConfig.isDebuggingEnabled && shouldLog(tag)) {
            android.util.Log.w(tag, message)
        }
    }

    private fun shouldLog(tag: String): Boolean {
        return when (tag) {
            LogConfig.TAG_LOCATION -> LogConfig.ENABLE_DEBUG_LOCATION
            LogConfig.TAG_MQTT -> LogConfig.ENABLE_DEBUG_MQTT
            LogConfig.TAG_SETTINGS -> LogConfig.ENABLE_DEBUG_SETTINGS
            LogConfig.TAG_NOTIFICATION -> LogConfig.ENABLE_DEBUG_NOTIFICATION
            else -> true
        }
    }
}

/**
 * Test-specific logger implementation.
 * Outputs logs to console and ignores debug flags.
 */
class TestLogger : Logger {
    override fun d(tag: String, message: String) {
        println("DEBUG/$tag: $message")
    }

    override fun e(tag: String, message: String, throwable: Throwable?) {
        println("ERROR/$tag: $message")
        throwable?.printStackTrace()
    }

    override fun i(tag: String, message: String) {
        println("INFO/$tag: $message")
    }

    override fun w(tag: String, message: String) {
        println("WARN/$tag: $message")
    }
}