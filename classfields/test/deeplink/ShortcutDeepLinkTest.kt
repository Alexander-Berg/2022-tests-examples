package com.yandex.mobile.realty.test.deeplink

import androidx.annotation.StringRes
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.yandex.mobile.realty.R
import com.yandex.mobile.realty.allure.step
import com.yandex.mobile.realty.core.longClick
import com.yandex.mobile.realty.core.pressHome
import com.yandex.mobile.realty.core.screen.CallHistoryScreen
import com.yandex.mobile.realty.core.screen.FavoriteScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.UserOffersScreen
import com.yandex.mobile.realty.core.uiDevice
import com.yandex.mobile.realty.core.viewMatchers.getResourceString
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author merionkov on 13/01/2021.
 */
@RunWith(AndroidJUnit4::class)
class ShortcutDeepLinkTest {

    @Test
    fun shouldOpenSubscriptions() {
        clickOnShortcut(R.string.shortcut_subscriptions)
        onScreen<FavoriteScreen> {
            subscriptionsTabView.waitUntil { isCompletelyDisplayed() }.isSelected()
        }
    }

    @Test
    fun shouldOpenUserOffers() {
        clickOnShortcut(R.string.shortcut_user_offers)
        onScreen<UserOffersScreen> {
            contentView.waitUntil { isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldOpenCalls() {
        clickOnShortcut(R.string.shortcut_calls)
        onScreen<CallHistoryScreen> {
            emptyView.waitUntil { isCompletelyDisplayed() }
        }
    }

    @Test
    fun shouldOpenFavoriteOffers() {
        clickOnShortcut(R.string.shortcut_favoite_offers)
        onScreen<FavoriteScreen> {
            favoriteOffersTabView.waitUntil { isCompletelyDisplayed() }.isSelected()
        }
    }

    private fun clickOnShortcut(@StringRes shortcutNameRes: Int) {
        pressHome()
        step("Открываем список приложений") {
            uiDevice.swipe(
                uiDevice.displayWidth / 2,
                uiDevice.displayHeight * 2 / 3,
                uiDevice.displayWidth / 2,
                uiDevice.displayHeight * 1 / 3,
                APPS_LIST_SWIPE_STEPS,
            )
        }
        step("Лонг тап на иконку приложения") {
            longClick(getResourceString(R.string.app_name_short), APP_LONG_CLICK_STEPS)
        }
        val shortcutName = getResourceString(shortcutNameRes)
        step("Клик на шорткат \"$shortcutName\"") {
            uiDevice.wait(Until.findObject(By.text(shortcutName)), 5000).click()
        }
    }

    private companion object {
        const val APPS_LIST_SWIPE_STEPS = 10
        const val APP_LONG_CLICK_STEPS = 100
    }
}
