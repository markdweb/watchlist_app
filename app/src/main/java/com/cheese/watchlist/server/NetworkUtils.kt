package com.cheese.watchlist.server

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address

/** WiFi-specific network detection so PC Manager only reports an address when actually on WiFi. */
object NetworkUtils {

    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /** Returns the phone's local WiFi IPv4 address (e.g. "192.168.1.100"), or null if not on WiFi. */
    fun getWifiIpAddress(context: Context): String? {
        if (!isWifiConnected(context)) return null
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return null
        val network = cm.activeNetwork ?: return null
        val linkProperties = cm.getLinkProperties(network) ?: return null
        return linkProperties.linkAddresses
            .map { it.address }
            .filterIsInstance<Inet4Address>()
            .firstOrNull { !it.isLoopbackAddress }
            ?.hostAddress
    }
}
