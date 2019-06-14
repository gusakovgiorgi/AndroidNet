package com.gusakov.library.internet

import java.io.IOException
import java.io.InputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class DownloadableFile(
    val size: Int = 40,
    val informationUnit: InformationUnit = InformationUnit.MEGABIT,
    val url: String = "https://gusakovgiorgi.github.io/5MBRandomFile.txt"
) {
    fun getSizeInBytes(): Int {
        val bytes: Int
        when (informationUnit) {
            InformationUnit.BYTE -> bytes = size
            InformationUnit.KILOBYTE -> bytes = size * 1024
            InformationUnit.MEGABYTE -> bytes = size * 1024 * 1024
            InformationUnit.MEGABIT -> bytes = size / 8 * 1024 * 1024
            InformationUnit.GIGABYTE -> bytes = size * 1024 * 1024 * 1024
        }
        return bytes
    }
}

class SpeedTesterOptions(val readTimeOutMs: Int = 3000, val connectTimeOutMs: Int = 3000)
enum class InformationUnit {
    BYTE, KILOBYTE, MEGABYTE, MEGABIT, GIGABYTE
}

class SpeedTester(
    val downloadableFile: DownloadableFile,
    val speedTesterOptions: SpeedTesterOptions = SpeedTesterOptions()
) {

    /**
     * Return speed test in informationUnit per second. Or return -1 if error occurred
     */
    @Throws(IOException::class)
    fun test(): Float {
        val url = URL(downloadableFile.url);
        var stream: InputStream? = null
        var connection: HttpsURLConnection? = null
        var startTimeMs: Long? = null
        var endTimeMs: Long? = null
        val bytesSize = downloadableFile.getSizeInBytes()
        try {

            connection = url.openConnection() as HttpsURLConnection
            // Timeout for reading InputStream arbitrarily set to 3000ms.
            connection.readTimeout = speedTesterOptions.readTimeOutMs
            // Timeout for connection.connect() arbitrarily set to 3000ms.
            connection.connectTimeout = speedTesterOptions.connectTimeOutMs
            // For this use case, set HTTP method to GET.
            connection.requestMethod = "GET"
            // Already true by default but setting just in case; needs to be true since this request
            // is carrying an input (response) body.
            connection.doInput = true
            // Open communications link (network traffic occurs here).
            connection.connect()
            val responseCode = connection.responseCode
            if (responseCode != HttpsURLConnection.HTTP_OK) {
                throw IOException("HTTP error code: $responseCode")
            }
            // Retrieve the response body as an InputStream.
            stream = connection.inputStream
            if (stream != null) {
                startTimeMs = System.currentTimeMillis()
                readStream(stream, downloadableFile.getSizeInBytes())
                endTimeMs = System.currentTimeMillis()
            }
        } finally {
            // Close Stream and disconnect HTTPS connection.
            stream?.close()
            connection?.disconnect()
        }
        return if (endTimeMs != null && startTimeMs != null) {
            val bytesPerSecond = (bytesSize.toFloat() / ((endTimeMs - startTimeMs) / 1000F))
            return convertToProperUnit(bytesPerSecond)
        } else {
            -1F
        }
    }

    private fun convertToProperUnit(bytesPerSecond: Float): Float {
        return when (downloadableFile.informationUnit) {
            InformationUnit.BYTE -> bytesPerSecond
            InformationUnit.KILOBYTE -> bytesPerSecond / 1024
            InformationUnit.MEGABYTE -> bytesPerSecond / 1024 / 1024
            InformationUnit.MEGABIT -> bytesPerSecond / 1024 / 1024 * 8
            InformationUnit.GIGABYTE -> bytesPerSecond / 1024 / 1024 / 1024
        }
    }


    /**
     * Converts the contents of an InputStream to a String.
     */
    @Throws(IOException::class)
    private fun readStream(stream: InputStream, maxLength: Int) {
        // Create temporary buffer to hold Stream data with specified max length.
        val buffer = ByteArray(maxLength)
        // Populate temporary buffer with Stream data.
        var numBytes = 0
        var readSize = 0
        while (numBytes < maxLength && readSize != -1) {
            numBytes += readSize
            readSize = stream.read(buffer, numBytes, buffer.size - numBytes)
        }
    }
}