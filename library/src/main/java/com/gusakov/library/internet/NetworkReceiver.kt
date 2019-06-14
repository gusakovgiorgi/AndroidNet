package com.gusakov.library.internet

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.util.Log
import android.widget.Toast
import com.gusakov.library.internet.ping.Pinger


class NetworkReceiver : BroadcastReceiver() {

    var netWorkConnected = false
    var pingMs = 0

    override fun onReceive(context: Context, intent: Intent) {
        val conn = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//        for (network in conn.allNetworks) {
//            Log.v("internet", "network capabilities ${conn.getNetworkCapabilities(network)}")
//            Log.v("internet", "link properties ${conn.getLinkProperties(network)}")
//        }
        val networkInfo: NetworkInfo? = conn.activeNetworkInfo
        if (networkInfo?.isConnected == true) {
            netWorkConnected = true
            Toast.makeText(context, "Network connected", Toast.LENGTH_SHORT).show()
            Log.e("internet", "network connected. $networkInfo")
            Pinger.onAddress("google.com").ping {
                Log.v("ping", it.toString())
            }
        } else {
            netWorkConnected = false
            Toast.makeText(context, "Network not connected", Toast.LENGTH_SHORT).show()
            Log.e("internet", "network not connected. $networkInfo")
        }

    }

}