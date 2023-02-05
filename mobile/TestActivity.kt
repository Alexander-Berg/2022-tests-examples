package ru.yandex.market.uikitapp

import android.content.Context
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import dagger.android.support.DaggerAppCompatActivity
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.android.synthetic.main.activity_test.textView
import ru.yandex.market.app.ui.kit.R
import ru.yandex.market.uikit.spannables.Spans

class TestActivity : DaggerAppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_test)

        val exampleString = SpannableStringBuilder().apply {
            append("Текст очень очень длинный")
            append(System.lineSeparator())
            append("Цена ")
            append("10000р", Spans.strikeThrough(this@TestActivity), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            append(System.lineSeparator())
            append("Ещё какой-то текст")
        }
        textView.text = exampleString

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.rootContainer, ParentFragment())
                .commit()
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        if (newBase != null) {
            super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
        } else {
            super.attachBaseContext(newBase)
        }
    }
}