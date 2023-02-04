package ru.auto.ara.test.offer

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.core.dispatchers.DispatcherHolder
import ru.auto.ara.core.dispatchers.offer_card.GetOfferTemplateDispatcher
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.tax.performTransportTax
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupTimeRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchOfferDetailsActivity
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.ui.activity.OfferDetailsActivity
import ru.auto.data.model.catalog.EngineType
import ru.auto.data.model.data.offer.CAR
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TransportTaxTest {

    private val templateDispatcherHolder = DispatcherHolder()
    private val dispatchers = listOf(templateDispatcherHolder)

    private val activityTestRule = lazyActivityScenarioRule<OfferDetailsActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        WebServerRule { delegateDispatchers(dispatchers) },
        DisableAdsRule(),
        SetPreferencesRule(),
        SetupTimeRule(1546304461000), // 01.01.19
        activityTestRule
    )

    @Test
    fun shouldShowNewYearNoticeIfThereIsPrevYear() {
        test(
            offerId = TEMPLATE_OFFER_ID,
            tax = 18675,
            year = 2018,
            price = "18 675 \u20BD в год",
            hint = "Налог рассчитан для двигателя мощностью 109 л.с. в Москве по тарифу 2018 года. " +
                "Правительство РФ пока не утвердило налоговую базу 2019 года."
        )
    }

    @Test
    fun shouldShowNewYearAndBoostNoticesIfThereIsPrevYear() {
        test(
            offerId = TEMPLATE_OFFER_ID,
            tax = 2875,
            year = 2018,
            boost = "1.1",
            price = "2 875 \u20BD в год",
            hint = "Налог рассчитан для двигателя мощностью 109 л.с. в Москве по тарифу 2018 года " +
                "с учётом налога на роскошь. Правительство РФ пока не утвердило налоговую базу 2019 года."
        )
    }

    @Test
    fun shouldNotShowNewYearNoticeIfThereIsCurrentYear() {
        test(
            offerId = TEMPLATE_OFFER_ID,
            tax = 2650,
            year = 2019,
            boost = "1.1",
            price = "2 650 \u20BD в год",
            hint = "Налог рассчитан для двигателя мощностью 109 л.с. в Москве по тарифу 2019 года с учётом налога на роскошь."
        )
    }

    @Test
    fun shouldNotShowNewYearNoticeIfThereIsNextYear() {
        test(
            offerId = TEMPLATE_OFFER_ID,
            tax = 40350,
            year = 2020,
            price = "40 350 \u20BD в год",
            hint = "Налог рассчитан для двигателя мощностью 109 л.с. в Москве по тарифу 2020 года."
        )
    }

    @Test
    fun shouldShowExemptionTaxNoticeIfElectroAnd0Tax() {
        test(
            offerId = TEMPLATE_OFFER_ID,
            tax = 0,
            price = "0 \u20BD в год",
            engineType = EngineType.ELECTRO,
            year = 2019,
            hint = "Налог рассчитан для двигателя мощностью 109 л.с. в Москве по тарифу 2019 года " +
                "с учётом льгот для электромобилей."
        )
    }

    @Test
    fun shouldNotShowBoostIfHasElectrocarExemptionNotice() {
        test(
            offerId = TEMPLATE_OFFER_ID,
            boost = "1.2",
            engineType = EngineType.ELECTRO,
            year = 2019,
            tax = 0,
            price = "0 \u20BD в год",
            hint = "Налог рассчитан для двигателя мощностью 109 л.с. в Москве по тарифу 2019 года " +
                "с учётом льгот для электромобилей."
        )
    }

    @Test
    fun shouldShowElectroExemtionAndNewYearNoticeIfPrevYear() {
        test(
            offerId = TEMPLATE_OFFER_ID,
            boost = "1.2",
            engineType = EngineType.ELECTRO,
            tax = 0,
            year = 2018,
            price = "0 \u20BD в год",
            hint = "Налог рассчитан для двигателя мощностью 109 л.с. в Москве по тарифу 2018 года " +
                "с учётом льгот для электромобилей. Правительство РФ пока не утвердило налоговую базу 2019 года."
        )
    }

    @Test
    fun shouldNotShowExemptionTaxNoticeIfNotElectroAnd0Tax() {
        test(
            offerId = TEMPLATE_OFFER_ID,
            tax = 0,
            year = 2018,
            price = "0 \u20BD в год",
            hint = "Налог рассчитан для двигателя мощностью 109 л.с. в Москве по тарифу 2018 года. " +
                "Правительство РФ пока не утвердило налоговую базу 2019 года."
        )
    }

    @Test
    fun shouldNotShowExemptionTaxNoticeIfElectroAndNonZeroTax() {
        test(
            offerId = TEMPLATE_OFFER_ID,
            engineType = EngineType.ELECTRO,
            tax = 2650,
            year = 2018,
            price = "2 650 \u20BD в год",
            hint = "Налог рассчитан для двигателя мощностью 109 л.с. в Москве по тарифу 2018 года. " +
                "Правительство РФ пока не утвердило налоговую базу 2019 года."
        )
    }


    private fun test(
        offerId: String,
        price: String,
        hint: String,
        engineType: EngineType = EngineType.GASOLINE,
        tax: Int = 4000,
        year: Int = 2019,
        boost: String = "1.0"
    ) {
        val offerTemplateDispatcher = GetOfferTemplateDispatcher.getOfferWithTax(
            CAR,
            TEMPLATE_OFFER_ID,
            tax = tax,
            boost = boost,
            engine = engineType,
            year = year
        )
        templateDispatcherHolder.innerDispatcher = offerTemplateDispatcher
        activityTestRule.launchOfferDetailsActivity(category = CAR, offerId = offerId)
        performOfferCard {
            collapseAppBar()
            scrollToAllParameters()
            waitSomething(1, TimeUnit.SECONDS) // wait for recycler to stop scrolling
            clickByTax()
        }

        performTransportTax()
            .checkResult {
                isTitleDisplayed()
                isContentDisplayed(price, hint)
            }
    }

    companion object {
        private const val TEMPLATE_OFFER_ID = "1077981475-96e6d"
    }
}
