package ru.yandex.market.test.util

import android.app.Instrumentation
import android.content.Context
import android.os.Build
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.FloatRange
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.ViewAssertion
import androidx.test.espresso.action.CoordinatesProvider
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import io.github.kakaocup.kakao.common.matchers.PositionMatcher
import io.github.kakaocup.kakao.common.views.KBaseView
import io.github.kakaocup.kakao.edit.KEditText
import io.github.kakaocup.kakao.rating.KRatingBar
import io.github.kakaocup.kakao.recycler.KRecyclerItem
import io.github.kakaocup.kakao.recycler.KRecyclerView
import io.github.kakaocup.kakao.text.KTextView
import org.hamcrest.Matcher
import ru.yandex.market.Dependencies
import ru.yandex.market.clean.presentation.formatter.MoneyFormatter
import ru.yandex.market.domain.money.model.Currency
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.utils.orNull
import ru.yandex.market.utils.toBitmap
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

fun KTextView.hasPriceText(price: Int, text: String?) {
    var formattedText = Dependencies.getMoneyFormatter().formatPrice(
        Money(BigDecimal(price), MoneyFormatter.DEFAULT_CURRENCY)
    )
    text?.let {
        formattedText = String.format(text, formattedText)
    }
    this.containsText(
        formattedText
    )
}

fun <T> KBaseView<T>.hasAlpha(@FloatRange(from = 0.0, to = 1.0) value: Float) {
    this.matches { withMatcher(ViewMatchers.withAlpha(value)) }
}

fun KRatingBar.setRatingStars(starsCount: Int) {

    val location = object : CoordinatesProvider {
        override fun calculateCoordinates(view: View?): FloatArray {
            if (view == null) return floatArrayOf(0f, 0f)

            val locationOnScreen = intArrayOf(0, 0)
            view.getLocationOnScreen(locationOnScreen)

            val starWidth = view.width / 5

            val x: Float = locationOnScreen[0] + starWidth * (starsCount.toFloat() - 0.5f)
            val y: Float = locationOnScreen[1] + view.height.toFloat() / 2

            return floatArrayOf(x, y)
        }
    }

    view.perform(
        GeneralClickAction(
            Tap.SINGLE, location, Press.FINGER,
            InputDevice.SOURCE_UNKNOWN, MotionEvent.BUTTON_PRIMARY
        )
    )
}

fun KTextView.hasCompoundDrawable(@DrawableRes drawableId: Int) {
    view.check(ViewAssertion { view, noViewFoundException ->
        if (view is TextView) {
            val drawable = view.context.getDrawable(drawableId)?.toBitmap()

            if (view.compoundDrawables.none { it?.toBitmap()?.sameAs(drawable) == true }) {
                throw AssertionError("TextView with text: ${view.text} doesn't have drawable with id: $drawableId")
            }
        } else {
            noViewFoundException?.let { throw AssertionError(it) }
        }
    })
}

fun KEditText.hasError(errorMsg: String) {
    view.check(ViewAssertion { view, noViewFoundException ->
        if (view is TextView) {
            if (view.error != errorMsg) {
                throw AssertionError("TextView with text: ${view.text} doesn't have error with text: $errorMsg")
            }
        } else {
            noViewFoundException?.let { throw AssertionError(it) }
        }
    })
}

fun KTextView.isBold() {
    view.check(ViewAssertion { view, noViewFoundException ->
        if (view is TextView) {
            if (!view.typeface.isBold) {
                throw AssertionError("TextView with text: ${view.text} is not bold")
            }
        } else {
            noViewFoundException?.let { throw AssertionError(it) }
        }
    })
}

fun formatPrice(price: Double, spaceChar: Char = '\u00A0'): String {
    val symbols = DecimalFormatSymbols().apply {
        decimalSeparator = '.'
        groupingSeparator = spaceChar
    }
    val df = DecimalFormat("###,###.##", symbols)
    return "${df.format(price)}${spaceChar}₽".replace(",", "$spaceChar")
}

fun formatPrice(price: Int, prefix: String? = null): String {
    return Dependencies.getPriceMapper()
        .map(BigDecimal.valueOf(price.toLong()), Currency.RUR)
        .orNull
        ?.let { Dependencies.getMoneyFormatter().formatPriceAsViewObject(it, prefix) }?.getFormatted()
        ?: ""
}

fun formatPhone(text: String): String {
    return String.format(
        "+%d (%03d) %03d-%02d-%02d",
        text.substring(1, 2).toInt(),
        text.substring(2, 5).toInt(),
        text.substring(5, 8).toInt(),
        text.substring(8, 10).toInt(),
        text.substring(10, 12).toInt()
    )
}

fun getString(@StringRes id: Int, vararg obj: Any): String {
    return InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *obj)
}

fun Instrumentation.isKeyboardShown(): Boolean {
    val inputMethodManager = targetContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    return inputMethodManager.isAcceptingText
}

fun UiDevice.closeSystemUiBrokenDialogIfExists(): Boolean {
    val systemDialog = this.findObject(By.pkg("android").clazz(FrameLayout::class.java))

    return if (systemDialog != null) {
        val closeButton = this.findObject(By.res("com.android.internal:id/aerr_close"))
            ?: this.findObject(By.res("android:id/closeButton"))
            ?: this.findObject(By.res("android:id/button1"))
        if (closeButton != null) {
            closeButton.click()
        } else {
            this.pressBack()
        }
        true
    } else {
        false
    }
}

fun UiDevice.hasSystemWindow(): Boolean {
    val systemDialog = this.findObject(By.pkg("android").clazz(FrameLayout::class.java))
        ?: getPermissionObject()
    return systemDialog != null
}

fun UiDevice.getPermissionObject(): UiObject2? {
    val packageInstallerPackageName: String = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
        "com.android.permissioncontroller"
    } else {
        "com.android.packageinstaller"
    }
    return this.findObject(By.pkg(packageInstallerPackageName))
}

fun UiDevice.findNotificationWithText(text: String): UiObject2? {
    openNotification()
    wait(Until.hasObject(By.textContains(text)), 10_000L)
    return findObject(By.textContains(text))
}

fun KRecyclerView.scrollToView(position: Int, @IdRes viewId: Int) {
    this.view.perform(object : ViewAction {
        override fun getDescription() = "Scroll RecyclerView to cartCounterButton"

        override fun getConstraints() = ViewMatchers.isAssignableFrom(RecyclerView::class.java)

        override fun perform(controller: UiController, view: View) {
            if (view is RecyclerView) {
                val viewHolderView = view.findViewHolderForLayoutPosition(position)!!.itemView
                val targetView = viewHolderView.findViewById<View>(viewId)
                val targetViewTop = targetView.top
                val viewHolderViewTop = viewHolderView.top
                view.scrollBy(0, targetViewTop - viewHolderViewTop + view.paddingBottom)
                controller.loopMainThreadUntilIdle()
            }
        }
    })
}

inline fun <reified T : KRecyclerItem<*>> KRecyclerView.childAtWithoutScroll(
    position: Int,
    function: T.() -> Unit
) {
    val provideItem = itemTypes.getOrElse(T::class) {
        throw IllegalStateException("${T::class.java.simpleName} did not register to KRecyclerView")
    }.provideItem

    val kRecyclerView = this
    function((provideItem(PositionMatcher(matcher, position)) as T).also { inRoot { withMatcher(kRecyclerView.root) } })
}

fun <T> KBaseView<T>.findRecyclerAndScrollTo() {
    this.view.perform(object : ViewAction {
        override fun getDescription() = "find parent recycler which allows scrolling and scroll it to its view position"

        override fun getConstraints() =
            ViewMatchers.isDescendantOfA(ViewMatchers.isAssignableFrom(RecyclerView::class.java))

        override fun perform(controller: UiController, view: View) {
            var viewPosition = view.top
            var parent = view.parent as? View
            var recyclerParent = view.parent as? RecyclerView
            while ((parent as? RecyclerView)?.isNestedScrollingEnabled != true && parent?.parent != null) {
                viewPosition += parent.top
                recyclerParent = parent.parent as? RecyclerView ?: recyclerParent
                parent = (parent.parent as? View)
            }

            recyclerParent?.let { recycler ->
                var yScroll = viewPosition
                yScroll -= recycler.scrollY
                yScroll -= recycler.measuredHeight
                yScroll += view.measuredHeight
                yScroll += recycler.paddingBottom

                recycler.scrollBy(0, yScroll)
            }
        }

    })
}

fun KTextView.clickSpan(text: String) {
    this.view.perform(object : ViewAction {
        override fun getDescription() = "find span in text view with given text and click on it"

        override fun getConstraints(): Matcher<View> {
            return ViewMatchers.isAssignableFrom(TextView::class.java)
        }

        override fun perform(controller: UiController, view: View) {
            val textView = view as TextView
            val spannableString = textView.text as Spanned

            if (spannableString.isEmpty()) {
                throw NoMatchingViewException.Builder()
                    .includeViewHierarchy(true)
                    .withRootView(textView)
                    .build()
            }

            val spans = spannableString.getSpans(0, spannableString.length, ClickableSpan::class.java)

            for (span in spans) {
                val start = spannableString.getSpanStart(span)
                val end = spannableString.getSpanEnd(span)
                val sequence = spannableString.subSequence(start, end)
                if (text == sequence.toString()) {
                    span.onClick(textView)
                    return
                }
            }

            throw NoMatchingViewException.Builder()
                .includeViewHierarchy(true)
                .withRootView(textView)
                .build()

        }

    })
}

fun KTextView.clickSpanBySpansNumber(spansNumber: Int) {
    this.view.perform(object : ViewAction {
        override fun getDescription() = "find span in text view with given text and click on it"

        override fun getConstraints(): Matcher<View> {
            return ViewMatchers.isAssignableFrom(TextView::class.java)
        }

        override fun perform(controller: UiController, view: View) {
            val textView = view as TextView
            val spannableString = textView.text as Spanned

            if (spannableString.isEmpty()) {
                throw NoMatchingViewException.Builder()
                    .includeViewHierarchy(true)
                    .withRootView(textView)
                    .build()
            }

            val spans = spannableString.getSpans(0, spannableString.length, ClickableSpan::class.java)
            val span = spans.getOrNull(spansNumber)
            if (span != null) {
                span.onClick(textView)
                return
            }

            throw NoMatchingViewException.Builder()
                .includeViewHierarchy(true)
                .withRootView(textView)
                .build()

        }

    })
}

inline fun <reified T : Any, R> T.getPrivateProperty(name: String): R? =
    T::class
        .memberProperties
        .firstOrNull { it.name == name }
        ?.apply { isAccessible = true }
        ?.get(this) as? R
