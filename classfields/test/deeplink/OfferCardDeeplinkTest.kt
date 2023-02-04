package ru.auto.ara.test.deeplink

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.core.dispatchers.offer_card.GetOfferDispatcher
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.routing.delegateDispatchers
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity

@RunWith(Parameterized::class)
class OfferCardDeeplinkTest(val parameters: TestParameter) {

    private val webServerRule = WebServerRule {
        delegateDispatchers(
            GetOfferDispatcher.getOffer(category = parameters.category, offerId = parameters.offerId)
        )
    }
    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule()
    )

    @Test
    fun shouldOpenDeeplink() {
        activityTestRule.launchDeepLinkActivity(parameters.uri)
        performOfferCard().checkResult {
            isOfferCardTitle(parameters.title)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> =
            listOf(
                TestParameter(
                    "https://auto.ru/cars/used/sale/daewoo/lanos/1077957027-c7abdb2f/",
                    "cars",
                    "1077957027-c7abdb2f",
                    "Daewoo Lanos, 2008"
                ),
                TestParameter(
                    "http://auto.ru/cars/used/sale/daewoo/lanos/1077957027-c7abdb2f/",
                    "cars",
                    "1077957027-c7abdb2f",
                    "Daewoo Lanos, 2008"
                ),
                TestParameter(
                    "autoru://app/cars/used/sale/daewoo/lanos/1077957027-c7abdb2f/",
                    "cars",
                    "1077957027-c7abdb2f",
                    "Daewoo Lanos, 2008"
                ),
                TestParameter(
                    "https://auto.ru/mototsikly/used/sale/ural/8103_1/2649110-e6586f9b/",
                    "moto",
                    "2649110-e6586f9b",
                    "Урал ИМЗ-8.103, 1988"
                ),
                TestParameter(
                    "https://auto.ru/skutery/used/sale/honda/gyro_x/2651406-adf0e32d/",
                    "moto",
                    "2651406-adf0e32d",
                    "Honda Gyro, 2008"
                ),
                TestParameter(
                    "https://auto.ru/atv/used/sale/stels/atv_600_leopard/1894128-d229/",
                    "moto",
                    "1894128-d229",
                    "Stels ATV-600YS Leopard, 2018"
                ),
                TestParameter(
                    "https://auto.ru/snegohody/new/sale/polaris/widetrack_iq/2621880-1fa7f96f/",
                    "moto",
                    "2621880-1fa7f96f",
                    "Polaris Widetrak IQ, 2015"
                ),
                TestParameter(
                    "https://auto.ru/scooters/used/sale/honda/gyro_x/2651406-adf0e32d/",
                    "moto",
                    "2651406-adf0e32d",
                    "Honda Gyro, 2008"
                ),
                TestParameter(
                    uri = "https://auto.ru/amp/motorcycle/used/sale/suzuki/gsx_1400/2651406-adf0e32d/",
                    category = "moto",
                    "2651406-adf0e32d",
                    "Honda Gyro, 2008"
                ),
                TestParameter(
                    "https://auto.ru/legkie-gruzoviki/used/sale/ford/transit_lt/10558958-6e6d1683/",
                    "trucks",
                    "10558958-6e6d1683",
                    "Ford Transit, 2013"
                ),
                TestParameter(
                    "https://auto.ru/trucks/used/sale/fuso/supergreat/10629246-d59efd2d/",
                    "trucks",
                    "10629246-d59efd2d",
                    "Mitsubishi Fuso Super Great, 2001"
                ),
                TestParameter(
                    "https://auto.ru/artic/used/sale/scania/r_1/15572594-d127878a/",
                    "trucks",
                    "15572594-d127878a",
                    "Scania R-Series, 2015"
                ),
                TestParameter(
                    "https://auto.ru/bus/used/sale/hyundai/universe/10448426-ce654669/",
                    "trucks",
                    "10448426-ce654669",
                    "Hyundai Universe, 2019"
                ),
                TestParameter(
                    "https://auto.ru/drags/used/sale/kmz/8119/15673892-50417541/",
                    "trucks",
                    "15673892-50417541",
                    "КМЗ 8119, 1990"
                ),
                TestParameter(
                    "https://auto.ru/light_trucks/new/sale/ford/transit_lt/10367272-2aff29c1/",
                    "trucks",
                    "10367272-2aff29c1",
                    "Ford Transit, 2018"
                ),
                TestParameter(
                    "https://auto.ru/trucks/used/sale/fuso/supergreat/10629246-d59efd2d/",
                    "trucks",
                    "10629246-d59efd2d",
                    "Mitsubishi Fuso Super Great, 2001"
                ),
                TestParameter(
                    "https://auto.ru/lcv/used/sale/ford/transit_lt/10558958-6e6d1683/",
                    "trucks",
                    "10558958-6e6d1683",
                    "Ford Transit, 2013"
                ),
                TestParameter(
                    "https://auto.ru/truck/used/sale/fuso/supergreat/10629246-d59efd2d/",
                    "trucks",
                    "10629246-d59efd2d",
                    "Mitsubishi Fuso Super Great, 2001"
                ),
                TestParameter(
                    "https://auto.ru/trailer/used/sale/kmz/8119/15673892-50417541/",
                    "trucks",
                    "15673892-50417541",
                    "КМЗ 8119, 1990"
                ),
                TestParameter(
                    "https://auto.ru/agricultural/used/sale/xingtai/xt_s/16049730-a933cc9a/",
                    "trucks",
                    "16049730-a933cc9a",
                    "Xingtai XT, 2014"
                ),
                TestParameter(
                    "https://auto.ru/construction/used/sale/vogele/super/16079424-564c9079/",
                    "trucks",
                    "16079424-564c9079",
                    "Vogele Super, 2008"
                ),
                TestParameter(
                    "https://auto.ru/autoloader/new/sale/sdlg/lg_946/15968098-bf039c5b/",
                    "trucks",
                    "15968098-bf039c5b",
                    "SDLG LG 946, 2019"
                ),
                TestParameter(
                    "https://auto.ru/bulldozers/used/sale/mtz/mtz_bulldozers/16102236-cd53745c/",
                    "trucks",
                    "16102236-cd53745c",
                    "МТЗ МТЗ, 1996"
                ),
                TestParameter(
                    "https://auto.ru/dredge/used/sale/doosan/solar/16084200-115339fe/",
                    "trucks",
                    "16084200-115339fe",
                    "Doosan Solar, 2012"
                ),
                TestParameter(
                    "https://auto.ru/crane/new/sale/ivanovets/ks_45717_crane/15848548-25cd4821/",
                    "trucks",
                    "15848548-25cd4821",
                    "Ивановец КС-45717, 2019"
                ),
                TestParameter(
                    "https://auto.ru/municipal/used/sale/gaz/gazon_next_m/16135706-57186006/",
                    "trucks",
                    "16135706-57186006",
                    "ГАЗ ГАЗон Next, 2017"
                ),
                TestParameter(
                    "https://auto.ru/bus/used/sale/mersedes/sprinter_bus/15850592-5b41e424/",
                    "trucks",
                    "15850592-5b41e424",
                    "Mercedes-Benz Sprinter, 2020"
                ),
            ).map { arrayOf(it) }

        data class TestParameter(
            val uri: String,
            val category: String,
            val offerId: String,
            val title: String
        ) {
            override fun toString(): String = offerId
        }
    }
}
