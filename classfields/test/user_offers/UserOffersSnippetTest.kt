package ru.auto.ara.test.user_offers

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.draft.Draft
import ru.auto.ara.core.dispatchers.draft.Draft.DEFAULT
import ru.auto.ara.core.dispatchers.draft.deleteCurrentDraft
import ru.auto.ara.core.dispatchers.draft.getCurrentDraft
import ru.auto.ara.core.dispatchers.draft.getDraft
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user_offers.GetUserOffersDispatcher
import ru.auto.ara.core.robot.full_draft.checkFullDraft
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.robot.useroffers.checkOffers
import ru.auto.ara.core.robot.useroffers.performOffers
import ru.auto.ara.core.robot.webview.checkWebView
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule

@RunWith(AndroidJUnit4::class)
class UserOffersSnippetTest {

    private val activityTestRule = lazyActivityScenarioRule<MainActivity>()

    private val userOffersDispatcherHolder = DispatcherHolder()

    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            userOffersDispatcherHolder
        )
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        SetupTimeRule(date = "10.12.2019"),
        SetupAuthRule()
    )

    @Test
    fun shouldSeeActiveOfferSnippet() {
        openLK {
            userOffersDispatcherHolder.innerDispatcher = GetUserOffersDispatcher.active()
        }

        performOffers {
            waitForOfferSnippets(count = 1)
        }.checkResult {
            isActiveSnippetDisplayed(
                index = 0,
                title = "BMW 3 серия 330e VI (F3x) Рестайлинг, 2019",
                price = "1,887,000 \u20BD",
                viewsCount = "18 / 9 за неделю",
                favoritesCount = "10 сохранили",
                searchPlace = "16 в поиске",
                countDown = "3 д. до снятия"
            )
        }
    }

    @Test
    fun shouldSeeInactiveOfferSnippet() {
        openLK {
            setupInactiveOffers()
        }

        performOffers {
            waitForOfferSnippets(count = 1)
        }.checkResult {
            isInactiveSnippetDisplayed(
                index = 0,
                title = "Audi A5 II (F5), 2018",
                price = "2,591,000 \u20BD",
                status = "Неактивно",
                updated = "Обновлено 61 день назад",
                paidReason = R.string.vas_desc_activate_payment_group
            )
        }
    }

    @Test
    fun shouldSeeBannedEditableOfferSnippet() {
        openLK {
            userOffersDispatcherHolder.innerDispatcher = GetUserOffersDispatcher.bannedEditable()
        }

        val checkedSnippetIndex = 0

        performOffers {
            setUserOffersToolbarExpanded(isExpanded = false)
            waitForOfferSnippets(count = 1)
        }.checkResult {
            isBannedEditableSnippetDisplayed(
                index = checkedSnippetIndex,
                title = "Daewoo Prima, 2012",
                price = "45,555 \u20BD",
                status = "Удалено\nмодератором",
                banReasonCount = 1
            )
        }

        performOffers {
            scrollToBanReasonItemTitleOnOfferSnippet(checkedSnippetIndex, banReasonIndex = 0)
        }.checkResult {
            isBanReasonItemDisplayed(
                index = checkedSnippetIndex,
                banReasonItemIndex = 0,
                banReasonItemTitle = "Не рестайлинг",
                banReasonItemText = "Исправьте поколение автомобиля на не рестайлинг. " +
                        "Если сомневаетесь, какое выбрать, сверьтесь с каталогом Авто.ру."
            )
        }

        performOffers {
            scrollToLeftButtonOnOfferSnippet(checkedSnippetIndex)
        }.checkResult {
            isOfferActionOnBannedEditableSnippetDisplayed(checkedSnippetIndex)
        }

        performOffers {
            scrollToMoreButtonOnOfferSnippet(checkedSnippetIndex)
            clickOnBannedSnippetMenuIcon(checkedSnippetIndex)
        }.checkResult {
            isBannedEditableMenuDisplayed()
        }
    }

    @Test
    fun shouldSeeBannedUneditableOfferSnippet() {
        openLK {
            userOffersDispatcherHolder.innerDispatcher = GetUserOffersDispatcher.bannedUneditable()
        }

        val checkedSnippetIndex = 0

        performOffers {
            setUserOffersToolbarExpanded(isExpanded = false)
            waitForOfferSnippets(count = 1)
        }.checkResult {
            isBannedUneditableSnippetDisplayed(
                index = checkedSnippetIndex,
                title = "Volvo EC210, 2012",
                price = "255,555 \u20BD",
                status = "Удалено\nмодератором",
                banReasonCount = 2
            )
        }

        performOffers {
            scrollToBanReasonItemTitleOnOfferSnippet(checkedSnippetIndex, banReasonIndex = 0)
        }.checkResult {
            isBanReasonItemDisplayed(
                index = checkedSnippetIndex,
                banReasonItemIndex = 0,
                banReasonItemTitle = "Неверный раздел",
                banReasonItemText = "Продавать новые автомобили на Авто.ру могут только дилеры."
            )
        }

        performOffers {
            scrollToBanReasonItemTitleOnOfferSnippet(checkedSnippetIndex, banReasonIndex = 1)
        }.checkResult {
            isBanReasonItemDisplayed(
                index = checkedSnippetIndex,
                banReasonItemIndex = 1,
                banReasonItemTitle = "Этот автомобиль уже продается на Авто.ру",
                banReasonItemText = null
            )
        }

        performOffers {
            scrollToLeftButtonOnOfferSnippet(checkedSnippetIndex)
        }.checkResult {
            isOfferActionOnBannedUneditableSnippetDisplayed(checkedSnippetIndex)
        }

        performOffers {
            scrollToMoreButtonOnOfferSnippet(checkedSnippetIndex)
            clickOnBannedSnippetMenuIcon(checkedSnippetIndex)
        }.checkResult {
            isBannedUneditableMenuDisplayed()
        }
    }

    // This test is not completed. Add more checks within https://st.yandex-team.ru/AUTORUAPPS-12023
    @Test
    fun shouldOpenWebViewByBanReasonClick() {
        openLK {
            userOffersDispatcherHolder.innerDispatcher = GetUserOffersDispatcher.bannedEditable()
        }

        performOffers {
            setUserOffersToolbarExpanded(isExpanded = false)
            waitForOfferSnippets(count = 1)
            scrollToBanReasonItemTitleOnOfferSnippet(index = 0, banReasonIndex = 0)
            clickOnSnippetBanReasonItemText(
                index = 0,
                banReasonIndex = 0,
                clickableText = "каталогом Авто.ру"
            )
        }

        checkWebView { isWebViewToolBarDisplayed(text = "Каталог") }
    }

    @Test
    fun shouldShowDraftAsFirstSnippetIfUserHasCarDraft() {
        openLKNoOffers {
            setupGetDraft()
        }

        checkOffers { checkDraftOfferSnippetScreenshot() }
    }

    @Test
    fun shouldOpenDraftEditWhenClickOnSnippet() {
        openLKNoOffers { setupGetDraft() }
        performOffers { clickOfferSnippet(0) }
        checkFullDraft {
            checkDraftDisplayed()
            checkClearButtonIsDisplayed()
        }
    }

    @Test
    fun shouldOpenDraftEditWhenClickOnEditButton() {
        openLKNoOffers { setupGetDraft() }
        performOffers { clickOnEditButtonOnOfferSnippet(0) }
        checkFullDraft {
            checkDraftDisplayed()
            checkClearButtonIsDisplayed()
        }
    }

    @Test
    fun shouldDeleteDraftWhenClickOnDeleteButton() {
        openLKNoOffers {
            setupGetDraft()
        }
        webServerRule.routing {
            deleteCurrentDraft()
            getCurrentDraft(draft = DEFAULT.copy(offer = Draft.Offer.DEFAULT.copy(car_info = null)))
        }
        performOffers { clickOnDeleteButtonOnDraftSnippet() }
        checkOffers { isEmptyAuthorized() }
    }

    private fun setupGetDraft() {
        val draftId = "2085703703405514745-14faedd7"
        webServerRule.routing { getDraft(null, fileName = draftId) }
    }

    private fun setupInactiveOffers() {
        userOffersDispatcherHolder.innerDispatcher = GetUserOffersDispatcher.inactive()
    }

    private fun openLK(setup: () -> Unit) {
        setup()

        activityTestRule.launchActivity()

        performMain {
            openLowTab(R.string.offers)
        }.checkResult {
            isLowTabSelected(R.string.offers)
        }
    }

    private fun openLKNoOffers(setup: () -> Unit) {
        setup()

        activityTestRule.launchActivity()

        performMain {
            openLowTab(R.string.add_offer)
        }.checkResult {
            isLowTabSelected(R.string.add_offer)
        }
    }

}
