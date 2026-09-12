package com.example.util

import android.content.Context
import android.content.SharedPreferences

class ProtectionPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("antivirus_protection_prefs", Context.MODE_PRIVATE)

    var isRealTimeProtectionEnabled: Boolean
        get() = prefs.getBoolean(KEY_REALTIME_PROTECTION, true)
        set(value) = prefs.edit().putBoolean(KEY_REALTIME_PROTECTION, value).apply()

    var isAutoScanNewAppsEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTOSCAN_NEW_APPS, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTOSCAN_NEW_APPS, value).apply()

    var isHeuristicAnalysisEnabled: Boolean
        get() = prefs.getBoolean(KEY_HEURISTIC_ANALYSIS, true)
        set(value) = prefs.edit().putBoolean(KEY_HEURISTIC_ANALYSIS, value).apply()

    var lastScanTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SCAN, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SCAN, value).apply()

    var lastDailyReportTimestamp: Long
        get() = prefs.getLong(KEY_LAST_DAILY_REPORT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_DAILY_REPORT, value).apply()

    var isKvkkAccepted: Boolean
        get() = prefs.getBoolean(KEY_KVKK_ACCEPTED, false)
        set(value) = prefs.edit().putBoolean(KEY_KVKK_ACCEPTED, value).apply()

    var isBackgroundAiLearningEnabled: Boolean
        get() = prefs.getBoolean(KEY_AI_LEARNING, true)
        set(value) = prefs.edit().putBoolean(KEY_AI_LEARNING, value).apply()


    var isDarkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_THEME, value).apply()

    var isShizukuEnabled: Boolean
        get() = prefs.getBoolean(KEY_SHIZUKU_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SHIZUKU_ENABLED, value).apply()
        
    var isEnergyEfficiencyModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENERGY_EFFICIENCY, false)
        set(value) = prefs.edit().putBoolean(KEY_ENERGY_EFFICIENCY, value).apply()

    var energyOptimizationScheduleMode: Int
        get() = prefs.getInt(KEY_ENERGY_SCHEDULE, 0) // 0: Kapalı, 1: Her 6 Saatte, 2: Günde 1 Kez
        set(value) = prefs.edit().putInt(KEY_ENERGY_SCHEDULE, value).apply()

    var scanScheduleMode: Int
        get() = prefs.getInt(KEY_SCAN_SCHEDULE, 1) // 0: Kapalı, 1: Günlük, 2: Haftalık
        set(value) = prefs.edit().putInt(KEY_SCAN_SCHEDULE, value).apply()
        
    var scanTargetHour: Int
        get() = prefs.getInt(KEY_SCAN_TARGET_HOUR, 3) // Varsayılan gece 03:00
        set(value) = prefs.edit().putInt(KEY_SCAN_TARGET_HOUR, value).apply()
        
    var scanTargetMinute: Int
        get() = prefs.getInt(KEY_SCAN_TARGET_MINUTE, 0)
        set(value) = prefs.edit().putInt(KEY_SCAN_TARGET_MINUTE, value).apply()

    var activeAiModelName: String
        get() = prefs.getString(KEY_AI_MODEL_NAME, "Yerel MLP Motoru (Varsayılan)") ?: "Yerel MLP Motoru (Varsayılan)"
        set(value) = prefs.edit().putString(KEY_AI_MODEL_NAME, value).apply()

    var activeAiModelParams: String
        get() = prefs.getString(KEY_AI_MODEL_PARAMS, "1224 (Yerel MLP)") ?: "1224 (Yerel MLP)"
        set(value) = prefs.edit().putString(KEY_AI_MODEL_PARAMS, value).apply()

    var activeDnsProfileId: String
        get() = prefs.getString(KEY_ACTIVE_DNS_PROFILE_ID, "cloudflare") ?: "cloudflare"
        set(value) = prefs.edit().putString(KEY_ACTIVE_DNS_PROFILE_ID, value).apply()

    var primaryDns: String
        get() = prefs.getString(KEY_PRIMARY_DNS, "1.1.1.1") ?: "1.1.1.1"
        set(value) = prefs.edit().putString(KEY_PRIMARY_DNS, value).apply()

    var secondaryDns: String
        get() = prefs.getString(KEY_SECONDARY_DNS, "1.0.0.1") ?: "1.0.0.1"
        set(value) = prefs.edit().putString(KEY_SECONDARY_DNS, value).apply()

    var dynamicBlockedKeywords: Set<String>
        get() = prefs.getStringSet(KEY_DYNAMIC_BLOCKED_KEYWORDS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_DYNAMIC_BLOCKED_KEYWORDS, value).apply()

    var dynamicThreatSignatures: Set<String>
        get() = prefs.getStringSet(KEY_DYNAMIC_THREAT_SIGNATURES, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_DYNAMIC_THREAT_SIGNATURES, value).apply()

    companion object {
        private const val KEY_REALTIME_PROTECTION = "key_realtime_protection"
        private const val KEY_AUTOSCAN_NEW_APPS = "key_autoscan_new_apps"
        private const val KEY_HEURISTIC_ANALYSIS = "key_heuristic_analysis"
        private const val KEY_LAST_SCAN = "key_last_scan"
        private const val KEY_LAST_DAILY_REPORT = "key_last_daily_report"
        private const val KEY_KVKK_ACCEPTED = "key_kvkk_accepted"
        private const val KEY_AI_LEARNING = "key_ai_learning"

        private const val KEY_DARK_THEME = "key_dark_theme"
        private const val KEY_SHIZUKU_ENABLED = "key_shizuku_enabled"
        private const val KEY_ENERGY_EFFICIENCY = "key_energy_efficiency"
        private const val KEY_ENERGY_SCHEDULE = "key_energy_schedule"
        private const val KEY_SCAN_SCHEDULE = "key_scan_schedule"
        private const val KEY_SCAN_TARGET_HOUR = "key_scan_target_hour"
        private const val KEY_SCAN_TARGET_MINUTE = "key_scan_target_minute"
        private const val KEY_AI_MODEL_NAME = "key_ai_model_name"
        private const val KEY_AI_MODEL_PARAMS = "key_ai_model_params"

        private const val KEY_ACTIVE_DNS_PROFILE_ID = "key_active_dns_profile_id"
        private const val KEY_PRIMARY_DNS = "key_primary_dns"
        private const val KEY_SECONDARY_DNS = "key_secondary_dns"
        private const val KEY_DYNAMIC_BLOCKED_KEYWORDS = "key_dynamic_blocked_keywords"
        private const val KEY_DYNAMIC_THREAT_SIGNATURES = "key_dynamic_threat_signatures"

        val DNS_PROFILES = listOf(
            DnsProfilePreset(
                id = "cloudflare",
                name = "Cloudflare (1.1.1.1)",
                description = "Ultra hızlı çözümleme ve gizlilik odaklı DNS.",
                primary = "1.1.1.1",
                secondary = "1.0.0.1",
                badge = "Hızlı & Güvenli"
            ),
            DnsProfilePreset(
                id = "adguard",
                name = "AdGuard DNS",
                description = "Zararlı reklamları, pop-up'ları ve izleyicileri engeller.",
                primary = "94.140.14.14",
                secondary = "94.140.15.15",
                badge = "Reklam Engelleyici"
            ),
            DnsProfilePreset(
                id = "google",
                name = "Google Public DNS",
                description = "Yüksek güvenilirlik ve küresel ölçekte hızlı DNS.",
                primary = "8.8.8.8",
                secondary = "8.8.4.4",
                badge = "Yüksek Hız"
            ),
            DnsProfilePreset(
                id = "quad9",
                name = "Quad9 Security",
                description = "Bilinen kötü amaçlı yazılım ve kimlik avı sitelerini engeller.",
                primary = "9.9.9.9",
                secondary = "149.112.112.112",
                badge = "Zararlı Yazılım Kalkanı"
            ),
            DnsProfilePreset(
                id = "opendns",
                name = "OpenDNS Home",
                description = "Aile güvenliği, phishing koruması ve içerik filtreleme.",
                primary = "208.67.222.222",
                secondary = "208.67.220.220",
                badge = "Aile Koruması"
            )
        )
    }
}

data class DnsProfilePreset(
    val id: String,
    val name: String,
    val description: String,
    val primary: String,
    val secondary: String,
    val badge: String
)

