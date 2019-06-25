package com.gusakov.library.internet

import android.content.Context
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import com.gusakov.library.internet.ping.PingResult
import com.gusakov.library.internet.ping.Pinger
import com.gusakov.library.internet.speed_testing.DownloadableFile
import com.gusakov.library.internet.speed_testing.InformationUnit
import com.gusakov.library.internet.speed_testing.SpeedTester
import com.gusakov.library.internet.speed_testing.SpeedTesterOptions
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
        callback: (unitPerSecond: Float, unit: InformationUnit) -> Unit
    ) {
        GlobalScope.launch(Dispatchers.Main) {
            val tester = SpeedTester(downloadableFile, speedTesterOptions)
            var result = -1F
            try {
                result = withContext(Dispatchers.IO) {
                    tester.test(object :SpeedTester.IntermediateResult{
                        override fun connected() {
                            Log.v("test","get intermediate result in thread ${Thread.currentThread()}")
                        }
                    })
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
            callback(result, downloadableFile.informationUnit)
        }
    }

    class Builder(val context: Context) {
        fun build() = InternetModule(context)
    }
}