package com.gusakov.library.internet.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import com.gusakov.library.internet.InternetConfiguration
import com.gusakov.library.internet.STANDARD_NUMBER_OF_PACKETS
import com.gusakov.library.internet.STANDARD_TIMEOUT_SEC
import com.gusakov.library.internet.ping.PingResult
import com.gusakov.library.internet.ping.Pinger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


internal class OldNetworkListenerImpl(
    context: Context,
    config: InternetConfiguration,
    private val networkAvailableCallBack: (physicallyConnected: Boolean, connectedToWorld: Boolean) -> Unit
) :
    NetworkListener(config, networkAvailableCallBack) {

    private val broadcastReceiver: BroadcastReceiver
    private var connectedToWorld: Boolean? = null
    private var physicallyConnected = false
    private val pinger = Pinger.onAddress(config.address).setNumberOfPackets(STANDARD_NUMBER_OF_PACKETS).setTimeoutSec(
        STANDARD_TIMEOUT_SEC
    )
    private val UI = Dispatchers.Main

    init {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.v("test", "received internet event")
                val cm = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
                @Suppress("DEPRECATION") val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true
                if (isConnected) {
                    physicallyConnected = true
                    checkOnlineConnection()
                } else {
                    physicallyConnected = false
                    networkConnected()
                }
            }
        }
        @Suppress("DEPRECATION")
        context.registerReceiver(broadcastReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    override fun connectedToSomeInternetResource() {
        checkOnlineConnection()
    }

    override fun notConnectedToSomeInternetResource() {
        launchOnUiThread {
            networkConnected()
        }
    }

    override fun checkOnlineConnection() {
        pinger.ping {
            launchOnUiThread {
                handlePingResult(it)
            }
        }
    }

    override fun clear() {
        // NOP
    }

    private fun handlePingResult(pingResult: PingResult) {
        when {
            pingResult.errorMessage != null -> {
                Log.w("internet", "error message = ${pingResult.errorMessage}")
                networkDisconnected()
            }
            pingResult.packetsReceived!! > 0 -> networkConnected()
            else -> networkConnected()
        }
    }

    private fun networkDisconnected() {
        if (connectedToWorld == null || connectedToWorld!!) {
            connectedToWorld = false
            networkAvailableCallBack(physicallyConnected, false)
        }
    }

    private fun launchOnUiThread(function: () -> Unit) = GlobalScope.launch(UI) {
        function()
    }

    private fun networkConnected() {
        if (connectedToWorld == null || !connectedToWorld!!) {
            connectedToWorld = true
            networkAvailableCallBack(physicallyConnected, true)
        }
    }

}