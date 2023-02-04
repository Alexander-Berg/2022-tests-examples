package ru.auto.ara.core

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class TestEmptyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()

        intent?.also { intent ->
            val res = intent.getIntExtra(EXTRA_RES, Activity.RESULT_OK)
            val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
            setResult(res, data)
        }

        finish()
    }

    companion object {
        private const val EXTRA_RES = "EXTRA_RES"
        private const val EXTRA_DATA = "EXTRA_DATA"

        fun getIntent(context: Context, res: Int, data: Intent?) = Intent(context, TestEmptyActivity::class.java)
            .apply {
                putExtra(EXTRA_RES, res)
                data?.also { putExtra(EXTRA_DATA, it) }
            }
    }

}
