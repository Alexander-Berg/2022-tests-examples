package ru.auto.ara.test.sale

import androidx.test.core.app.launchActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.SplashActivity
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.sale.SaleDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user_offers.GetUserOffersDispatcher
import ru.auto.ara.core.robot.sale.checkSale
import ru.auto.ara.core.robot.sale.performSale
import ru.auto.ara.core.robot.transporttab.checkMain
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule

class SaleForMoreThenOneOfferTest {

    private val dispatcherHolder = DispatcherHolder()

    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            GetUserOffersDispatcher.saleManyActiveOffers(),
            dispatcherHolder
        )
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        SetupAuthRule(),
        SetupTimeRule(date = "01.01.2020", time = "12:00"),
    )

    @Before
    fun setup() {
        dispatcherHolder.innerDispatcher = SaleDispatcher.moreThenOneOffer()
    }

    @Test
    fun shouldOpenSaleOnStartApp() {
        launchActivity<SplashActivity>().use {
            checkSale {
                isCorrectTitleDisplayed(SALE_TITLE)
                isCorrectDescriptionDisplayed(SALE_DESCRIPTION)
                isCloseIconDisplayed()

                isSalePromoImageDisplayed()
                isSalePromoButtonDisplayed(SALE_BUTTON_TEXT)
            }
        }
    }

    @Test
    fun shouldOpenSaleFromUserOffersScreen() {
        launchActivity<MainActivity>().use {
            performMain { openLowTab(R.string.offers) }
            performSale { clickOnSalePreviewItem() }
            checkSale {
                isCorrectTitleDisplayed(SALE_TITLE)
                isCorrectDescriptionDisplayed(SALE_DESCRIPTION)
                isCloseIconDisplayed()
                isSalePromoImageDisplayed()
                isSalePromoButtonDisplayed(SALE_BUTTON_TEXT)
            }
            performSale { clickOnCloseIcon() }
        }
    }

    @Test
    fun shouldOpenUserOfferScreenAfterClickOnPromoButton() {
        launchActivity<SplashActivity>().use {
            performSale { clickOnPromoButton(SALE_BUTTON_TEXT) }
            checkMain { isLowTabSelected(USER_OFFERS) }
        }
    }

    @Test
    fun shouldCloseSaleAfterClickOnCloseIcon() {
        launchActivity<SplashActivity>().use {
            performSale { clickOnCloseIcon() }
            checkMain { isLowTabSelected(SEARCH) }
        }
    }

    companion object {
        const val SEARCH = R.string.search
        const val USER_OFFERS = R.string.offers
        private const val DISCOUNT = 70
        const val SALE_TITLE = "Скидка до $DISCOUNT%"
        const val SALE_DESCRIPTION = "До окончания акции"
        const val SALE_BUTTON_TEXT = "Начать покупки"
    }
}
