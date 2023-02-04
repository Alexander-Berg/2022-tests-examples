package ru.auto.ara.test.offer.specials_related

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ru.auto.ara.R
import ru.auto.ara.core.TestGeoRepository
import ru.auto.ara.core.actions.ViewActions.setAppBarExpandedState
import ru.auto.ara.core.dispatchers.device.copyWithGeoUfa
import ru.auto.ara.core.dispatchers.device.getParsedDeeplink
import ru.auto.ara.core.dispatchers.geo.getGeoSuggest
import ru.auto.ara.core.dispatchers.offer_card.getOffer
import ru.auto.ara.core.dispatchers.offer_card.getOfferSpecials
import ru.auto.ara.core.dispatchers.search_offers.OfferLocatorCountersResponse
import ru.auto.ara.core.dispatchers.search_offers.getOfferLocatorCounters
import ru.auto.ara.core.dispatchers.search_offers.getOfferRelated
import ru.auto.ara.core.dispatchers.search_offers.postSearchOffers
import ru.auto.ara.core.robot.offercard.checkOfferCard
import ru.auto.ara.core.robot.offercard.performOfferCard
import ru.auto.ara.core.robot.searchfeed.performListingOffers
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.di.GeoSuggestRule
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.core.utils.pressBack
import ru.auto.ara.core.utils.waitSomething
import ru.auto.ara.deeplink.DeeplinkActivity
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SpecialsRelatedTest {
    private val PAGE_SIZE_PARAM = "page_size"
    private val RID_PARAM = "rid"
    private val CAR_USED_URI = "https://auto.ru/cars/used/sale/1082957054-8d55bf9a"
    private val CAR_NEW_URI = "https://auto.ru/cars/new/sale/1084250931-f8070529"
    private val CAR_USED_ID = "1082957054-8d55bf9a"
    private val CAR_NEW_ID = "1084250931-f8070529"
    private val DEFAULT_REGION_NAME = "ufa"
    private val GEO_RADIUS_PARAM = "geo_radius"

    private val activityTestRule = lazyActivityScenarioRule<DeeplinkActivity>()

    private val geoArgs = TestGeoRepository.GeoArgs.ufa300(geoRadiusSupport = true)

    private val webServerRule = WebServerRule {
        getOfferLocatorCounters(OfferLocatorCountersResponse.GEO_RADIUS_BUBBLES)
        getGeoSuggest(DEFAULT_REGION_NAME)
    }

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        DisableAdsRule(),
        activityTestRule,
        SetPreferencesRule(),
        GeoSuggestRule(geoArgs)
    )

    @Before
    fun setUp() {
        geoArgs.radius = null
    }

    @Test
    fun shouldSeeBlockOfRelated() {
        webServerRule.routing {
            getOfferRelated().watch {
                checkNotQueryParameter(GEO_RADIUS_PARAM)
                checkQueryParameters(listOf(PAGE_SIZE_PARAM to "2", RID_PARAM to "172"))
            }
            getOffer(CAR_USED_ID)
        }

        activityTestRule.launchDeepLinkActivity(CAR_USED_URI)
        scrollToSpecials()

        checkRelatedBlockDisplayed()
    }

    @Test
    fun shouldSeeBlockOfSpecials() {
        webServerRule.routing {
            getOfferSpecials().watch {
                checkNotQueryParameter(GEO_RADIUS_PARAM)
                checkQueryParameters(listOf(PAGE_SIZE_PARAM to "2", RID_PARAM to "172"))
            }
            getOffer(CAR_USED_ID)
        }

        activityTestRule.launchDeepLinkActivity(CAR_USED_URI)
        scrollToSpecials()

        checkSpecialsBlockDisplayed()
    }

    @Test
    fun shouldNotSeeBlockOfRelatedWhenGotOneSpecial() {
        webServerRule.routing {
            getOfferRelated()
            getOfferSpecials("specials_one_offer.json")
            getOffer(CAR_USED_ID)
        }

        activityTestRule.launchDeepLinkActivity(CAR_USED_URI)
        scrollToSpecials()

        checkOfferCard { interactions.onSpecialBlockTitle().waitUntilIsCompletelyDisplayed() }
        checkOfferCard { interactions.onRelatedBlockTitle().checkNotExists() }
    }

    @Test
    fun shouldSeeSpecialsAndRelatedWithScroll() {
        webServerRule.routing {
            getOfferRelated()
            getOfferSpecials()
            getOffer(CAR_USED_ID)
        }

        activityTestRule.launchDeepLinkActivity(CAR_USED_URI)
        scrollToSpecials()

        checkSpecialsBlockDisplayed()
        performOfferCard { scrollHorizontalViewOfSpecialsToRelated("110 000 \u20BD") }
        checkRelatedBlockDisplayed()
    }

    @Test
    fun shouldUseRidAndRadiusFromListing() {
        webServerRule.routing {
            getOfferRelated().watch {
                checkQueryParameters(listOf(PAGE_SIZE_PARAM to "2", RID_PARAM to "172", GEO_RADIUS_PARAM to "1000"))
            }
            getOfferSpecials().watch {
                checkQueryParameters(listOf(PAGE_SIZE_PARAM to "2", RID_PARAM to "172", GEO_RADIUS_PARAM to "1000"))
            }
            getOffer(CAR_USED_ID)
            getParsedDeeplink(expectedResponse = "cars_all", mapper = { it.copyWithGeoUfa()} )
            postSearchOffers("listing_offers/extended_availability_on_order.json")
        }

        activityTestRule.launchDeepLinkActivity("https://auto.ru/ufa/cars/used")

        performSearchFeed {
            scrollToGeoRadiusFromBubbles(1_000)
            clickGeoRadiusItem(1_000)
            geoArgs.radius = 1_000
        }
        performListingOffers {
            scrollToFirstSnippet()
            interactions.onPrice().waitUntilIsCompletelyDisplayed().performClick()
        }
        scrollToSpecials()
    }

    @Test
    fun shouldSeeBlockOfDealsOfTheDay() {
        webServerRule.routing {
            getOfferRelated().watch {
                checkRequestWasNotCalled()
            }
            getOfferSpecials("deals_of_the_day.json").watch {
                checkQueryParameters(listOf(PAGE_SIZE_PARAM to "4", RID_PARAM to "172"))
                checkNotQueryParameter(GEO_RADIUS_PARAM)
            }
            getOffer(CAR_NEW_ID)
        }

        activityTestRule.launchDeepLinkActivity(CAR_NEW_URI)
        scrollToDealsOfTheDay()

        checkOfferCard {
            isSpecialBlockDealsOfTheDayTitleDisplayed()
            isSpecialBlockOfferOfDealsOfTheDayDisplayed(
                snippetIndex = 0,
                mark = "Land Rover Range Rover IV",
                price = "от 8 614 000 \u20BD",
                carParams = "3.0d AT (249 л.с.) 4WD • Vogue SE",
                badge = "Новый"
            )
            isSpecialBlockOfferOfDealsOfTheDayDisplayed(
                snippetIndex = 1,
                mark = "Land Rover Range Rover IV Рестайлинг",
                price = "8 734 000 \u20BD",
                carParams = "3.0d AT (249 л.с.) 4WD • Vogue SE"
            )
        }
        performOfferCard { scrollHorizontalViewOfDealsOfTheDay("от 6 919 000 \u20BD") }
        checkOfferCard {
            isSpecialBlockDealsOfTheDayTitleDisplayed()
            isSpecialBlockOfferOfDealsOfTheDayDisplayed(
                snippetIndex = 2,
                mark = "Land Rover Range Rover Sport II Рестайлинг",
                price = "от 6 919 000 \u20BD",
                carParams = "3.0d AT (249 л.с.) 4WD • HSE",
                badge = "Новый"
            )
        }
    }

    @Test
    fun shouldOpenRelatedOfferAndBackFromIt() {
        webServerRule.routing {
            getOfferRelated()
            getGeoSuggest(DEFAULT_REGION_NAME)
            getOffer(CAR_USED_ID)
            getOffer("1089534836-475a5aca")
        }

        activityTestRule.launchDeepLinkActivity(CAR_USED_URI)
        scrollToSpecials()

        performOfferCard {
            interactions.onSpecialBlockOfferImage(R.id.first_offer, R.string.related_offers_private)
                .waitUntilIsCompletelyDisplayed().performClick()
        }
        checkOfferCard { isOfferCardTitle("Audi A5 II, 2019") }
        pressBack()
        checkRelatedBlockDisplayed()
    }

    @Test
    fun shouldOpenDealOfTheDayOfferAndBackFromIt() {
        webServerRule.routing {
            getOfferSpecials("deals_of_the_day.json")
            getOffer(CAR_NEW_ID)
            getOffer("1089335968-ed330052")
        }

        activityTestRule.launchDeepLinkActivity(CAR_NEW_URI)
        scrollToDealsOfTheDay()

        performOfferCard {
            waitSomething(3000, TimeUnit.MILLISECONDS)
            interactions.onOfferParamOnDealsOfTheDay(0, 0).waitUntilIsCompletelyDisplayed().performClick()
        }
        checkOfferCard { isOfferCardTitle("Land Rover Range Rover IV, 2019") }
        pressBack()
        checkOfferCard { isSpecialBlockDealsOfTheDayTitleDisplayed() }
    }

    @Test
    fun shouldSeeSpecialsBlockOnMotoUsedOffer() {
        webServerRule.routing {
            getOfferSpecials("moto.json").watch { checkRequestWasCalled() }
            getOfferRelated("moto.json").watch { checkRequestWasCalled() }
            getOffer(offerId = "1894128-d229", category = "moto")
        }


        activityTestRule.launchDeepLinkActivity("https://auto.ru/moto/used/sale/1894128-d229")
        scrollToMotoCommSpecials()

        checkOfferCard { interactions.onSpecialBlockTitle().waitUntilIsCompletelyDisplayed() }
    }

    @Test
    fun shouldSeeSpecialsBlockOnTrucksNewOffer() {
        webServerRule.routing {
            getOfferSpecials("trucks.json").watch { checkRequestWasCalled() }
            getOfferRelated("trucks.json").watch { checkRequestWasCalled() }
            getOffer(offerId = "15868913-4916fa3b", category = "trucks")
        }

        activityTestRule.launchDeepLinkActivity("https://auto.ru/trucks/new/sale/15868913-4916fa3b")
        scrollToMotoCommSpecials()

        checkOfferCard { interactions.onSpecialBlockTitle().waitUntilIsCompletelyDisplayed() }
    }

    private fun scrollToSpecials() {
        checkOfferCard { interactions.onMakeCallOnCardButton().waitUntilIsCompletelyDisplayed() }
        onView(withId(R.id.app_bar_layout)).perform(setAppBarExpandedState(false))
        performOfferCard {
            scrollToComplain() //to initiate request for special block
            waitSomething(500, TimeUnit.MILLISECONDS) //wait until specials view is presented
            scrollToSpecials()
        }
    }

    private fun scrollToMotoCommSpecials() {
        checkOfferCard { interactions.onMakeCallOnCardButton().waitUntilIsCompletelyDisplayed() }
        onView(withId(R.id.app_bar_layout)).perform(setAppBarExpandedState(false))
        performOfferCard {
            scrollToComplain() //to initiate request for special block
            waitSomething(500, TimeUnit.MILLISECONDS) //wait until specials view is presented
            scrollToMotoCommSpecials()
        }
    }


    private fun scrollToDealsOfTheDay() {
        checkOfferCard { interactions.onMakeCallOnCardButton().waitUntilIsCompletelyDisplayed() }
        onView(withId(R.id.app_bar_layout)).perform(setAppBarExpandedState(false))
        performOfferCard {
            scrollToComplain() //to initiate request for special block
            waitSomething(500, TimeUnit.MILLISECONDS) //wait until deals of the day view is presented
            scrollToDealsOfTheDay()
        }
    }

    private fun checkRelatedBlockDisplayed() {
        checkOfferCard {
            isSpecialBlockOfferDisplayed(
                snippetNumberId = R.id.first_offer,
                blockTitle = R.string.related_offers_private,
                title = "3 050 000 \u20BD",
                subtitle = "Audi A5 II, 2019",
                badge = "Новый"
            )
            isSpecialBlockOfferDisplayed(
                snippetNumberId = R.id.second_offer,
                blockTitle = R.string.related_offers_private,
                title = "110 000 \u20BD",
                subtitle = "Volvo 940, 1992, 355000 км"
            )
        }
    }

    private fun checkSpecialsBlockDisplayed() {
        checkOfferCard {
            isSpecialBlockOfferDisplayed(
                snippetNumberId = R.id.first_offer,
                blockTitle = R.string.specials,
                title = "от 2 099 000 \u20BD",
                subtitle = "Lexus UX I 200, 2019",
                badge = "Новый"
            )
            isSpecialBlockOfferDisplayed(
                snippetNumberId = R.id.second_offer,
                blockTitle = R.string.specials,
                title = "1 279 000 \u20BD",
                subtitle = "Toyota RAV 4 IV (CA40), 2015, 67000 км"
            )
        }
    }

}
