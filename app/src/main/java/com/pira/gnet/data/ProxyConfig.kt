package com.pira.gnet.data

data class ProxyConfig(
    val proxyType: ProxyType = ProxyType.HTTP,
    val port: Int = 8080,
    val isActive: Boolean = false
)

enum class ProxyType {
    HTTP,
    SOCKS5
}