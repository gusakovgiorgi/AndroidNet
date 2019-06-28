package com.gusakov.library.internet.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.annotation.RequiresApi
import com.gusakov.library.internet.InternetConfiguration
import com.gusakov.library.internet.STANDARD_NUMBER_OF_PACKETS
import com.gusakov.library.internet.STANDARD_TIMEOUT_SEC
import com.gusakov.library.internet.ping.PingResult
import com.gusakov.library.internet.ping.Pinger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
internal class NetworkListenerImpl(
    context: Context,
    config: InternetConfiguration,
    private val networkAvailableCallBack: (physicallyConnected: Boolean, connectedToWorld: Boolean) -> Unit
) : NetworkListener(config, networkAvailableCallBack) {

    private val conn = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var connectedToWorld: Boolean? = null
    private var networkAvailableInvoked = false
    @Volatile
    private var physicallyConnected = false
    private val pinger =
        Pinger.onAddress(config.address).setNumberOfPackets(STANDARD_NUMBER_OF_PACKETS).setTimeoutSec(
            STANDARD_TIMEOUT_SEC
        )
    private val UI = Dispatchers.Main
    private val workerHandler: Handler

    init {
        val threadHandlerThread = HandlerThread("handlerThread")
        threadHandlerThread.start()
        workerHandler = Handler(threadHandlerThread.looper)
        conn.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network?, networkCapabilities: NetworkCapabilities?) {
                Log.v(
                    "internet",
                    "onCapabilites changed network=$network, networkCapabilities=$networkCapabilities. networkAv availableInvoked=$networkAvailableInvoked"
                )
            }

            override fun onLost(network: Network?) {
                Log.v("internet", "onlost network=$network")
                physicallyConnected = false
                launchOnUiThread {
                    networkDisconnected()
                }
            }

            override fun onLinkPropertiesChanged(network: Network?, linkProperties: LinkProperties?) {
                Log.v("internet", "link properties changed. newtork = $network. linkproperties = $linkProperties")
                if (!networkAvailableInvoked) {
                    checkOnlineConnection()
                }
                networkAvailableInvoked = false
            }

            override fun onUnavailable() {
                Log.v("internet", "network is unvailable")
            }

            override fun onLosing(network: Network?, maxMsToLive: Int) {
                Log.v("internet", "Losing network $network. max time is $maxMsToLive")
            }

            override fun onAvailable(network: Network?) {
                Log.v("internet", "Now network $network is available. thread id ${Thread.currentThread()}")
                networkAvailableInvoked = true
                physicallyConnected = true
                checkOnlineConnection()
            }
        })
    }

    internal fun startPinging() {
        Log.v("internet", "start pinging")
        pinger.ping {
            handlePingResult(it)
        }
    }

    private fun launchOnUiThread(function: () -> Unit) = GlobalScope.launch(UI) {
        function()
    }

    override fun checkOnlineConnection() {
        startPinging()
    }

    override fun connectedToSomeInternetResource() {
        GlobalScope.launch(workerHandler.asCoroutineDispatcher()) {
            networkConnected()
        }
    }

    override fun notConnectedToSomeInternetResource() {
        GlobalScope.launch(workerHandler.asCoroutineDispatcher()) {
            checkOnlineConnection()
        }
    }

    override fun clear() {
        // NOP
    }

    private fun handlePingResult(pingResult: PingResult) {
        when {
            pingResult.errorMessage != null -> {
                Log.w("internte", "error message = ${pingResult.errorMessage}")
                networkDisconnected()
            }
            pingResult.packetsReceived!! > 0 -> networkConnected()
            else -> networkConnected()
        }
    }

    private fun networkDisconnected() = GlobalScope.launch(workerHandler.asCoroutineDispatcher()) {
        if (connectedToWorld == null || connectedToWorld!!) {
            connectedToWorld = false
            launchOnUiThread {
                networkAvailableCallBack(physicallyConnected, false)
            }
        }
    }

    private fun networkConnected() = GlobalScope.launch(workerHandler.asCoroutineDispatcher()) {
        if (connectedToWorld == null || !connectedToWorld!!) {
            connectedToWorld = true
            launchOnUiThread {
                networkAvailableCallBack(physicallyConnected, true)
            }
        }
    }
}