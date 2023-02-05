package ru.yandex.market.test.kakao.views

import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.test.espresso.ViewAssertion
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import ru.beru.android.R

class KFactsSummaryContainer(function: ViewBuilder.() -> Unit) : KBaseView<KFactsSummaryContainer>(function)

fun KFactsSummaryContainer.checkFactStat(
    position: Int,
    value: Float,
    name: String,
    progress: Int
) {
    view.check(
        ViewAssertion { view, noViewFoundException ->
            if (view is LinearLayout) {
                val item = view.getChildAt(position)
                val factProgress = item.findViewById<ProgressBar>(R.id.factPercent)
                val factValue: TextView = item.findViewById(R.id.factValue)
                val factName: TextView = item.findViewById(R.id.factName)

                if (factProgress.progress == progress
                    && factValue.text.toString() == "$value"
                    && factName.text == name
                ) {
                    return@ViewAssertion
                }

                throw AssertionError("Some of fact's values don't match")
            } else {
                noViewFoundException.let {
                    throw AssertionError(it)
                }
            }
        }
    )
}