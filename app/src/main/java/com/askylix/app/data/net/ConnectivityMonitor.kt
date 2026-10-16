package com.askylix.app.data.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Whether the phone has a working connection right now.
 *
 * Only for telling the person: a "No connection" note while there is none. Whether a call can be made
 * is decided by trying it, because a phone that claims to be online can still fail, and one that claims
 * not to sometimes works.
 */
class ConnectivityMonitor(context: Context) {

    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)

    /** True while the default network can reach the internet. Starts with the answer for right now. */
    val online: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(isOnline()) }
            override fun onLost(network: Network) { trySend(isOnline()) }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.hasUsableInternet())
            }
        }
        trySend(isOnline())
        manager.registerDefaultNetworkCallback(callback)
        awaitClose { manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()

    private fun isOnline(): Boolean =
        manager.getNetworkCapabilities(manager.activeNetwork)?.hasUsableInternet() == true

    private fun NetworkCapabilities.hasUsableInternet(): Boolean =
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
