package com.gusakov.library.internet

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.Log
import com.gusakov.library.internet.network.NetworkListener
import com.gusakov.library.internet.network.NetworkListenerImpl
import com.gusakov.library.internet.network.OldNetworkListenerImpl
import com.gusakov.library.internet.speed_testing.DownloadableFile
import com.gusakov.library.internet.speed_testing.InformationUnit
import com.gusakov.library.internet.speed_testing.SpeedTester
import com.gusakov.library.internet.speed_testing.SpeedTesterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class InternetModule(private val config: InternetConfiguration) {
    private val listenersMap: MutableMap<Context, (arg1: Boolean, arg2: Boolean) -> Unit> =
        mutableMapOf()
    private var networkListener: NetworkListener? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        private var INSTANCE: InternetModule? = null

        fun getInstance(): InternetModule {
            return INSTANCE ?: throw RuntimeException("InternetModule isn't initialized.")
        }
    }

    fun startListening(
        context: Context,
        callback: (physicallyConnected: Boolean, connectedToWorld: Boolean) -> Unit
    ) {
        listenersMap[context] = callback
        if (networkListener == null) {
            initializeNetworkListener(context)
        }
    }

    private fun initializeNetworkListener(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkListener =
                NetworkListenerImpl(
                    context.applicationContext,
                    config
                ) { physicalConn: Boolean, internetConn: Boolean ->
                    notifyListeners(physicalConn, internetConn)
                }
        } else {
            networkListener =
                OldNetworkListenerImpl(
                    context.applicationContext,
                    config
                ) { physicalConn: Boolean, internetConn: Boolean ->
                    notifyListeners(physicalConn, internetConn)
                }
        }
    }

    private fun notifyListeners(physicallyConnected: Boolean, internetConnected: Boolean) {
        listenersMap.values.forEach { callback ->
            callback(physicallyConnected, internetConnected)
        }
    }

    fun removeListeners(context: Context) {
        listenersMap.remove(context)
        if (listenersMap.isEmpty()) {
            networkListener?.clear()
            networkListener = null
        }
    }

    init {
        INSTANCE = this
    }

    /**
     * Recheck the connection. For example, if it is still connected to remote access point but not
     * connected to www anymore
     */
    fun recheckConnection() {
        networkListener?.checkOnlineConnection()
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
                    tester.test(object : SpeedTester.IntermediateResult {
                        override fun connected() {
                            Log.v(
                                "test",
                                "get intermediate result in thread ${Thread.currentThread()}"
                            )
                            networkListener?.connectedToSomeInternetResource()
                        }
                    })
                }
            } catch (e: IOException) {
                e.printStackTrace()
                networkListener?.notConnectedToSomeInternetResource()
            }
            callback(result, downloadableFile.informationUnit)
        }
    }

    class Builder {
        val internetConfiguration: InternetConfiguration

        init {
            internetConfiguration = InternetConfiguration()
        }

        /**
         * Library decide is internet available or not bia checking address availability. Standard checking address
         * is 8.8.8.8 but you can set your own as domain or via ip address
         */
        fun internetAvailabilityProofAddress(address: String) {
            internetConfiguration.address = address
        }

        fun build() = InternetModule(internetConfiguration)
    }
}