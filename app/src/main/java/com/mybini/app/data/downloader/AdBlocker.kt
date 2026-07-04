package com.mybini.app.data.downloader

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URI

object AdBlocker {
    private val blockedDomains = HashSet<String>()

    fun init(context: Context) {
        try {
            val assetManager = context.assets
            val inputStream = assetManager.open("ads-blocklist.txt")
            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val trimmed = line!!.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    blockedDomains.add(trimmed)
                }
            }
            reader.close()
            inputStream.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shouldBlock(url: String): Boolean {
        return try {
            val host = URI(url).host ?: return false
            // Periksa apakah host persis sama atau berakhiran domain yang terdaftar di blocklist
            blockedDomains.any { host == it || host.endsWith(".$it") }
        } catch (e: Exception) {
            false
        }
    }
}
