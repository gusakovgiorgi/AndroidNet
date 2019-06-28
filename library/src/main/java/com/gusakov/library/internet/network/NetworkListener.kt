package com.gusakov.library.internet.network

import com.gusakov.library.internet.InternetConfiguration

internal abstract class NetworkListener(
    val configuration: InternetConfiguration,
    private val networkAvailableCallBack: (physicallyConnected: Boolean, connectedToWorld: Boolean) -> Unit
) {
    /**
     * It may be situation when dome physical device connected to internet, but we yet don't know about it.
     * And, for example, if we are trying to check speed and we connect ot url, than it can help us identifying that network
     * is available
     */
    abstract fun connectedToSomeInternetResource()

    /**
     * It may be situation when dome physical device can't connected to internet, but we yet don't know about it.
     * And, for example, if we are trying to check speed and we connect ot url, than it can help us identifying that network
     * is available
     */
    abstract fun notConnectedToSomeInternetResource()

    abstract fun checkOnlineConnection()
    /**
     * Invoke when we don't need this listener anymore and to release resources
     */
    abstract fun clear()

}