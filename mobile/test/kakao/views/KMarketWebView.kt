package ru.yandex.market.test.kakao.views

import android.view.View
import android.webkit.WebView
import androidx.test.espresso.DataInteraction
import io.github.kakaocup.kakao.common.builders.ViewBuilder
import io.github.kakaocup.kakao.common.views.KBaseView
import org.hamcrest.Matcher
import ru.yandex.market.mocks.testUrl

class KMarketWebView : KBaseView<KMarketWebView> {

    constructor(function: ViewBuilder.() -> Unit) : super(function)

    constructor(parent: Matcher<View>, function: ViewBuilder.() -> Unit) : super(parent, function)

    constructor(parent: DataInteraction, function: ViewBuilder.() -> Unit) : super(parent, function)

    fun hasUrl(url: String) {
        view.check { view, noViewFoundException ->
            if (view is WebView) {
                if (view.testUrl != url && view.url != url) {
                    throw AssertionError(
                        "The WebView URL is expected to be $url but has " +
                                "original url ${view.url} and test url ${view.testUrl}"
                    )
                }
            } else if (view != null) {
                throw AssertionError("The view is expected to be WebView but is ${view::class}")
            } else {
                throw noViewFoundException
            }
        }
    }

    fun hasPartUrl(partUrl: String) {
        view.check { view, noViewFoundException ->
            if (view is WebView) {
                if (view.testUrl?.contains(partUrl) != true && view.url?.contains(partUrl) != true) {
                    throw AssertionError(
                        "The WebView URL is expected to be $partUrl but has " +
                                "original url ${view.url} and test url ${view.testUrl}"
                    )
                }
            } else if (view != null) {
                throw AssertionError("The view is expected to be WebView but is ${view::class}")
            } else {
                throw noViewFoundException
            }
        }
    }
}