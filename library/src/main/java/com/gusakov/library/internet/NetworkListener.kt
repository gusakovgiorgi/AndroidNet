package com.gusakov.library.internet

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.gusakov.library.internet.ping.PingResult
import com.gusakov.library.internet.ping.Pinger

@RequiresApi(Build.VERSION_CODES.N)
class NetworkListener(context: Context, val networkAvailableCallBack: (connectedToWorld: Boolean) -> Unit) {
    private val conn = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var connectedToWorld: Boolean? = null
    private var networkAvailableInvoked = false
    private val pinger = Pinger.onAddress(GOOGLE).setNumberOfPackets(STANDARD_NUMBER_OF_PACKETS).setTimeoutSec(
        STANDARD_NUMBER_OF_PACKETS
    )

    init {
        conn.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network?, networkCapabilities: NetworkCapabilities?) {
                Log.v(
                    "internet",
                    "onCapabilites changed network=$network, networkCapabilities=$networkCapabilities. networkAv availableInvoked=$networkAvailableInvoked"
                )
            }

            override fun onLost(network: Network?) {
                Log.v("internet", "onlost network=$network")
                networkDisconnected()
            }

            override fun onLinkPropertiesChanged(network: Network?, linkProperties: LinkProperties?) {
                Log.v("internet", "link properties changed. newtork = $network. linkproperties = $linkProperties")
                if (!networkAvailableInvoked) {
                    startPinging()
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
                startPinging()
            }
        })
    }

    internal fun startPinging() {
        Log.v("internet", "start pinging")
        pinger.ping {
            handlePingResult(it)
        }
    }

    /**
     * It may be situation when dome phisical device connected to internet, but we yet don't know about it.
     * And, for example, if we are trying to check speed and we connect ot url, than it can help us identifying that network
     * is available
     */
    internal fun connectedToSomeInternetResource(){
//        if(!connectedToWorld){
//            startPinging()
//        }
    }

    private fun handlePingResult(pingResult: PingResult) {
        when {
            pingResult.errorMessage != null -> {
                Log.w("internte","error message = ${pingResult.errorMessage}")
                networkDisconnected()
            }
            pingResult.packetsReceived!! > 0 -> networkConnected()
            else -> networkConnected()
        }
    }

    private fun networkDisconnected() {
        if (connectedToWorld == null || connectedToWorld!!) {
            connectedToWorld = false
            networkAvailableCallBack(false)
        }
    }

    private fun networkConnected() {
        if (connectedToWorld == null || !connectedToWorld!!) {
            connectedToWorld = true
            networkAvailableCallBack(true)
        }
    }
}