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
    private var networkConnected = false
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
                if (networkConnected) {
                    networkConnected = false
                    networkAvailableCallBack(false)
                }
            }

            override fun onLinkPropertiesChanged(network: Network?, linkProperties: LinkProperties?) {
                Log.v("internet", "link properties changed. newtork = $network. linkproperties = $linkProperties")
                if (!networkAvailableInvoked){
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
                Log.v("internet", "Now network $network is available")
                networkAvailableInvoked = true
                startPinging()
            }
        })
    }

    internal fun startPinging() {
        pinger.ping {
            handlePingResult(it)
        }
    }

    private fun handlePingResult(pingResult: PingResult) {
        when {
            pingResult.errorMessage != null -> networkDisconnected()
            pingResult.packetsReceived!! > 0 -> networkConnected()
            else -> networkConnected()
        }
    }

    private fun networkDisconnected() {
        if (networkConnected) {
            networkConnected = false
            networkAvailableCallBack(false)
        }
    }

    private fun networkConnected() {
        if (!networkConnected) {
            networkConnected = true
            networkAvailableCallBack(true)
        }
    }
}