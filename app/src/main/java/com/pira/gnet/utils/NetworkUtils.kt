package com.pira.gnet.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import java.lang.reflect.Method
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {
    private const val TAG = "NetworkUtils"
    
    /**
     * Get all available IP addresses on the device
     */
    fun getAvailableIPs(context: Context): List<String> {
        val ips = mutableListOf<String>()
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                // Skip loopback and inactive interfaces
                if (intf.isLoopback || !intf.isUp) continue
                
                val addresses = Collections.list(intf.inetAddresses)
                for (addr in addresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        // Filter out IPv6 addresses and add IPv4 addresses
                        if (!ip.contains(":")) {
                            ips.add(ip)
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Logger.e(TAG, "Error getting available IPs", ex)
        }
        
        // Add hotspot IP as default if not already present
        val hotspotIp = getHotspotIpAddress()
        if (!ips.contains(hotspotIp)) {
            ips.add(0, hotspotIp)
        }
        
        Logger.d(TAG, "Available IPs: $ips")
        return ips
    }
    
    /**
     * Get the IP address of the device's WiFi hotspot
     */
    fun getHotspotIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.name.contains("wlan", ignoreCase = true) && 
                    !intf.name.contains("ap", ignoreCase = true)) continue
                
                val addresses = Collections.list(intf.inetAddresses)
                for (addr in addresses) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        // Hotspot IPs typically start with 192.168.x.x
                        if (ip.startsWith("192.168.")) {
                            Logger.d(TAG, "Found hotspot IP: $ip")
                            return ip
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Logger.e(TAG, "Error getting hotspot IP address", ex)
        }
        Logger.d(TAG, "Using default hotspot IP: 192.168.43.1")
        return "192.168.43.1" // Default fallback
    }
    
    /**
     * Check if the device is connected to a VPN
     */
    fun isVpnConnected(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                val isConnected = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
                Logger.d(TAG, "VPN connected (API 23+): $isConnected")
                isConnected
            } else {
                @Suppress("DEPRECATION")
                val activeNetworkInfo = connectivityManager.activeNetworkInfo
                val isConnected = activeNetworkInfo?.type == ConnectivityManager.TYPE_VPN
                Logger.d(TAG, "VPN connected (API < 23): $isConnected")
                isConnected
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error checking VPN connection", e)
            false
        }
    }
    
    /**
     * Check if WiFi hotspot is enabled using multiple approaches for better compatibility
     */
    fun isHotspotEnabled(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            
            // First try: Check for tethering interfaces
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val activeNetwork = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                
                // Check if we have a WiFi network that could be a hotspot
                if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) {
                    // Additional check for hotspot by looking at network interfaces
                    if (isHotspotInterfaceAvailable()) {
                        Logger.d(TAG, "Hotspot detected via network interfaces")
                        return true
                    }
                }
            }
            
            // Second try: Use reflection for older approach
            try {
                val method: Method = wifiManager.javaClass.getMethod("isWifiApEnabled")
                val isEnabled = method.invoke(wifiManager) as Boolean
                Logger.d(TAG, "Hotspot enabled (reflection): $isEnabled")
                return isEnabled
            } catch (e: Exception) {
                Logger.e(TAG, "Reflection method failed", e)
            }
            
            // Third try: Check system properties (requires root, but we can try)
            try {
                val systemProperties = Class.forName("android.os.SystemProperties")
                val method = systemProperties.getMethod("get", String::class.java)
                val tethering = method.invoke(null, "sys.tethering") as String
                if (tethering == "1") {
                    Logger.d(TAG, "Hotspot enabled (system property)")
                    return true
                }
            } catch (e: Exception) {
                Logger.e(TAG, "System property check failed", e)
            }
            
            // Fourth try: Check WiFi state and tethering state
            try {
                // Check if WiFi is enabled
                if (wifiManager.isWifiEnabled) {
                    // Check if tethering is active
                    val tetheredIfaces = getTetheredInterfaces(context)
                    if (tetheredIfaces.isNotEmpty()) {
                        Logger.d(TAG, "Hotspot enabled (tethering active)")
                        return true
                    }
                }
            } catch (e: Exception) {
                Logger.e(TAG, "WiFi/tethering check failed", e)
            }
            
            Logger.d(TAG, "Hotspot not detected by any method")
            false
        } catch (e: Exception) {
            Logger.e(TAG, "Error checking hotspot status", e)
            false
        }
    }
    
    /**
     * Check for hotspot interfaces directly
     */
    private fun isHotspotInterfaceAvailable(): Boolean {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                // Look for typical hotspot interface names
                if (intf.name.contains("ap", ignoreCase = true) || 
                    intf.name.contains("wlan", ignoreCase = true) ||
                    intf.name.contains("softap", ignoreCase = true)) {
                    // Check if interface has an IP in hotspot range
                    val addresses = Collections.list(intf.inetAddresses)
                    for (addr in addresses) {
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            val ip = addr.hostAddress ?: continue
                            if (ip.startsWith("192.168.")) {
                                Logger.d(TAG, "Hotspot interface found: ${intf.name} with IP $ip")
                                return true
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Logger.e(TAG, "Error checking hotspot interfaces", ex)
        }
        return false
    }
    
    /**
     * Get tethered interfaces
     */
    private fun getTetheredInterfaces(context: Context): Array<String> {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val method = connectivityManager.javaClass.getMethod("getTetheredIfaces")
            return method.invoke(connectivityManager) as Array<String>
        } catch (e: Exception) {
            Logger.e(TAG, "Error getting tethered interfaces", e)
            return emptyArray()
        }
    }
}