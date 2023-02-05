package ru.yandex.market.test.kakao.views

import android.text.Spannable
import android.widget.TextView
import androidx.test.espresso.ViewAssertion
import io.github.kakaocup.kakao.text.KTextView
import ru.yandex.market.uikit.spannables.LinkClickableSpan

fun KTextView.hasLink(linkText: String) {
    view.check(
        ViewAssertion { view, noViewFoundException ->
            if (view is TextView) {
                val text = view.text

                if (text is Spannable) {
                    val spans = text.getSpans(0, text.length, LinkClickableSpan::class.java)

                    if (spans.isEmpty()) throw AssertionError("The TextView doesn't have LinkClickableSpans")

                    val span = spans.firstOrNull {
                        text.substring(
                            text.getSpanStart(it),
                            text.getSpanEnd(it)
                        ) == linkText
                    }

                    if (span == null) throw AssertionError("""The TextView doesn't have a link with the text "$linkText"""")
                } else {
                    throw AssertionError("The TextView text is expected to be Spannable")
                }
            } else if (view != null) {
                throw AssertionError("The view is expected to be TextView but is ${view::class}")
            } else {
                throw noViewFoundException
            }
        }
    )
}