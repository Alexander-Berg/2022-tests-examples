package ru.yandex.yandexbus.favorite

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.FileNotFoundException
import java.io.InputStream

class MainTestActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    @SuppressLint("Recycle")
    override fun onResume() {
        super.onResume()

        val result = try {
            val inputStream = contentResolver.openInputStream(Uri.parse(favorites))
            if (inputStream != null) {
                inputStream.printDump()
                R.string.success
            } else {
                R.string.failure
            }
        } catch (e: SecurityException) {
            Log.i(TAG, "error = $e")
            R.string.failure
        } catch (e: FileNotFoundException){
            Log.i(TAG, "error = $e")
            R.string.failure
        }

        val text = findViewById<TextView>(R.id.result)
        text.setText(result)
    }

    fun InputStream.printDump() {
        if (available() > 0) {
            val buffer = ByteArray(available())
            read(buffer)
            Log.w(TAG, "data: ${String(buffer)}")
        }
    }

    companion object {
        const val TAG = "FAVIROTE_EXPORT_TEST"
        val favorites = "content://ru.yandex.yandexbus.provider.favorites"
    }
}
