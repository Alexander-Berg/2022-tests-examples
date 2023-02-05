package ru.yandex.market.test.kakao.matchers

import android.content.Context
import android.graphics.drawable.InsetDrawable
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.text.getSpans
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import ru.yandex.market.uikit.spannables.ForegroundGradientSpan
import ru.yandex.market.uikit.utils.GradientColor

class TextViewSpannableTextMatcher(private val spans: List<SpannableDescription>) : TypeSafeMatcher<View>() {

    override fun describeTo(description: Description?) {
        description?.appendText("TextView has spannable text with\n")
        spans.forEach {
            description?.appendText(it.toString())?.appendText("\n")
        }
    }

    override fun describeMismatchSafely(item: View?, mismatchDescription: Description?) {
        when {
            item is TextView && item.text is Spanned -> {
                val spannedText = (item.text as Spanned)
                val spans = spannedText.getSpans<Any>(0, item.text.length)
                mismatchDescription?.appendText("TextView has spans\n")
                spans.forEach {
                    mismatchDescription?.appendText(getSpanMismatchDescription(it, spannedText))?.appendText("\n")
                }
            }
            item is TextView -> {
                mismatchDescription?.appendText("text is not Spannable")
            }
            else -> {
                mismatchDescription?.appendText("view is not TextView")
            }
        }
    }

    override fun matchesSafely(item: View?): Boolean {
        return (item is TextView) && matchSpans(item.text, item.context)
    }

    private fun matchSpans(text: CharSequence, context: Context): Boolean {
        val spannableText = text as? Spanned
        var matches = spannableText != null
        if (spannableText != null) {
            spans.forEach {
                matches = matches && when (it) {
                    is SpannableDescription.Image -> matchImageSpan(it, spannableText, context)
                    is SpannableDescription.Color -> matchColorSpan(it, spannableText, context)
                    is SpannableDescription.Gradient -> matchGradientSpan(it, spannableText)
                }
            }
        }
        return matches
    }

    private fun matchImageSpan(expected: SpannableDescription.Image, actual: Spanned, context: Context): Boolean {
        val expectedImage = AppCompatResources.getDrawable(context, expected.drawableRes)
        val actualSpan = actual.getSpans(expected.start, expected.end, ImageSpan::class.java)
        return if (actualSpan.size == 1) {
            val actualImage = actualSpan[0].drawable
            if (actualImage is InsetDrawable) {
                actualImage.drawable?.matches(expectedImage) == true
            } else {
                actualImage.matches(expectedImage)
            }
        } else {
            false
        }
    }

    private fun matchColorSpan(expected: SpannableDescription.Color, actual: Spanned, context: Context): Boolean {
        val actualSpan = actual.getSpans(expected.start, expected.end, ForegroundColorSpan::class.java)
        return actualSpan.size == 1 && actualSpan[0].foregroundColor == context.getColor(expected.color)
    }

    private fun matchGradientSpan(expected: SpannableDescription.Gradient, actual: Spanned): Boolean {
        val actualSpan = actual.getSpans(expected.start, expected.end, ForegroundGradientSpan::class.java)
        return actualSpan.size == 1 && actualSpan[0].gradientColor == expected.color
    }

    private fun getSpanMismatchDescription(span: Any, spannedText: Spanned): String {
        val start = spannedText.getSpanStart(span)
        val end = spannedText.getSpanEnd(span)
        val className = when (span) {
            is ImageSpan -> SpannableDescription.Image::class.java.simpleName
            is ForegroundColorSpan -> SpannableDescription.Color::class.java.simpleName
            is ForegroundGradientSpan -> SpannableDescription.Gradient::class.java.simpleName
            else -> span::class.java.simpleName
        }
        return "$className(start= $start, end= $end)"
    }

    sealed class SpannableDescription {
        abstract val start: Int
        abstract val end: Int

        data class Image(
            override val start: Int,
            override val end: Int,
            @DrawableRes val drawableRes: Int
        ) : SpannableDescription()

        data class Color(
            override val start: Int,
            override val end: Int,
            @ColorRes val color: Int
        ) : SpannableDescription()

        data class Gradient(
            override val start: Int,
            override val end: Int,
            val color: GradientColor
        ) : SpannableDescription()

    }
}