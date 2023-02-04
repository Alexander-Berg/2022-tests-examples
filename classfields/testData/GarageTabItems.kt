package ru.auto.ara.core.testdata

import androidx.compose.ui.test.junit4.ComposeTestRule
import ru.auto.ara.R
import ru.auto.ara.core.robot.othertab.aboutapplication.checkAbout
import ru.auto.ara.core.robot.othertab.checkEvaluate
import ru.auto.ara.core.robot.othertab.checkNotifications
import ru.auto.ara.core.robot.userreviews.checkUserReviewsFeed

class SettingsTabNativeItem(
    val itemNameRes: Int,
    val check: (composeTestRule: ComposeTestRule) -> Unit,
)

class GarageTabWebviewItem(
    val itemNameRes: Int,
    val uri: String,
)

val AUTH_GARAGE_TAB_NATIVE_ITEMS: Array<SettingsTabNativeItem> = arrayOf(
    SettingsTabNativeItem(R.string.my_reviews_title, { checkUserReviewsFeed { isTitleDisplayed() } }),
    SettingsTabNativeItem(R.string.evaluate_action, { checkEvaluate { isEvaluate() } }),
    SettingsTabNativeItem(R.string.notifications, { checkNotifications { isNotifications() } }),
    SettingsTabNativeItem(R.string.settings_about, { it.checkAbout { isAbout() } })
)

val GARAGE_TAB_WEBVIEW_ITEMS: Array<GarageTabWebviewItem> = arrayOf(
    GarageTabWebviewItem(R.string.card_catalog, "https://auto.ru/catalog"),
    GarageTabWebviewItem(R.string.journal, "https://mag.auto.ru/"),
    GarageTabWebviewItem(R.string.manual_title, "https://mag.auto.ru/theme/uchebnik"),
    GarageTabWebviewItem(R.string.video, "https://auto.ru/video/?only-content=true"),
    GarageTabWebviewItem(R.string.core_help, "https://yandex.ru/support/autoru-app-android/"),
    GarageTabWebviewItem(R.string.about_license_agreement, "https://yandex.ru/legal/autoru_mobile_license/"),
    GarageTabWebviewItem(R.string.privacy_policy, "https://yandex.ru/legal/confidential/?lang=ru"),
)
