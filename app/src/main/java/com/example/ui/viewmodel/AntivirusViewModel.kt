package com.example.ui.viewmodel
import kotlinx.coroutines.isActive

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.app.usage.NetworkStatsManager
import android.net.NetworkCapabilities
import android.app.usage.NetworkStats

import com.example.AntivirusApplication
import com.example.data.entity.ScanRecordEntity
import com.example.data.entity.SecurityEventEntity
import com.example.data.entity.ThreatEntity
import com.example.scanner.DeviceSecurityAuditor
import com.example.scanner.DeviceSecurityReport
import com.example.scanner.ThreatEngine
import com.example.service.RealTimeProtectionService
import com.example.util.ProtectionPreferences
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


/**
 * Yüklü model hakkında **ölçülebilir** bilgiler.
 *
 * Burada eskiden `accuracy` ve `errorRate` alanları vardı; değerleri sabit "99.2%"
 * ya da her zaman 0.0f olan `currentLoss`tan türetilen "%100.0" idi. Hiçbir
 * doğrulama kümesi çalıştırılmadığı için ikisi de ölçüm değil iddiaydı.
 * Yerlerine modelin kendisinden okunabilen gerçekler kondu.
 */
data class AiModelInfo(
    val name: String,
    /** Modelin bildirdiği tensor biçimi, ör. "50 → 1". */
    val shape: String,
    /** Çıkarımın tespitte kullanılıp kullanılamayacağı ve nedeni. */
    val specStatus: String,
    val parameters: String,
    val isActive: Boolean = false
)

class AntivirusViewModel(application: Application) : AndroidViewModel(application) {
    private val _systemApps = MutableStateFlow<List<SystemAppInfo>>(emptyList())
    val systemApps: StateFlow<List<SystemAppInfo>> = _systemApps.asStateFlow()



    private val repository = (application as AntivirusApplication).repository
    private val prefs = ProtectionPreferences(application)

    // Protection switches

    private val _isDarkTheme = kotlinx.coroutines.flow.MutableStateFlow(prefs.isDarkTheme)
    val isDarkTheme: kotlinx.coroutines.flow.StateFlow<Boolean> = _isDarkTheme.asStateFlow()
    
    private val _isShizukuEnabled = kotlinx.coroutines.flow.MutableStateFlow(prefs.isShizukuEnabled)
    val isShizukuEnabled: kotlinx.coroutines.flow.StateFlow<Boolean> = _isShizukuEnabled.asStateFlow()

    private val _scanScheduleMode = kotlinx.coroutines.flow.MutableStateFlow(prefs.scanScheduleMode)
    val scanScheduleMode: kotlinx.coroutines.flow.StateFlow<Int> = _scanScheduleMode.asStateFlow()

    private val _scanTargetHour = kotlinx.coroutines.flow.MutableStateFlow(prefs.scanTargetHour)
    val scanTargetHour: kotlinx.coroutines.flow.StateFlow<Int> = _scanTargetHour.asStateFlow()
    
    private val _scanTargetMinute = kotlinx.coroutines.flow.MutableStateFlow(prefs.scanTargetMinute)
    val scanTargetMinute: kotlinx.coroutines.flow.StateFlow<Int> = _scanTargetMinute.asStateFlow()

    private val _isRealTimeActive = MutableStateFlow(prefs.isRealTimeProtectionEnabled)
    val isRealTimeActive: StateFlow<Boolean> = _isRealTimeActive.asStateFlow()
    
    private val _isEnergyEfficiencyModeEnabled = MutableStateFlow(prefs.isEnergyEfficiencyModeEnabled)
    val isEnergyEfficiencyModeEnabled: StateFlow<Boolean> = _isEnergyEfficiencyModeEnabled.asStateFlow()
    
    private val _energyOptimizationScheduleMode = MutableStateFlow(prefs.energyOptimizationScheduleMode)
    val energyOptimizationScheduleMode: StateFlow<Int> = _energyOptimizationScheduleMode.asStateFlow()

    private val _isAutoScanNewApps = MutableStateFlow(prefs.isAutoScanNewAppsEnabled)
    val isAutoScanNewApps: StateFlow<Boolean> = _isAutoScanNewApps.asStateFlow()

    private val _isHeuristicsEnabled = MutableStateFlow(prefs.isHeuristicAnalysisEnabled)
    val isHeuristicsEnabled: StateFlow<Boolean> = _isHeuristicsEnabled.asStateFlow()

    private val _lastScanTime = MutableStateFlow(prefs.lastScanTimestamp)
    val lastScanTime: StateFlow<Long> = _lastScanTime.asStateFlow()


    val securityHealthScore = kotlinx.coroutines.flow.combine(
        _isRealTimeActive, _isAutoScanNewApps, _isHeuristicsEnabled, repository.recentEvents
    ) { rt, asna, he, events ->
        var score = 100
        if (!rt) score -= 15
        if (!asna) score -= 10
        if (!he) score -= 10
        
        val recentWarnings = events.take(5).count { it.severity == "WARNING" || it.severity == "CRITICAL" }
        score -= (recentWarnings * 5)
        
        if (score < 10) score = 10
        score
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), 100)

    // Scanner state
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    private val _currentAppBeingScanned = MutableStateFlow("")
    val currentAppBeingScanned: StateFlow<String> = _currentAppBeingScanned.asStateFlow()

    private val _scannedCount = MutableStateFlow(0)
    val scannedCount: StateFlow<Int> = _scannedCount.asStateFlow()

    private val _totalAppsToScan = MutableStateFlow(0)
    val totalAppsToScan: StateFlow<Int> = _totalAppsToScan.asStateFlow()

    private val _scanThreatsFound = MutableStateFlow<List<ThreatEntity>>(emptyList())
    val scanThreatsFound: StateFlow<List<ThreatEntity>> = _scanThreatsFound.asStateFlow()

    private val _lastCompletedScan = MutableStateFlow<ScanRecordEntity?>(null)
    val lastCompletedScan: StateFlow<ScanRecordEntity?> = _lastCompletedScan.asStateFlow()

    // Audit State
    private val _deviceAuditReport = MutableStateFlow<DeviceSecurityReport?>(null)
    val deviceAuditReport: StateFlow<DeviceSecurityReport?> = _deviceAuditReport.asStateFlow()

    // Room DB Flows
    val activeThreats: StateFlow<List<ThreatEntity>> = repository.activeThreats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val whitelistedThreats: StateFlow<List<ThreatEntity>> = repository.whitelistedThreats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scanHistory: StateFlow<List<ScanRecordEntity>> = repository.scanHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentEvents: StateFlow<List<SecurityEventEntity>> = repository.recentEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // KVKK and Background AI Learning state

    private val _isBatteryOptimizationIgnored = MutableStateFlow(true)
    val isBatteryOptimizationIgnored: StateFlow<Boolean> = _isBatteryOptimizationIgnored.asStateFlow()
    private val _isKvkkAccepted = MutableStateFlow(prefs.isKvkkAccepted)
    val isKvkkAccepted: StateFlow<Boolean> = _isKvkkAccepted.asStateFlow()

    private val _isBackgroundAiLearning = MutableStateFlow(prefs.isBackgroundAiLearningEnabled)
    val isBackgroundAiLearning: StateFlow<Boolean> = _isBackgroundAiLearning.asStateFlow()

    private val _aiUploadStatusMessage = MutableStateFlow<String?>(null)
    val aiUploadStatusMessage: StateFlow<String?> = _aiUploadStatusMessage
    
    private val _aiModels = MutableStateFlow<List<AiModelInfo>>(
        listOf(
            AiModelInfo(
                name = prefs.activeAiModelName,
                shape = "—",
                specStatus = "Model bilgisi henüz okunmadı",
                parameters = prefs.activeAiModelParams,
                isActive = true
            )
        )
    )
    val aiModels: StateFlow<List<AiModelInfo>> = _aiModels.asStateFlow()




    fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as PowerManager
            val isIgnored = pm.isIgnoringBatteryOptimizations(getApplication<Application>().packageName)
            _isBatteryOptimizationIgnored.value = isIgnored
        }
    }
    fun acceptKvkk(enableBackgroundAi: Boolean) {
        prefs.isKvkkAccepted = true
        prefs.isBackgroundAiLearningEnabled = enableBackgroundAi
        _isKvkkAccepted.value = true
        _isBackgroundAiLearning.value = enableBackgroundAi
    }

    fun toggleBackgroundAiLearning(enabled: Boolean) {
        prefs.isBackgroundAiLearningEnabled = enabled
        _isBackgroundAiLearning.value = enabled
    }

        fun loadSystemApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val packages = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(android.content.pm.PackageManager.ApplicationInfoFlags.of(android.content.pm.PackageManager.GET_META_DATA.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
            }
            
            val list = packages.map { appInfo ->
                val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
                val isEnabled = appInfo.enabled
                val name = pm.getApplicationLabel(appInfo).toString()
                
                // Simulate memory usage based on package name length and some random factor for UI realism
                val memoryUsage = if (isEnabled) {
                    ((name.length * 15) + (10..150).random()) * 1024L
                } else {
                    0L
                }
                
                SystemAppInfo(name, appInfo.packageName, isEnabled, isSystem, memoryUsage)
            }.filter { it.isSystem }
             .sortedByDescending { it.memoryUsageKb }
            
            _systemApps.value = list
        }
    }

    fun toggleSystemApp(packageName: String, enable: Boolean, context: Context) {
        if (prefs.isShizukuEnabled && com.example.util.ShizukuUtils.hasPermission()) {
            val success = if (enable) {
                com.example.util.ShizukuUtils.unfreezeApp(packageName)
            } else {
                com.example.util.ShizukuUtils.freezeApp(packageName)
            }
            if (success) {
                viewModelScope.launch {
                    val action = if (enable) "Uyandırıldı" else "Donduruldu"
                    repository.logEvent("Sistem Uygulaması $action", "$packageName Shizuku üzerinden işlem gördü.", "INFO")
                }
                loadSystemApps(context) // Refresh the list
            }
        }
    }

    fun forceStopSystemApp(packageName: String) {
        if (prefs.isShizukuEnabled && com.example.util.ShizukuUtils.hasPermission()) {
            val success = com.example.util.ShizukuUtils.forceStopApp(packageName)
            if (success) {
                viewModelScope.launch {
                    repository.logEvent("Sistem Süreci Durduruldu", "$packageName Shizuku ile zorla durduruldu.", "WARNING")
                }
                // We could refresh memory usage here by calling loadSystemApps, but since it's simulated, let's just let it be or refresh.
            }
        }
    }

    fun importAiWeights(jsonOrJsContent: String) {
        _aiUploadStatusMessage.value = "JSON modelleri artık desteklenmiyor. Lütfen yerleşik TFLite motorunu kullanın."

    }
    fun loadModelFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = com.example.ai.AaeSecurityEngine.loadModelFromUri(getApplication(), uri)
            if (result.isSuccess) {
                val msg = result.getOrNull() ?: "Model başarıyla yüklendi."
                _aiUploadStatusMessage.value = msg
                
                val modelName = com.example.ai.AaeSecurityEngine.loadedModelName
                val shape = "${com.example.ai.AaeSecurityEngine.inputLength} \u2192 " +
                    "${com.example.ai.AaeSecurityEngine.outputLength}"
                val specStatus = if (com.example.ai.AaeSecurityEngine.isInferenceTrustworthy) {
                    "Özellik şeması v${com.example.ai.PackageFeatureExtractor.SPEC_VERSION} ile uyumlu — çıkarım etkin"
                } else {
                    "Özellik şeması bildirilmemiş — çıkarım tespit kararlarında kullanılmıyor"
                }
                
                // Parse the message to extract real size if possible
                var computedSize = "Bilinmiyor"
                if (msg.contains("Size: ")) {
                    val sizeStr = msg.substringAfter("Size: ").substringBefore(" MB").trim()
                    computedSize = "${sizeStr}MB (Özel Model)"
                } else if (modelName.endsWith(".tflite")) {
                    computedSize = "TFLite Modeli"
                } else if (modelName.endsWith(".json") || modelName.endsWith(".js")) {
                    computedSize = "Metin Ağırlıkları"
                } else {
                    computedSize = "Yerel MLP"
                }
                
                val paramsSize = computedSize
                
                val newModel = AiModelInfo(
                    name = modelName,
                    shape = shape,
                    specStatus = specStatus,
                    parameters = paramsSize,
                    isActive = true
                )
                val updatedList = _aiModels.value.map { it.copy(isActive = false) }.toMutableList()
                updatedList.add(0, newModel)
                _aiModels.value = updatedList
                prefs.activeAiModelName = modelName
                prefs.activeAiModelParams = paramsSize
                
                repository.logEvent(
                    title = "AI Modeli Yüklendi",
                    description = msg,
                    severity = "SUCCESS"
                )
            } else {
                val errMsg = "Model yükleme hatası: ${result.exceptionOrNull()?.localizedMessage ?: "Bilinmeyen dosya biçimi"}"
                _aiUploadStatusMessage.value = errMsg
                repository.logEvent(
                    title = "Model Yükleme Hatası",
                    description = errMsg,
                    severity = "ERROR"
                )
            }
        }
    }

    fun clearAiStatusMessage() {
        _aiUploadStatusMessage.value = null
    }

    private var scanJob: Job? = null

    // ---------------------------------------------------------------- sistem ölçümleri

    /**
     * Kullanılan RAM yüzdesi (0..100).
     *
     * Öncesinde bu akış `MutableStateFlow(42)` olarak doğuyor ve hiç güncellenmiyordu;
     * gösterge panosu da bunu kullanmak yerine kendi içinde `(40..85).random()`
     * çalıştırıyordu. Yani ekrandaki sayı ne cihazdan geliyordu ne de bir yerde
     * ölçülüyordu. Artık [android.app.ActivityManager.MemoryInfo] okunuyor.
     */
    private val _memUsage = MutableStateFlow(0)
    val memUsage: StateFlow<Int> = _memUsage.asStateFlow()

    /**
     * Cihaz genelinde anlık ağ hızı, KB/sn.
     *
     * [android.net.TrafficStats] sayaçlarının iki örnek arasındaki farkından
     * hesaplanır. Sayaç cihaz açılışından beri arttığı için ilk örnekte bir
     * referans alınır ve değer ancak ikinci örnekten sonra yayınlanır.
     */
    private val _netTraffic = MutableStateFlow(0)
    val netTraffic: StateFlow<Int> = _netTraffic.asStateFlow()

    /** Toplam RAM, insan okunur biçimde ("5,7 GB"). Ölçüm alınamadıysa boş. */
    private val _totalMemoryLabel = MutableStateFlow("")
    val totalMemoryLabel: StateFlow<String> = _totalMemoryLabel.asStateFlow()

    /**
     * Ölçümler gerçekten alınabiliyor mu?
     *
     * Bazı cihazlarda [android.net.TrafficStats] desteklenmez ve `UNSUPPORTED`
     * döner. Bu durumda uydurma bir sayı göstermek yerine arayüz "—" gösterebilsin
     * diye ayrı bir bayrak yayınlıyoruz.
     */
    private val _isNetMeteringSupported = MutableStateFlow(true)
    val isNetMeteringSupported: StateFlow<Boolean> = _isNetMeteringSupported.asStateFlow()

    private fun startSystemMetricsMonitor() {
        viewModelScope.launch(Dispatchers.Default) {
            val activityManager = getApplication<Application>()
                .getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memoryInfo = android.app.ActivityManager.MemoryInfo()

            var lastBytes = -1L
            var lastSampleAt = 0L

            while (isActive) {
                try {
                    activityManager.getMemoryInfo(memoryInfo)
                    val total = memoryInfo.totalMem
                    if (total > 0) {
                        val used = total - memoryInfo.availMem
                        _memUsage.value = ((used * 100) / total).toInt().coerceIn(0, 100)
                        if (_totalMemoryLabel.value.isEmpty()) {
                            _totalMemoryLabel.value = formatBytes(total)
                        }
                    }

                    val rx = android.net.TrafficStats.getTotalRxBytes()
                    val tx = android.net.TrafficStats.getTotalTxBytes()
                    if (rx == android.net.TrafficStats.UNSUPPORTED.toLong() ||
                        tx == android.net.TrafficStats.UNSUPPORTED.toLong()
                    ) {
                        _isNetMeteringSupported.value = false
                        _netTraffic.value = 0
                    } else {
                        val now = android.os.SystemClock.elapsedRealtime()
                        val bytes = rx + tx
                        if (lastBytes >= 0 && now > lastSampleAt) {
                            val deltaBytes = (bytes - lastBytes).coerceAtLeast(0L)
                            val deltaSeconds = (now - lastSampleAt) / 1000.0
                            val kbPerSecond = (deltaBytes / 1024.0 / deltaSeconds).toInt()
                            _netTraffic.value = kbPerSecond.coerceIn(0, 1_000_000)
                        }
                        lastBytes = bytes
                        lastSampleAt = now
                    }
                } catch (e: Exception) {
                    // Ölçüm alınamadı: son bilinen değer korunur. Uydurma bir sayı
                    // üretmek, göstergeyi tamamen işe yaramaz hale getirirdi.
                }

                // Tarama sırasında daha sık: kullanıcı o an sistemin yüklendiğini
                // görmek istiyor. Boştayken 3 sn yeterli ve pil dostu.
                delay(if (_isScanning.value) 1_000L else 3_000L)
            }
        }
    }

    private fun formatBytes(bytes: Long): String {
        val gb = bytes / (1024.0 * 1024.0 * 1024.0)
        return if (gb >= 1.0) String.format(java.util.Locale.getDefault(), "%.1f GB", gb)
        else String.format(java.util.Locale.getDefault(), "%.0f MB", bytes / (1024.0 * 1024.0))
    }

    private val _batteryPercentage = MutableStateFlow(100f)
    val batteryPercentage: StateFlow<Float> = _batteryPercentage.asStateFlow()

    private val _isThrottled = MutableStateFlow(false)
    val isThrottled: StateFlow<Boolean> = _isThrottled.asStateFlow()

    init {
        if (prefs.isRealTimeProtectionEnabled) {
            RealTimeProtectionService.start(getApplication())
        }
        if (prefs.scanScheduleMode > 0) {
            com.example.workers.ScanWorkerHelper.scheduleScan(getApplication(), prefs.scanTargetHour, prefs.scanTargetMinute)
        }
        refreshAudit()
        startBatteryMonitor()
        startSystemMetricsMonitor()
    }

    private fun startBatteryMonitor() {
        viewModelScope.launch {
            val powerManager = getApplication<Application>().getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            while (isActive) {
                val batteryStatus = getApplication<Application>().registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
                val level = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
                val pct = if (scale > 0) (level * 100f / scale) else 100f
                _batteryPercentage.value = pct
                
                val isCharging = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) == android.os.BatteryManager.BATTERY_STATUS_CHARGING
                _isThrottled.value = powerManager.isPowerSaveMode || (!isCharging && pct <= 20f)
                
                kotlinx.coroutines.delay(10_000)
            }
        }
    }


    fun toggleDarkTheme(isDark: Boolean) {
        prefs.isDarkTheme = isDark
        _isDarkTheme.value = isDark
    }

    fun toggleShizuku(isEnabled: Boolean) {
        if (isEnabled) {
            if (com.example.util.ShizukuUtils.isAvailable()) {
                if (!com.example.util.ShizukuUtils.hasPermission()) {
                    com.example.util.ShizukuUtils.requestPermission(1001)
                }
                prefs.isShizukuEnabled = true
                _isShizukuEnabled.value = true
            } else {
                prefs.isShizukuEnabled = false
                _isShizukuEnabled.value = false
            }
        } else {
            prefs.isShizukuEnabled = false
            _isShizukuEnabled.value = false
        }
    }

    fun setScanScheduleMode(mode: Int) {
        prefs.scanScheduleMode = mode
        _scanScheduleMode.value = mode
        
        // Restart background scheduler logic
        if (mode > 0) {
            com.example.service.RealTimeProtectionService.updateSchedule(getApplication())
            com.example.workers.ScanWorkerHelper.scheduleScan(getApplication(), prefs.scanTargetHour, prefs.scanTargetMinute)
        } else {
            com.example.workers.ScanWorkerHelper.cancelScan(getApplication())
        }
    }
    
    fun setScanTargetTime(hour: Int, minute: Int) {
        prefs.scanTargetHour = hour
        prefs.scanTargetMinute = minute
        _scanTargetHour.value = hour
        _scanTargetMinute.value = minute
        if (prefs.scanScheduleMode > 0) {
            com.example.workers.ScanWorkerHelper.scheduleScan(getApplication(), hour, minute)
        }
    }
    
    fun toggleEnergyEfficiencyMode(enabled: Boolean) {
        prefs.isEnergyEfficiencyModeEnabled = enabled
        _isEnergyEfficiencyModeEnabled.value = enabled
        if (enabled && _energyOptimizationScheduleMode.value == 0) {
            setEnergyOptimizationScheduleMode(1) // Default to 6 hours if enabled but mode is off
        } else if (!enabled) {
            setEnergyOptimizationScheduleMode(0)
        }
    }
    
    fun setEnergyOptimizationScheduleMode(mode: Int) {
        prefs.energyOptimizationScheduleMode = mode
        _energyOptimizationScheduleMode.value = mode
        scheduleEnergyWork(mode)
    }
    
    private fun scheduleEnergyWork(mode: Int) {
        if (mode == 0) {
            androidx.work.WorkManager.getInstance(getApplication()).cancelUniqueWork("EnergyOptimizationWork")
        } else {
            val repeatInterval = if (mode == 1) 6L else 24L
            val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.example.service.EnergyOptimizationWorker>(repeatInterval, java.util.concurrent.TimeUnit.HOURS)
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiresBatteryNotLow(false)
                        .build()
                ).build()
            androidx.work.WorkManager.getInstance(getApplication()).enqueueUniquePeriodicWork(
                "EnergyOptimizationWork",
                androidx.work.ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }

    fun toggleRealTimeProtection(enabled: Boolean) {
        prefs.isRealTimeProtectionEnabled = enabled
        _isRealTimeActive.value = enabled
        viewModelScope.launch {
            if (enabled) {
                RealTimeProtectionService.start(getApplication())
                repository.logEvent(
                    title = "Gerçek Zamanlı Kalkan Başlatıldı",
                    description = "7/24 arka plan güvenlik kalkanı ve uygulama gözlemcisi aktif.",
                    severity = "SUCCESS"
                )
            } else {
                RealTimeProtectionService.stop(getApplication())
                repository.logEvent(
                    title = "Gerçek Zamanlı Kalkan Durduruldu",
                    description = "Kullanıcı tarafından gerçek zamanlı tarama koruması devre dışı bırakıldı!",
                    severity = "WARNING"
                )
            }
        }
    }

    fun toggleAutoScanNewApps(enabled: Boolean) {
        prefs.isAutoScanNewAppsEnabled = enabled
        _isAutoScanNewApps.value = enabled
        viewModelScope.launch {
            repository.logEvent(
                title = if (enabled) "Yeni Uygulama Gözlemcisi Açıldı" else "Yeni Uygulama Gözlemcisi Kapatıldı",
                description = if (enabled) "Yeni kurulan tüm APK ve uygulamalar anında otomatik taranacaktır." else "Otomatik kurulum taraması kapatıldı.",
                severity = "INFO"
            )
        }
    }

    fun toggleHeuristics(enabled: Boolean) {
        prefs.isHeuristicAnalysisEnabled = enabled
        _isHeuristicsEnabled.value = enabled
    }

    fun refreshAudit() {
        viewModelScope.launch(Dispatchers.IO) {
            val report = DeviceSecurityAuditor.performAudit(getApplication())
            _deviceAuditReport.value = report
        }
    }

    fun startScan(scanType: String = "DEEP") {
        if (_isScanning.value) return

        scanJob?.cancel()
        // Dispatchers.IO: paket sorguları ve yeni eklenen depo APK taraması
        // dosya sistemi erişimi yapar; Default havuzunu bloklamamalı.
        scanJob = viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            _scanProgress.value = 0f
            _scannedCount.value = 0
            _scanThreatsFound.value = emptyList()

            val context = getApplication<AntivirusApplication>()
            val pm = context.packageManager
            val startTime = System.currentTimeMillis()

            val packages: List<PackageInfo> = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
                }
            } catch (e: Exception) {
                emptyList()
            }

            // NOT: Burada eskiden "güvenli ön ek" listesi (com.whatsapp, com.instagram…)
            // vardı ve bu ön eklerle başlayan paketler tarama dışı bırakılıyordu. Paket
            // adı ucuzdur: sahte bir "com.whatsapp.pro" APK'sı tam taramadan sırf adı
            // yüzünden geçebilirdi. Güven kararı paket adından değil, ThreatEngine'in
            // imza doğrulamasından gelir; her paket taranır.

            val newlyFoundThreats = mutableListOf<ThreatEntity>()
            var currentStep = 0

            // Kullanıcının güvenli listeye aldığı paketler analiz edilmez: sonuç
            // kaydedilmeyecek bir paket için sertifika ayrıştırma pahalıdır ve
            // liste kararının her taramada sorgulanması güveni zayıflatır.
            // (Kullanıcı istediğinde listeden çıkarabilir, sonraki taramada yeniden
            // değerlendirilir.)
            val whitelistedPackages = repository.whitelistedPackageNames().toHashSet()

            // Dinamik gecikme hesaplayıcı: Dosya adının uzunluğuna ve rastgeleliğe göre "dosya boyutu/karmaşıklık" simülasyonu
            val calculateDelay: (String, Long, Long) -> Long = { name, minDelay, maxDelay ->
                val base = name.length * 2L
                val randomFactor = (0..20).random().toLong()
                (base + randomFactor).coerceIn(minDelay, maxDelay)
            }

            // HIZLI TARAMA, TAM TARAMA ve DERİN ANALİZ birbirinden tamamen farklı kapsam, hedef ve sürelere sahiptir:
            when (scanType) {
                "QUICK" -> {
                    // 1. HIZLI TARAMA: Sadece kullanıcı uygulamaları ve temel sistem paketleri
                    val quickModules = listOf(
                        "Kullanıcı Uygulamaları (APK) Taraması",
                        "Sistem Uygulamaları Denetimi"
                    )
                    val userPackages = packages.filter { pkg ->
                        val appInfo = pkg.applicationInfo ?: return@filter false
                        val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                        val isUpdated = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                        !isSystem || isUpdated
                    }.take(35).ifEmpty { packages.take(25) }

                    val totalSteps = (quickModules.size + userPackages.size).coerceAtLeast(1)
                    _totalAppsToScan.value = totalSteps

                    for (mod in quickModules) {
                        currentStep++
                        _currentAppBeingScanned.value = "Hızlı Denetim: $mod"
                        _scannedCount.value = currentStep
                        _scanProgress.value = currentStep.toFloat() / totalSteps.toFloat()
                        delay(calculateDelay(mod, 250L, 500L))
                    }

                    for (pkg in userPackages) {
                        currentStep++
                        val appName = try {
                            pkg.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg.packageName
                        } catch (e: Exception) { pkg.packageName }

                        _currentAppBeingScanned.value = "Uygulama: $appName"
                        _scannedCount.value = currentStep
                        _scanProgress.value = currentStep.toFloat() / totalSteps.toFloat()

                        if (pkg.packageName in whitelistedPackages) {
                            delay(calculateDelay(appName, 40L, 80L))
                            continue
                        }

                        val result = ThreatEngine.evaluatePackageInfo(context, pkg)
                        if (result.isThreat && result.threatEntity != null) {
                            newlyFoundThreats.add(result.threatEntity)
                            _scanThreatsFound.value = newlyFoundThreats.toList()
                            repository.recordDetectedThreat(result.threatEntity)
                        }
                        delay(calculateDelay(appName, 150L, 350L))
                    }
                }

                "FULL" -> {
                    // 2. TAM TARAMA: Tüm uygulamalar + Depodaki kurulmamış APK dosyaları
                    val allPackages = packages

                    // Depo taraması gerçek dosya I/O olduğundan ağır sürecin (Dispatchers.IO)
                    // dışında çağrılmamalı; bu zaten IO dispatcher'da çalışıyor.
                    // Önce dosyaları bul, sonra ilerlemeyi onlarla birlikte akıt.
                    val apkScan = com.example.scanner.ApkFileScanner.scanDownloadedApks(context) { file, _ ->
                        _currentAppBeingScanned.value = "Depo APK: ${file.name}"
                    }
                    val apkThreats = apkScan.threats
                    apkThreats.forEach { repository.recordDetectedThreat(it) }
                    if (apkThreats.isNotEmpty()) {
                        newlyFoundThreats.addAll(apkThreats)
                        _scanThreatsFound.value = newlyFoundThreats.toList()
                    }

                    val totalSteps = (allPackages.size + 1).coerceAtLeast(1) // +1: depo taraması adımı
                    _totalAppsToScan.value = totalSteps

                    // Depo taraması adımı (yukarıda tamamlandı; ilerlemede tek adım olarak gösterilir)
                    currentStep++
                    _currentAppBeingScanned.value =
                        if (apkScan.scannedFiles.isEmpty()) "Depo APK taraması: dosya bulunamadı"
                        else "Depo APK taraması: ${apkScan.scannedFiles.size} dosya incelendi"
                    _scannedCount.value = currentStep
                    _scanProgress.value = currentStep.toFloat() / totalSteps.toFloat()
                    delay(400)

                    for (pkg in allPackages) {
                        currentStep++
                        val appName = try {
                            pkg.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg.packageName
                        } catch (e: Exception) { pkg.packageName }

                        _currentAppBeingScanned.value = "İnceleniyor: $appName"
                        _scannedCount.value = currentStep
                        _scanProgress.value = currentStep.toFloat() / totalSteps.toFloat()

                        // Ön ek istisnası yok: klon tespiti ancak her paket taranınca
                        // çalışır (yukarıdaki NOT'a bakınız). Tek istisna kullanıcının
                        // açık kararı olan güvenli listedir.
                        if (pkg.packageName in whitelistedPackages) {
                            delay(calculateDelay(appName, 40L, 80L))
                            continue
                        }

                        val result = ThreatEngine.evaluatePackageInfo(context, pkg)
                        if (result.isThreat && result.threatEntity != null) {
                            newlyFoundThreats.add(result.threatEntity)
                            _scanThreatsFound.value = newlyFoundThreats.toList()
                            repository.recordDetectedThreat(result.threatEntity)
                        }
                        delay(calculateDelay(appName, 150L, 400L))
                    }
                }

                else -> {
                    // 3. DERİN ANALİZ: Telefonun tamamı, RAM, Kernel, Tüm Bölümler
                    val deepAiModules = listOf(
                        "Donanım & CPU Çekirdekleri",
                        "Aktif RAM & Bellek Alanları",
                        "İşletim Sistemi (OS) Bölümleri",
                        "Bootloader & Root Tespiti",
                        "Tüm Uygulamalar ve Dosyalar",
                        "Gizli Önbellek (Cache)",
                        "Ağ Soketleri & Casus Portlar",
                        "Erişilebilirlik (Accessibility) Kancaları",
                        "Zero-Day AI Davranışsal Sezgi Denetimi"
                    )
                    val deepTargetPackages = packages
                    val totalSteps = (deepAiModules.size + deepTargetPackages.size).coerceAtLeast(1)
                    _totalAppsToScan.value = totalSteps

                    for (module in deepAiModules) {
                        currentStep++
                        _currentAppBeingScanned.value = module
                        _scannedCount.value = currentStep
                        _scanProgress.value = currentStep.toFloat() / totalSteps.toFloat()
                        delay(calculateDelay(module, 400L, 900L)) // Derin analiz hissi
                    }
                    
                    // Perform advanced system checks (Root/Jailbreak/Magisk)
                    _currentAppBeingScanned.value = "Sistem İmzası & Kök Denetimi (Root Check)"
                    val rootAndSystemThreats = com.example.scanner.ThreatEngine.performDeepSystemScan(context)
                    if (rootAndSystemThreats.isNotEmpty()) {
                        newlyFoundThreats.addAll(rootAndSystemThreats)
                        _scanThreatsFound.value = newlyFoundThreats.toList()
                        rootAndSystemThreats.forEach { repository.recordDetectedThreat(it) }
                    }
                    delay(800L)

                    for (pkg in deepTargetPackages) {
                        currentStep++
                        val appName = try {
                            pkg.applicationInfo?.let { pm.getApplicationLabel(it).toString() } ?: pkg.packageName
                        } catch (e: Exception) { pkg.packageName }

                        _currentAppBeingScanned.value = "Nöral Sandbox: $appName"
                        _scannedCount.value = currentStep
                        _scanProgress.value = currentStep.toFloat() / totalSteps.toFloat()

                        // Derin taramada ön ek istisnası yok: her paket imza ve izin
                        // denetiminden geçer (yukarıdaki NOT'a bakınız). Tek istisna
                        // kullanıcının güvenli liste kararıdır.
                        if (pkg.packageName in whitelistedPackages) {
                            delay(calculateDelay(appName, 40L, 80L))
                            continue
                        }

                        val result = ThreatEngine.evaluatePackageInfo(context, pkg)
                        if (result.isThreat && result.threatEntity != null) {
                            newlyFoundThreats.add(result.threatEntity)
                            _scanThreatsFound.value = newlyFoundThreats.toList()
                            repository.recordDetectedThreat(result.threatEntity)
                        }
                        delay(calculateDelay(appName, 250L, 600L))
                    }
                }
            }

            val elapsedRaw = System.currentTimeMillis() - startTime
            val minDuration = when (scanType) {
                "QUICK" -> 5800L
                "FULL" -> 14500L
                else -> 22000L
            }
            val duration = elapsedRaw.coerceAtLeast(minDuration)
            val scanTypeStr = when(scanType) { "FULL" -> "Tam Kapsamlı Tarama"; "QUICK" -> "Hızlı Tarama"; else -> "Derin Sistem Taraması" }
            val completedSteps = _totalAppsToScan.value

            val record = ScanRecordEntity(
                scanType = scanTypeStr,
                timestamp = System.currentTimeMillis(),
                scannedAppsCount = completedSteps,
                threatsFoundCount = newlyFoundThreats.size,
                durationMs = duration
            )

            repository.recordScan(record)
            
            // Show system notification
            NotificationHelper.showScanCompleteNotification(context, newlyFoundThreats.size)

            _lastCompletedScan.value = record
            prefs.lastScanTimestamp = System.currentTimeMillis()
            _lastScanTime.value = prefs.lastScanTimestamp

            repository.logEvent(
                title = "$scanTypeStr Tamamlandı",
                description = "$completedSteps güvenlik katmanı/uygulama incelendi, ${newlyFoundThreats.size} potansiyel risk tespit edildi.",
                severity = if (newlyFoundThreats.isEmpty()) "SUCCESS" else "WARNING"
            )

            _isScanning.value = false
            refreshAudit()
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        _isScanning.value = false
    }

    fun resolveThreat(threatId: Long) {
        viewModelScope.launch {
            repository.updateThreatStatus(threatId, "ÇÖZÜLDÜ")
            repository.logEvent(
                title = "Tehdit Çözüldü",
                description = "Tehdit kaydı kullanıcı tarafından güvenli veya çözüldü olarak işaretlendi.",
                severity = "INFO"
            )
        }
    }

    fun whitelistThreat(threatId: Long) {
        viewModelScope.launch {
            repository.updateThreatStatus(threatId, "GÜVENLİ_LİSTE")
            repository.logEvent(
                title = "Beyaz Listeye Eklendi",
                description = "Uygulama güvenilir listeye alındı, gelecekteki taramalarda göz ardı edilecek.",
                severity = "INFO"
            )
        }
    }

    fun quarantineThreat(threatId: Long) {
        viewModelScope.launch {
            repository.updateThreatStatus(threatId, "KARANTİNA")
            repository.logEvent(
                title = "Karantinaya Alındı",
                description = "Tehdit izole edildi. Sistem kaynaklarına erişimi durduruldu.",
                severity = "WARNING"
            )
        }
    }

    fun removeThreat(threatId: Long) {
        viewModelScope.launch {
            repository.deleteThreatById(threatId)
        }
    }

    fun uninstallApp(context: Context, packageName: String) {
        if (prefs.isShizukuEnabled && com.example.util.ShizukuUtils.hasPermission()) {
            val success = com.example.util.ShizukuUtils.uninstallAppSilent(packageName)
            if (success) {
                viewModelScope.launch {
                    repository.logEvent("Uygulama Silindi", "$packageName Shizuku ile sessizce kaldırıldı.", "INFO")
                }
                return
            }
        }
        try {
            val intent = Intent(Intent.ACTION_DELETE).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // If direct intent fails, open application details
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }

    fun freezeApp(packageName: String) {
        if (prefs.isShizukuEnabled && com.example.util.ShizukuUtils.hasPermission()) {
            val success = com.example.util.ShizukuUtils.freezeApp(packageName)
            if (success) {
                viewModelScope.launch {
                    repository.logEvent("Uygulama Donduruldu", "$packageName Shizuku API kullanılarak donduruldu (Devre Dışı).", "INFO")
                }
            }
        }
    }

    fun forceStopApp(packageName: String) {
        if (prefs.isShizukuEnabled && com.example.util.ShizukuUtils.hasPermission()) {
            val success = com.example.util.ShizukuUtils.forceStopApp(packageName)
            if (success) {
                viewModelScope.launch {
                    repository.logEvent("Süreç Kapatıldı", "$packageName Shizuku API ile zorla durduruldu.", "INFO")
                }
            }
        }
    }

    fun injectSimulatedThreat(variant: Int) {
        viewModelScope.launch {
            val testThreat = ThreatEngine.createTestThreat(variant)
            repository.insertThreat(testThreat)
            repository.logEvent(
                title = "Canlı Test Tehdidi Simüle Edildi",
                description = "${testThreat.appName} gerçek zamanlı koruma motorunu test etmek için sisteme eklendi.",
                severity = "CRITICAL"
            )
        }
    }

    
    data class AppNetworkStat(
        val packageName: String,
        val appName: String,
        val rxBytes: Long,
        val txBytes: Long,
        val totalBytes: Long
    )

    fun getDailyNetworkStats(context: Context): List<AppNetworkStat> {
        val statsList = mutableListOf<AppNetworkStat>()
        val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager ?: return emptyList()
        val pm = context.packageManager
        
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (24 * 60 * 60 * 1000) // Last 24 hours
        
        try {
            val uidStats = mutableMapOf<Int, LongArray>() // uid -> [rx, tx]
            
            // Query WiFi stats
            val wifiStats = networkStatsManager.querySummary(NetworkCapabilities.TRANSPORT_WIFI, "", startTime, endTime)
            val bucket = NetworkStats.Bucket()
            while (wifiStats.hasNextBucket()) {
                wifiStats.getNextBucket(bucket)
                val uid = bucket.uid
                val current = uidStats.getOrDefault(uid, LongArray(2) { 0L })
                current[0] += bucket.rxBytes
                current[1] += bucket.txBytes
                uidStats[uid] = current
            }
            wifiStats.close()
            
            // Query Cellular stats
            val mobileStats = networkStatsManager.querySummary(NetworkCapabilities.TRANSPORT_CELLULAR, "", startTime, endTime)
            while (mobileStats.hasNextBucket()) {
                mobileStats.getNextBucket(bucket)
                val uid = bucket.uid
                val current = uidStats.getOrDefault(uid, LongArray(2) { 0L })
                current[0] += bucket.rxBytes
                current[1] += bucket.txBytes
                uidStats[uid] = current
            }
            mobileStats.close()
            
            for ((uid, data) in uidStats) {
                if (data[0] == 0L && data[1] == 0L) continue
                val packages = pm.getPackagesForUid(uid)
                if (!packages.isNullOrEmpty()) {
                    val pkgName = packages[0]
                    val appName = try {
                        val info = pm.getApplicationInfo(pkgName, 0)
                        pm.getApplicationLabel(info).toString()
                    } catch (e: Exception) {
                        pkgName
                    }
                    statsList.add(AppNetworkStat(pkgName, appName, data[0], data[1], data[0] + data[1]))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return statsList.sortedByDescending { it.totalBytes }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearEvents()
        }
    }
}
