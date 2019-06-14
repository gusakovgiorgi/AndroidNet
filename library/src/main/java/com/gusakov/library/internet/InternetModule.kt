package com.gusakov.library.internet

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class InternetModule private constructor(val context: Context) {
    private val listenersMap: MutableMap<Context, (_: Boolean) -> Unit> = mutableMapOf()
    private var networkListener: NetworkListener? = null

    companion object {
        private var INSTANCE: InternetModule? = null

        fun getInstance(): InternetModule {
            return INSTANCE ?: throw RuntimeException("InternetModule isn't initialized.")
        }
    }

    fun startListening(context: Context, callback: (internetConnected: Boolean) -> Unit) {
        listenersMap[context] = callback
        if (networkListener == null) {
            initializeNetworkListener(context)
        }
    }

    fun checkInternetNow() {
        networkListener?.startPinging()
    }

    private fun initializeNetworkListener(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkListener = NetworkListener(context.applicationContext) {
                listenersMap.values.forEach { callback ->
                    callback(it)
                }
            }
        } else {
            context.registerReceiver(NetworkReceiver(), IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        }
    }

    fun removeListeners(context: Context) {
        listenersMap.remove(context)
    }

    init {
        INSTANCE = this
    }

    fun getInternetSpeed(
        downloadableFile: DownloadableFile = DownloadableFile(),
        speedTesterOptions: SpeedTesterOptions = SpeedTesterOptions(),
        callback: (unitPerSecond: Float) -> Unit
    ) = GlobalScope.launch(Dispatchers.Main) {
        val tester = SpeedTester(downloadableFile, speedTesterOptions)
        var result = -1F
        try {
            result = withContext(Dispatchers.IO) { tester.test() }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        callback(result)
    }

    class Builder(val context: Context) {
        fun build() = InternetModule(context)
    }
}