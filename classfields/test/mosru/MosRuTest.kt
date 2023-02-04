package com.yandex.mobile.realty.test.mosru

import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.PatternMatcher
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.platform.app.InstrumentationRegistry
import com.yandex.mobile.realty.core.interaction.NamedIntents
import com.yandex.mobile.realty.core.viewMatchers.NamedIntentMatcher
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import java.net.URLEncoder

/**
 * @author andrey-bgm on 17/09/2020.
 */
private const val MOS_RU_DEEPLINK = "yandexrealty://realty.yandex.ru/mosruauth"
private const val TASK_ID = "32f14065-0ec8-4261-925b-ab6862481796"
private const val BINDING_URL = "https://test.social.yandex.ru/mosru?retpath=$MOS_RU_DEEPLINK"
private val BINDING_URL_WITH_LOGOUT =
    "https://test.mos.ru/logout?redirect=${URLEncoder.encode(BINDING_URL, "UTF-8")}"

fun prepareMosRuBinding() {
    prepareBinding("test.social.yandex.ru", "mosru")
}

fun prepareMosRuBindingWithLogout() {
    prepareBinding("test.mos.ru", "logout")
}

fun intendedOpenMosRu() {
    intendedOpenBrowser("запуск браузера для логина в mos.ru", BINDING_URL)
}

fun intendedOpenMosRuWithLogout() {
    intendedOpenBrowser("запуск браузера для логина в mos.ru с логаутом", BINDING_URL_WITH_LOGOUT)
}

fun matchesOpenInBrowserMosRuHelpIntent(): Matcher<Intent> {
    val mosRuHelpUrl =
        "https://www.mos.ru/otvet-tehnologii/kak-polzovatsya-lichnym-kabinetom-na-mos-ru"

    return NamedIntentMatcher(
        "запуск браузера по адресу $mosRuHelpUrl",
        allOf(
            hasAction(Intent.ACTION_VIEW),
            hasData(mosRuHelpUrl)
        )
    )
}

fun matchesOpenInBrowserYandexHelpIntent(): Matcher<Intent> {
    val yandexHelpUrl = "https://yandex.ru/support/realty/mosru.html"

    return NamedIntentMatcher(
        "запуск браузера по адресу $yandexHelpUrl",
        allOf(
            hasAction(Intent.ACTION_VIEW),
            hasData(yandexHelpUrl)
        )
    )
}

private fun prepareBinding(host: String, path: String) {
    InstrumentationRegistry.getInstrumentation().addMonitor(
        IntentFilter(Intent.ACTION_VIEW).apply {
            addDataScheme("https")
            addDataAuthority(host, null)
            addDataPath("/$path", PatternMatcher.PATTERN_PREFIX)
        },
        null,
        true
    )
}

private fun intendedOpenBrowser(name: String, url: String) {
    NamedIntents.intended(
        NamedIntentMatcher(
            name,
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(url)
            )
        )
    )

    val uri = Uri.parse("$MOS_RU_DEEPLINK?task_id=$TASK_ID")
    InstrumentationRegistry.getInstrumentation().context.startActivity(
        Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )
}

fun DispatcherRegistry.registerMosRuBindingUrl() {
    register(
        request {
            method("GET")
            path("2.0/user/redirect/mosru")
            queryParam("retpath", MOS_RU_DEEPLINK)
        },
        response {
            setBody(
                """
                            {
                                "response": {
                                    "url": "$BINDING_URL",
                                    "urlLogout": "$BINDING_URL_WITH_LOGOUT"
                                }
                            }
                """.trimIndent()
            )
        }
    )
}

fun DispatcherRegistry.registerMosRuBindingUrlError() {
    register(
        request {
            method("GET")
            path("2.0/user/redirect/mosru")
            queryParam("retpath", MOS_RU_DEEPLINK)
        },
        response {
            setResponseCode(500)
        }
    )
}

fun DispatcherRegistry.registerMosRuSubmitTaskId() {
    register(
        request {
            method("POST")
            path("2.0/user/me/callback/mosru")
            queryParam("task_id", TASK_ID)
        },
        response {
            setResponseCode(200)
        }
    )
}

fun DispatcherRegistry.registerMosRuSubmitTaskIdError() {
    register(
        request {
            method("POST")
            path("2.0/user/me/callback/mosru")
            queryParam("task_id", TASK_ID)
        },
        response {
            setResponseCode(500)
        }
    )
}

fun DispatcherRegistry.registerMosRuUnbind() {
    register(
        request {
            method("PUT")
            path("2.0/user/me/unlink/mosru")
        },
        response {
            setResponseCode(200)
        }
    )
}
