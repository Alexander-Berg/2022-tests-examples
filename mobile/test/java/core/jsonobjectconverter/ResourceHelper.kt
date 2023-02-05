package core.jsonobjectconverter

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.lang.StringBuilder

object ResourceHelper {
    fun getResponse(resourcePath: String): String {
        val res = StringBuilder()
        try {
            val inputStream = ResourceHelper::class.java.getResourceAsStream(resourcePath)
            if (inputStream != null) {
                val isr = InputStreamReader(inputStream)
                val br = BufferedReader(isr)
                var str = br.use { reader ->
                    reader.readLine()
                }
                while (str != null) {
                    res.append(str)
                    str = br.use { reader ->
                        reader.readLine()
                    }
                }
                isr.close()
                br.close()
                inputStream.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        check(res.isNotEmpty()) { "Response not found $resourcePath" }
        return res.toString()
    }
}
