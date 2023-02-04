package ru.auto.testextension

import com.google.gson.Gson
import java.io.IOException
import java.io.InputStream

object FileTestUtils {

    private val gson = Gson()

    @JvmStatic
    fun <T> readJsonAsset(assetPath: String, classOfT: Class<T>): T = gson.fromJson(readFromFile(assetPath), classOfT)

    @JvmStatic
    fun readFromFile(assetPath: String): String {
        var jsonString = ""
        var stream: InputStream? = null
        try {
            stream = this.javaClass.getResourceAsStream(assetPath)
            val size = stream.available()
            val buffer = ByteArray(size)
            stream.read(buffer)
            jsonString = String(buffer)
        } catch (e: IOException) {
            println("parse file, parsing error $e")
        } finally {
            closeSilently(stream)
        }
        return jsonString
    }


    @JvmStatic
    fun closeSilently(stream: InputStream?): Boolean {
        if (stream != null) {
            try {
                stream.close()
                return true
            } catch (e: IOException) {
                println("closing json reading stream failed $e")
                return false
            }
        } else {
            return false
        }
    }

}
