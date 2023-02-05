package com.yandex.mail.testopithecus.pages

import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.test.espresso.Espresso
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.util.HumanReadables
import androidx.test.espresso.web.assertion.WebViewAssertions
import androidx.test.espresso.web.matcher.DomMatchers
import androidx.test.espresso.web.sugar.Web
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.Locator
import com.yandex.xplat.common.YSSet
import com.yandex.xplat.common.split
import org.hamcrest.Matcher
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class WebViewPage(private val resourceName: String) {

    fun clickByClassName(classname: String) {
        clickByXpath("//*[contains(@class, '$classname')]")
    }

    fun clickByXpath(xpath: String) {
        Web.onWebView(ViewMatchers.withResourceName(resourceName))
            .check(WebViewAssertions.webContent(DomMatchers.hasElementWithXpath(xpath)))
        Web.onWebView(ViewMatchers.withResourceName(resourceName))
            .withElement(DriverAtoms.findElement(Locator.XPATH, xpath))
            .perform(DriverAtoms.webClick())
    }

    private fun evaluateJsFunction(function: String): String {
        var output = ""

        class EvaluateJsAction(javaScriptString: String) : ViewAction, ValueCallback<String> {

            private val TIME_OUT: Long = 5000
            private val mJsString: String = javaScriptString
            private val mEvaluateFinished: AtomicBoolean = AtomicBoolean(false)

            override fun getDescription(): String {
                return "evaluate '$mJsString' on webview"
            }

            override fun getConstraints(): Matcher<View> {
                return ViewMatchers.isAssignableFrom(WebView::class.java)
            }

            override fun perform(uiController: UiController, view: View?) {
                uiController.loopMainThreadUntilIdle()

                val webView: WebView = view as WebView
                webView.evaluateJavascript(mJsString, this)

                val timeOut: Long = System.currentTimeMillis() + TIME_OUT
                while (!mEvaluateFinished.get()) {
                    if (timeOut < System.currentTimeMillis()) {
                        throw PerformException.Builder()
                            .withActionDescription(this.description)
                            .withViewDescription(HumanReadables.describe(view))
                            .withCause(
                                RuntimeException(
                                    String.format(
                                        Locale.ENGLISH,
                                        "Evaluating java script did not finish after %d ms of waiting.", TIME_OUT
                                    )
                                )
                            )
                            .build()
                    }
                    uiController.loopMainThreadForAtLeast(50)
                }
            }

            override fun onReceiveValue(value: String?) {
                mEvaluateFinished.set(true)
                output = requireNotNull(value)
            }
        }

        Espresso.onView(ViewMatchers.withResourceName(resourceName))
            .perform(EvaluateJsAction(function))

        return output.removeSurrounding("\"")
    }

    fun getHtmlData(xpath: String): String {
        Web.onWebView(ViewMatchers.withResourceName(resourceName))
            .check(WebViewAssertions.webContent(DomMatchers.hasElementWithXpath(xpath)))

        return evaluateJsFunction(
            """(function() {
                return (document.evaluate("$xpath", document, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null).snapshotItem(0).innerHTML)
            })()
            """.trimMargin()
        )
    }

    fun isElementExists(xpath: String): Boolean {
        try {
            Web.onWebView(ViewMatchers.withResourceName(resourceName)).withElement(
                DriverAtoms.findElement(
                    Locator.XPATH,
                    xpath
                )
            )
        } catch (notExist: RuntimeException) {
            return false
        }
        return true
    }

    fun clickNextSpan(name: String) {
        evaluateJsFunction(
            """(() => {
            const clickEvent = new MouseEvent("click", {"view": window, "bubbles": true, "cancelable": false});
            const labels = document.getElementsByClassName('message-details__label');
            labels.forEach(label => {
                for (const span of label.querySelectorAll("span")) {
                    if (span.textContent.includes(`$name`)) {
                        label.getElementsByClassName(`icon svg-remove-label`)[0].dispatchEvent(clickEvent)
                    }
                }
            })
        })()
        """.trimMargin()
        )
    }

    fun getText(xpath: String): String {
        return evaluateJsFunction(
            """
            (function() {
                return (document.evaluate(
                        "$xpath",
                        document,
                        null,
                        XPathResult.ORDERED_NODE_SNAPSHOT_TYPE,
                        null
                    ).snapshotItem(0).textContent)
            })()
            """.trimIndent()
        )
    }

    fun getTextFromListOfElements(xpath: String): YSSet<String> {
        val result = YSSet(
            evaluateJsFunction(
                """
                    (function() {
                        let elements = document.evaluate("$xpath", document, null, XPathResult.ANY_TYPE, null);
                        let texts = "";
                        while (element = elements.iterateNext()) { texts = [texts, element.textContent].filter(Boolean).join(",");};
                        return texts;
                    })()
                """
            ).split(",")
        )
        if (result !== null) {
            return result
        }
        return YSSet()
    }
}
