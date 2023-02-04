package com.yandex.maps.testapp.mapdesign

import java.io.*
import java.lang.Exception
import java.net.URL
import javax.net.ssl.SSLHandshakeException

object Utils {
    @JvmStatic fun inputStreamToString(inputStream: InputStream): String {
        val bufferedReader = inputStream.bufferedReader()
        try {
            return bufferedReader.readText()
        } finally {
            bufferedReader.close()
        }
    }

    @JvmStatic fun requestContent(urlString: String): String {
        return try {
            val url = URL(urlString)
            url.openConnection().run {
                connectTimeout = 5000
                readTimeout = 10000
                inputStreamToString(getInputStream())
            }
        } catch (e: SSLHandshakeException) {
            throw CertificateError(e.toString())
        } catch (e: Exception) {
            throw ConnectionError(e.toString())
        }
    }
}