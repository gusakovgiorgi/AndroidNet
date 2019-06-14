package com.gusakov.library.internet.ping

import java.util.regex.Pattern

class InformationExtractor {

    val patternBytes = Pattern.compile("bytes from .+time=(\\d+\\.?\\d+)")
    val patternOveral = Pattern.compile("(\\d+)\\s+packet[s]? transmitted.+(\\d+).+received")

    fun extract(pingMessage: String): PingResult {
        println("ping message: $pingMessage")
        val times = mutableListOf<Float>()
        var packetsTransmitted: Int = 0
        var packetsReceived: Int = 0
        var timeStr: String? = null
        try {
            var matcher = patternBytes.matcher(pingMessage)
            while (matcher.find()) {
                timeStr = matcher.group(1)
                times.add(timeStr.toFloat())
            }

            matcher = patternOveral.matcher(pingMessage)
            if (matcher.find()) {
                packetsTransmitted = matcher.group(1).toInt()
                packetsReceived = matcher.group(2).toInt()
            } else {
                return PingResult(errorMessage = "Can't extract number of sent and received bytes for this message: $pingMessage")
            }
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
            return PingResult(errorMessage = "Can't parse this message: $pingMessage")
        } catch (e2: NumberFormatException) {
            e2.printStackTrace()
            return PingResult(errorMessage = "Can't convert string to float")
        }
        var timeSum = 0F
        times.forEach {
            timeSum += it
        }
        val averagePingTime = timeSum / times.size
        return PingResult(packetsTransmitted, packetsReceived, averagePingTime)
    }
}