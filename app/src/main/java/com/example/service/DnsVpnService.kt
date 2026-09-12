package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.util.ProtectionPreferences
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlinx.coroutines.flow.MutableStateFlow
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DnsVpnService : VpnService(), Runnable {
    companion object {
        private const val KEYWORD_REFRESH_MS = 60_000L
        private const val BLOCK_LOG_COOLDOWN_MS = 5 * 60_000L

        private val _isVpnActive = MutableStateFlow(false)
        val isVpnActive = _isVpnActive.asStateFlow()
        
        private var isMonitoring = false

        fun startMonitoring(context: Context) {
            if (isMonitoring) return
            isMonitoring = true
            
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
                .removeCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                .build()

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isVpnActive.value = true
                }

                override fun onLost(network: Network) {
                    _isVpnActive.value = false
                }
            }

            try {
                connectivityManager.registerNetworkCallback(request, callback)
                
                // Check current status immediately
                val activeNetwork = connectivityManager.activeNetwork
                val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
                if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) {
                    _isVpnActive.value = true
                }
            } catch (e: Exception) {
                // Ignore security exceptions if permissions are weird on older Androids
            }
        }
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null
    @Volatile private var isRunning = false
    private var targetDnsPrimary = "1.1.1.1"
    private var targetDnsSecondary = "1.0.0.1"

    /** Her DNS paketinde SharedPreferences okumamak için anahtar kelimeler önbelleklenir. */
    @Volatile private var cachedKeywords: List<String> = com.example.util.DnsFilter.DEFAULT_BLOCKED_KEYWORDS
    @Volatile private var keywordsLoadedAt = 0L

    /** Aynı alan adı için olay kaydını dakikada bir kereye indirir. */
    private val blockLogTimestamps = HashMap<String, Long>()

    /**
     * Engel listesini tazeler.
     *
     * Kaynak [com.example.data.intel.ThreatIntelStore]: gömülü küme çevrimdışı
     * çalışır, Wi-Fi'da indirilen güncelleme onu genişletir. Kullanıcının elle
     * eklediği kelimeler (`dynamicBlockedKeywords`) her zaman üstüne eklenir.
     */
    private fun currentBlockedKeywords(): List<String> {
        val now = System.currentTimeMillis()
        if (now - keywordsLoadedAt > KEYWORD_REFRESH_MS) {
            keywordsLoadedAt = now
            val fromIntel = try {
                com.example.data.intel.ThreatIntelStore.current(applicationContext).blockedDomainKeywords
            } catch (e: Exception) {
                emptySet<String>()
            }
            val userDefined = try {
                ProtectionPreferences(applicationContext).dynamicBlockedKeywords
            } catch (e: Exception) {
                emptySet<String>()
            }
            cachedKeywords = (com.example.util.DnsFilter.DEFAULT_BLOCKED_KEYWORDS + fromIntel + userDefined).distinct()
        }
        return cachedKeywords
    }

    private fun logBlockedDomain(domain: String, keyword: String) {
        val now = System.currentTimeMillis()
        synchronized(blockLogTimestamps) {
            val last = blockLogTimestamps[domain]
            if (last != null && now - last < BLOCK_LOG_COOLDOWN_MS) return
            if (blockLogTimestamps.size > 256) blockLogTimestamps.clear()
            blockLogTimestamps[domain] = now
        }

        val repository = (applicationContext as com.example.AntivirusApplication).repository
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            repository.logEvent(
                title = "Ağ Kalkanı: Bağlantı Engellendi",
                description = "$domain adresine yapılan DNS sorgusu \"$keyword\" filtresiyle eşleşti ve engellendi.",
                severity = "WARNING"
            )
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == "STOP") {
            stopVpn()
            return START_NOT_STICKY
        }
        
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        val prefs = ProtectionPreferences(this)
        targetDnsPrimary = prefs.primaryDns
        targetDnsSecondary = prefs.secondaryDns
        val activeProfile = ProtectionPreferences.DNS_PROFILES.find { it.id == prefs.activeDnsProfileId }
        val profileName = activeProfile?.name ?: "Güvenli DNS"
        
        val channelId = "vpn_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "VPN Bağlantısı",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("$profileName Aktif")
            .setContentText("DNS istekleri $targetDnsPrimary üzerinden filtreleniyor")
            .setSmallIcon(android.R.drawable.ic_secure)
            .build()
            
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(2, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(2, notification)
        }

        try {
            val builder = Builder()
                .setSession("AAE Güvenli DNS - $profileName")
                .addAddress("10.233.0.2", 30)
                .addDnsServer(targetDnsPrimary)
                .addDnsServer(targetDnsSecondary)
                // Critical Architecture: Route ONLY the DNS server IPs into the TUN interface!
                // This ensures YouTube, games, Chrome and web streaming NEVER hit a dead tunnel or drop packets!
                .addRoute(targetDnsPrimary, 32)
                .addRoute(targetDnsSecondary, 32)
                .setMtu(1500)
                .allowBypass()

            // Disallow our own package so our socket works without loops
            try {
                builder.addDisallowedApplication(packageName)
            } catch (ignored: Exception) {}

            vpnInterface = builder.establish()
            if (vpnInterface != null) {
                isRunning = true
                _isVpnActive.value = true
                vpnThread = Thread(this, "AaeDnsVpnThread").apply { start() }
                Log.i("DnsVpnService", "VPN Started for DNS: $targetDnsPrimary / $targetDnsSecondary")
            } else {
                Log.e("DnsVpnService", "VPN establish returned null")
                stopSelf()
            }
        } catch (e: Exception) {
            Log.e("DnsVpnService", "Error starting VPN", e)
            stopSelf()
        }
    }

    override fun run() {
        val pfd = vpnInterface ?: return
        val inputStream = FileInputStream(pfd.fileDescriptor)
        val outputStream = FileOutputStream(pfd.fileDescriptor)
        val packet = ByteArray(32767)

        var dnsSocket: DatagramSocket? = null
        try {
            dnsSocket = DatagramSocket()
            protect(dnsSocket)
            dnsSocket.soTimeout = 2500

            val primaryAddr = InetAddress.getByName(targetDnsPrimary)
            val primaryIpBytes = primaryAddr.address

            while (isRunning && !Thread.currentThread().isInterrupted) {
                val length = try {
                    inputStream.read(packet)
                } catch (e: Exception) {
                    break
                }
                if (length <= 0) continue

                // Check IPv4 and UDP
                if ((packet[0].toInt() and 0xF0) != 0x40) continue
                val protocol = packet[9].toInt() and 0xFF
                if (protocol != 17) continue // 17 is UDP

                val ipHeaderLen = (packet[0].toInt() and 0x0F) * 4
                if (length < ipHeaderLen + 8) continue

                val dstPort = ((packet[ipHeaderLen + 2].toInt() and 0xFF) shl 8) or (packet[ipHeaderLen + 3].toInt() and 0xFF)
                if (dstPort != 53) continue

                val srcPort = ((packet[ipHeaderLen].toInt() and 0xFF) shl 8) or (packet[ipHeaderLen + 1].toInt() and 0xFF)
                val udpLen = ((packet[ipHeaderLen + 4].toInt() and 0xFF) shl 8) or (packet[ipHeaderLen + 5].toInt() and 0xFF)
                val dnsDataLen = udpLen - 8
                if (dnsDataLen <= 0 || ipHeaderLen + 8 + dnsDataLen > length) continue

                val clientIpBytes = ByteArray(4)
                System.arraycopy(packet, 12, clientIpBytes, 0, 4)

                val dnsQuery = ByteArray(dnsDataLen)
                System.arraycopy(packet, ipHeaderLen + 8, dnsQuery, 0, dnsDataLen)
                
                // Oltalama ve Zararlı Bağlantı Filtresi (Anti-Phishing / AdBlock)
                // Ham ASCII üzerinde contains() yerine sorguyu gerçekten çözümleyip
                // alan adı etiketi sınırlarına göre eşleştiriyoruz.
                val domain = com.example.util.DnsFilter.extractQueryName(dnsQuery, dnsDataLen)
                val matchedKeyword = if (domain != null) {
                    com.example.util.DnsFilter.findBlockedKeyword(domain, currentBlockedKeywords())
                } else {
                    null
                }

                try {
                    if (matchedKeyword != null && domain != null) {
                        logBlockedDomain(domain, matchedKeyword)

                        // Paketi sessizce düşürmek istemciyi DNS zaman aşımına kadar
                        // bekletiyordu; NXDOMAIN anında "yok" cevabı verir.
                        val nxDomain = com.example.util.DnsFilter.buildNxDomainResponse(dnsQuery, dnsDataLen)
                        if (nxDomain != null) {
                            val respPacket = buildResponsePacket(
                                clientIp = clientIpBytes,
                                dnsServerIp = primaryIpBytes,
                                clientPort = srcPort,
                                dnsData = nxDomain,
                                dnsDataLen = nxDomain.size
                            )
                            outputStream.write(respPacket)
                        }
                        continue
                    }

                    val outPacket = DatagramPacket(dnsQuery, dnsDataLen, primaryAddr, 53)
                    dnsSocket.send(outPacket)

                    val inBuffer = ByteArray(4096)
                    val inPacket = DatagramPacket(inBuffer, inBuffer.size)
                    dnsSocket.receive(inPacket)

                    val respPacket = buildResponsePacket(
                        clientIp = clientIpBytes,
                        dnsServerIp = primaryIpBytes,
                        clientPort = srcPort,
                        dnsData = inBuffer,
                        dnsDataLen = inPacket.length
                    )
                    outputStream.write(respPacket)
                } catch (e: Exception) {
                    // Socket timeout or packet drop, continue
                }
            }
        } catch (e: Exception) {
            Log.e("DnsVpnService", "Worker thread exception", e)
        } finally {
            try { dnsSocket?.close() } catch (ignored: Exception) {}
        }
    }

    private fun computeIpChecksum(header: ByteArray, length: Int): Int {
        var sum = 0
        var i = 0
        while (i < length) {
            if (i == 10) { i += 2; continue } // Skip checksum field itself
            val word = ((header[i].toInt() and 0xFF) shl 8) or (header[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        while ((sum shr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return (sum.inv()) and 0xFFFF
    }

    private fun buildResponsePacket(
        clientIp: ByteArray,
        dnsServerIp: ByteArray,
        clientPort: Int,
        dnsData: ByteArray,
        dnsDataLen: Int
    ): ByteArray {
        val totalLen = 20 + 8 + dnsDataLen
        val packet = ByteArray(totalLen)

        // IPv4 Header (20 bytes)
        packet[0] = 0x45.toByte() // Ver 4, IHL 5
        packet[1] = 0x00.toByte()
        packet[2] = ((totalLen shr 8) and 0xFF).toByte()
        packet[3] = (totalLen and 0xFF).toByte()
        packet[4] = 0x12.toByte()
        packet[5] = 0x34.toByte()
        packet[6] = 0x40.toByte() // Don't fragment
        packet[7] = 0x00.toByte()
        packet[8] = 64.toByte()   // TTL
        packet[9] = 17.toByte()   // UDP
        System.arraycopy(dnsServerIp, 0, packet, 12, 4)
        System.arraycopy(clientIp, 0, packet, 16, 4)

        val ipChecksum = computeIpChecksum(packet, 20)
        packet[10] = ((ipChecksum shr 8) and 0xFF).toByte()
        packet[11] = (ipChecksum and 0xFF).toByte()

        // UDP Header (8 bytes)
        packet[20] = 0x00.toByte() // Src port 53
        packet[21] = 0x35.toByte()
        packet[22] = ((clientPort shr 8) and 0xFF).toByte()
        packet[23] = (clientPort and 0xFF).toByte()
        val udpLen = 8 + dnsDataLen
        packet[24] = ((udpLen shr 8) and 0xFF).toByte()
        packet[25] = (udpLen and 0xFF).toByte()
        packet[26] = 0x00.toByte() // UDP checksum 0
        packet[27] = 0x00.toByte()

        // DNS Payload
        System.arraycopy(dnsData, 0, packet, 28, dnsDataLen)
        return packet
    }

    private fun stopVpn() {
        isRunning = false
        _isVpnActive.value = false
        vpnThread?.interrupt()
        vpnThread = null
        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e("DnsVpnService", "Error closing VPN", e)
        }
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVpn()
    }
}
