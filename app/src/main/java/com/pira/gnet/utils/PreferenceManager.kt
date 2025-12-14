package com.pira.gnet.utils

import android.content.Context
import android.content.SharedPreferences
import com.pira.gnet.data.ProxyConfig
import com.pira.gnet.data.ProxyType

class PreferenceManager private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val SELECTED_PROXY_TYPE = "selected_proxy_type"
        private const val SELECTED_PORT = "selected_port"
        private const val SELECTED_IP_ADDRESS = "selected_ip_address"
        private const val DEFAULT_PROXY_TYPE = "HTTP"
        private const val DEFAULT_PORT = 8080
        private const val DEFAULT_IP = ""
        
        @Volatile
        private var INSTANCE: PreferenceManager? = null
        
        fun getInstance(context: Context): PreferenceManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferenceManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    fun saveProxySettings(proxyConfig: ProxyConfig) {
        prefs.edit()
            .putString(SELECTED_PROXY_TYPE, proxyConfig.proxyType.name)
            .putInt(SELECTED_PORT, proxyConfig.port)
            .apply()
    }
    
    fun loadProxySettings(): ProxyConfig {
        val proxyTypeStr = prefs.getString(SELECTED_PROXY_TYPE, DEFAULT_PROXY_TYPE) ?: DEFAULT_PROXY_TYPE
        val port = prefs.getInt(SELECTED_PORT, DEFAULT_PORT)
        
        val proxyType = try {
            ProxyType.valueOf(proxyTypeStr)
        } catch (e: IllegalArgumentException) {
            ProxyType.HTTP
        }
        
        return ProxyConfig(
            proxyType = proxyType,
            port = port,
            isActive = false
        )
    }
    
    fun saveSelectedIpAddress(ipAddress: String) {
        prefs.edit()
            .putString(SELECTED_IP_ADDRESS, ipAddress)
            .apply()
    }
    
    fun getSelectedIpAddress(): String {
        return prefs.getString(SELECTED_IP_ADDRESS, DEFAULT_IP) ?: DEFAULT_IP
    }
    
    fun clearSelectedIpAddress() {
        prefs.edit()
            .remove(SELECTED_IP_ADDRESS)
            .apply()
    }
}