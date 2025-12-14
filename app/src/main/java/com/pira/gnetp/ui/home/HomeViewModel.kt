package com.pira.gnetp.ui.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pira.gnetp.data.ProxyConfig
import com.pira.gnetp.data.ProxyType
import com.pira.gnetp.proxy.ProxyServerService
import com.pira.gnetp.utils.Logger
import com.pira.gnetp.utils.NetworkUtils
import com.pira.gnetp.utils.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val proxyConfig: MutableStateFlow<ProxyConfig>
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    companion object {
        private const val TAG = "HomeViewModel"
        private const val STATUS_CHECK_INTERVAL = 1000L // 1 second for more responsive updates
    }

    init {
        Logger.d(TAG, "HomeViewModel initialized")
        
        // Load saved proxy settings
        loadSavedProxySettings()
        
        // Start continuous status monitoring
        startContinuousStatusMonitoring()
        
        // Initialize with available IPs
        getAvailableIPs()
    }

    fun startProxy() {
        Logger.d(TAG, "startProxy called")
        viewModelScope.launch {
            try {
                Logger.d(TAG, "Starting proxy service")
                startProxyService()
            } catch (e: Exception) {
                Logger.e(TAG, "Error starting proxy", e)
                _uiState.value = _uiState.value.copy(
                    isProxyActive = false,
                    errorMessage = "Failed to start proxy service: ${e.message}"
                )
            }
        }
    }

    fun stopProxy() {
        Logger.d(TAG, "stopProxy called")
        viewModelScope.launch {
            proxyConfig.value = proxyConfig.value.copy(isActive = false)
            val intent = Intent(context, ProxyServerService::class.java)
            context.stopService(intent)
            _uiState.value = _uiState.value.copy(isProxyActive = false)
            Logger.i(TAG, "Proxy service stopped")
        }
    }

    fun updateProxyType(type: ProxyType) {
        Logger.d(TAG, "updateProxyType: $type")
        viewModelScope.launch {
            proxyConfig.value = proxyConfig.value.copy(proxyType = type)
            _uiState.value = _uiState.value.copy(selectedProxyType = type)
        }
    }

    fun updatePort(port: Int) {
        Logger.d(TAG, "updatePort: $port")
        viewModelScope.launch {
            proxyConfig.value = proxyConfig.value.copy(port = port)
            _uiState.value = _uiState.value.copy(port = port)
        }
    }
    
    fun selectIpAddress(ip: String) {
        Logger.d(TAG, "selectIpAddress: $ip")
        _uiState.value = _uiState.value.copy(selectedIpAddress = ip)
        
        // Save the selected IP address
        viewModelScope.launch {
            try {
                val preferenceManager = PreferenceManager.getInstance(context)
                preferenceManager.saveSelectedIpAddress(ip)
                Logger.d(TAG, "Saved selected IP address: $ip")
            } catch (e: Exception) {
                Logger.e(TAG, "Error saving selected IP address", e)
            }
        }
    }

    private fun startProxyService() {
        Logger.d(TAG, "startProxyService called")
        viewModelScope.launch {
            try {
                proxyConfig.value = proxyConfig.value.copy(isActive = true)
                val intent = Intent(context, ProxyServerService::class.java)
                context.startService(intent)
                _uiState.value = _uiState.value.copy(
                    isProxyActive = true,
                    errorMessage = null
                )
                Logger.i(TAG, "Proxy service started")
            } catch (e: Exception) {
                Logger.e(TAG, "Error starting proxy service", e)
                _uiState.value = _uiState.value.copy(
                    isProxyActive = false,
                    errorMessage = "Failed to start proxy service: ${e.message}"
                )
            }
        }
    }

    /**
     * Load saved proxy settings from preferences
     */
    private fun loadSavedProxySettings() {
        Logger.d(TAG, "loadSavedProxySettings called")
        try {
            val preferenceManager = PreferenceManager.getInstance(context)
            val savedConfig = preferenceManager.loadProxySettings()
            val savedIpAddress = preferenceManager.getSelectedIpAddress()
            
            // Update the proxy config
            proxyConfig.value = proxyConfig.value.copy(
                proxyType = savedConfig.proxyType,
                port = savedConfig.port
            )
            
            // Update UI state
            _uiState.value = _uiState.value.copy(
                selectedProxyType = savedConfig.proxyType,
                port = savedConfig.port,
                selectedIpAddress = savedIpAddress
            )
            
            Logger.d(TAG, "Loaded saved proxy settings: ${savedConfig.proxyType}, port: ${savedConfig.port}, IP: $savedIpAddress")
        } catch (e: Exception) {
            Logger.e(TAG, "Error loading saved proxy settings", e)
        }
    }

    /**
     * Continuously monitor network and proxy status
     */
    private fun startContinuousStatusMonitoring() {
        viewModelScope.launch {
            while (true) {
                try {
                    // Check VPN status
                    val isVpnConnected = NetworkUtils.isVpnConnected(context)
                    
                    // Check hotspot status
                    val isHotspotEnabled = NetworkUtils.isHotspotEnabled(context)
                    
                    // Get available IPs
                    val availableIPs = NetworkUtils.getAvailableIPs(context)
                    
                    // Preserve selected IP if it's still available, otherwise select first available
                    val currentSelectedIp = _uiState.value.selectedIpAddress
                    val newSelectedIp = if (currentSelectedIp.isNotEmpty() && availableIPs.contains(currentSelectedIp)) {
                        currentSelectedIp
                    } else if (availableIPs.isNotEmpty()) {
                        availableIPs.first()
                    } else {
                        ""
                    }
                    
                    // Update UI state
                    _uiState.value = _uiState.value.copy(
                        isVpnConnected = isVpnConnected,
                        isHotspotEnabled = isHotspotEnabled,
                        availableIPs = availableIPs,
                        selectedIpAddress = newSelectedIp
                    )
                    
                    Logger.d(TAG, "Status update - VPN: $isVpnConnected, Hotspot: $isHotspotEnabled, IPs: ${availableIPs.size}, Selected: $newSelectedIp")
                } catch (e: Exception) {
                    Logger.e(TAG, "Error checking status", e)
                }
                
                // Wait before next check
                delay(STATUS_CHECK_INTERVAL)
            }
        }
    }
    
    private fun getAvailableIPs() {
        viewModelScope.launch {
            try {
                val availableIPs = NetworkUtils.getAvailableIPs(context)
                val selectedIp = if (availableIPs.isNotEmpty()) availableIPs.first() else ""
                _uiState.value = _uiState.value.copy(
                    availableIPs = availableIPs,
                    selectedIpAddress = selectedIp
                )
                Logger.d(TAG, "Available IPs: $availableIPs, Selected: $selectedIp")
            } catch (e: Exception) {
                Logger.e(TAG, "Error getting available IPs", e)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        Logger.d(TAG, "HomeViewModel cleared")
    }
}

data class HomeUiState(
    val isVpnConnected: Boolean = false,
    val isProxyActive: Boolean = false,
    val needsVpnPermission: Boolean = false,
    val selectedProxyType: ProxyType = ProxyType.HTTP,
    val port: Int = 8080,
    val isHotspotEnabled: Boolean = false,
    val errorMessage: String? = null,
    val availableIPs: List<String> = emptyList(),
    val selectedIpAddress: String = ""
)