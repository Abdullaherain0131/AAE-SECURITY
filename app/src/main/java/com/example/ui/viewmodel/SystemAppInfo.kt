package com.example.ui.viewmodel

data class SystemAppInfo(
    val appName: String,
    val packageName: String,
    val isEnabled: Boolean,
    val isSystem: Boolean,
    val memoryUsageKb: Long
)
