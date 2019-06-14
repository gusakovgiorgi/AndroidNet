package com.gusakov.library.internet.ping

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader

class PingOptions(var timeoutSec: Int = 4, var numberOfPackets: Int = 3)
data class PingResult(
    val packetsTransmitted: Int? = null,
    val packetsReceived: Int? = null,
    val averagePingTime: Float? = null,
    val errorMessage: String? = null
)

class Pinger private constructor(private val address: String) {

    private var pingOptions = PingOptions()

    companion object {
        fun onAddress(address: String): Pinger {
            return Pinger(address)
        }
    }

    fun setTimeoutSec(timeoutSec: Int): Pinger {
        pingOptions.timeoutSec = timeoutSec
        return this
    }

    fun setNumberOfPackets(numberOfPackets: Int): Pinger {
        pingOptions.numberOfPackets = numberOfPackets
        return this
    }

    fun ping(callBack: (pingResult: PingResult) -> Unit) = GlobalScope.launch(Dispatchers.Main) {
        val result = withContext(Dispatchers.IO) {
            var process: Process? = null
            try {
                process = Runtime.getRuntime()
                    .exec("/system/bin/ping -c ${pingOptions.numberOfPackets} -w ${pingOptions.timeoutSec} $address")
                val exitValue = process.waitFor()
                // Success
                if (exitValue == 0) {
                    return@withContext InformationExtractor().extract(getStringFromStream(process.inputStream))
                } else {
                    return@withContext PingResult(errorMessage = "exit value is $exitValue, ${getStringFromStream(process.errorStream)}")
                }
            } catch (e: IOException) {
                e.printStackTrace()
                return@withContext PingResult(errorMessage = e.message)
            } finally {
                process?.destroy()
            }
        }
        callBack(result)
    }

    private fun pingResultString(): String {
        try {
            val process = Runtime.getRuntime()
                .exec("/system/bin/ping -c ${pingOptions.numberOfPackets} -w ${pingOptions.timeoutSec} $address")
            val exitValue = process.waitFor()
            // Success
            return if (exitValue == 0) {
                getStringFromStream(process.inputStream)
            } else {
                getStringFromStream(process.errorStream)
            }

        } catch (ignore: InterruptedException) {
            ignore.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return ""
    }

    private fun getStringFromStream(inputStream: InputStream): String {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val resultStr = StringBuilder()
        var line: String? = reader.readLine()
        while (line != null) {
            resultStr.append(System.lineSeparator()).append(line)
            line = reader.readLine()
        }
        return resultStr.toString()
    }
}

