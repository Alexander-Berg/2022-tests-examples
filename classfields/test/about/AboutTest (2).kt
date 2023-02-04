package com.yandex.mobile.realty.test.about

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.yandex.mobile.realty.activity.NamedActivityTestRule
import com.yandex.mobile.realty.activity.SettingsActivity
import com.yandex.mobile.realty.core.rule.baseChainOf
import com.yandex.mobile.realty.core.screen.AboutScreen
import com.yandex.mobile.realty.core.screen.Screen.Companion.onScreen
import com.yandex.mobile.realty.core.screen.SettingsScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * @author scrooge on 22.04.2019.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class AboutTest {

    @JvmField
    @Rule
    var ruleChain = baseChainOf(
        NamedActivityTestRule("Настройки", SettingsActivity::class.java)
    )

    @Test
    fun shouldShowToolbarTitle() {
        onScreen<SettingsScreen> {
            listView.scrollTo(aboutItem).click()
        }

        onScreen<AboutScreen> {
            toolbarTitle.isCompletelyDisplayed()
        }
    }
}
