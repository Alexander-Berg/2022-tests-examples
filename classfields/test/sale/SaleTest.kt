package ru.auto.ara.test.sale

import androidx.test.core.app.launchActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.SplashActivity
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.sale.SaleDispatcher
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.dispatchers.user_offers.GetUserOffersDispatcher
import ru.auto.ara.core.robot.sale.checkSale
import ru.auto.ara.core.robot.sale.performSale
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule

@RunWith(AndroidJUnit4::class)
class SaleTest {
    private val saleDispatcherHolder = DispatcherHolder()
    private val userOfferDispatcherHolder = DispatcherHolder()

    private val webServerRule = WebServerRule {
        userSetup()
        delegateDispatchers(
            userOfferDispatcherHolder,
            saleDispatcherHolder
        )
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        SetupAuthRule(),
        SetupTimeRule(date = "01.01.2020", time = "12:00"),
    )

    @Test
    fun shouldSaleNotAvailableIfVIPBuying() {
        saleDispatcherHolder.innerDispatcher = SaleDispatcher.saleOfferWhenVipBuying()
        userOfferDispatcherHolder.innerDispatcher = GetUserOffersDispatcher.offerWithBuyingVip()
        launchActivity<SplashActivity>().use {
            checkSale { isSaleDialogAreNotDisplayed() }
            performMain { openLowTab(R.string.offers) }
            checkSale { isSaleSmallFieldIsNotExists() }
        }
    }

    @Test
    fun shouldDoNotShowVasIfOfferNotSupportTheir() {
        saleDispatcherHolder.innerDispatcher = SaleDispatcher.saleWithVasWhichOfferDoNotSupport()
        userOfferDispatcherHolder.innerDispatcher = GetUserOffersDispatcher.saleOffer()
        launchActivity<MainActivity>().use {
            performMain { openLowTab(R.string.offers) }
            performSale { clickOnSalePreviewItem() }

            AVAILABLE_VAS_LIST.forEach { title ->
                performSale { scrollToVasItem(title) }
                checkSale { isCorrectCollapseVasTitle(title) }
            }
            NOT_AVAILABLE_VAS_LIST.forEach { title ->
                checkSale { isCollapseVasTitleNotExists(title) }
            }
        }
    }

    companion object {
        private val AVAILABLE_VAS_LIST = listOf(
            "Спецпредложение",
            "Выделение цветом"
        )
        private val NOT_AVAILABLE_VAS_LIST = listOf("Экспресс продажа")
    }
}
