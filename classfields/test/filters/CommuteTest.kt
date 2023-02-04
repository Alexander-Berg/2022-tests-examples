package com.yandex.mobile.realty.test.filters

import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.test.filters.SdkSuppress
import androidx.test.rule.GrantPermissionRule
import com.yandex.mobile.realty.core.robot.FiltersRobot
import com.yandex.mobile.realty.core.robot.performOnAddressSelectScreen
import com.yandex.mobile.realty.core.robot.performOnCommuteParamsScreen
import com.yandex.mobile.realty.core.rule.MockLocationRule
import com.yandex.mobile.realty.core.webserver.DispatcherRegistry
import com.yandex.mobile.realty.core.webserver.assetBody
import com.yandex.mobile.realty.core.webserver.request
import com.yandex.mobile.realty.core.webserver.response
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * @author andrikeev on 31/10/2019.
 */
@SdkSuppress(minSdkVersion = 23)
@RunWith(Parameterized::class)
class CommuteTest(
    private val dealType: DealType,
    private val propertyType: PropertyType,
    private val transport: CommuteTransport,
    private val time: CommuteTime,
    private val extraConfiguration: ExtraConfiguration
) : FilterParamTest() {

    private val mockLocationRule = MockLocationRule()

    init {
        ruleChain = RuleChain
            .outerRule(mockLocationRule)
            .around(GrantPermissionRule.grant(ACCESS_FINE_LOCATION))
            .around(ruleChain)
    }

    @Test
    fun shouldChangeOffersCountWhenCommuteSet() {
        val latitude = 55.734655
        val longitude = 37.642313
        mockLocationRule.setMockLocation(latitude, longitude)
        val searchParams = arrayOf(
            dealType.param,
            propertyType.param,
            transport.param,
            time.param,
            "commutePointLatitude" to latitude.toString(),
            "commutePointLongitude" to longitude.toString(),
            *extraConfiguration.params
        )
        shouldChangeOffersCount(
            webServerConfiguration = {
                registerGetAddress(
                    "latitude" to latitude.toString(),
                    "longitude" to longitude.toString()
                )
                registerGetPolygonAndCount(*searchParams)
                registerSearchCountOnly(EXPECTED_TOTAL_COUNT, *searchParams)
            },
            actionConfiguration = {
                tapOn(lookup.matchesDealTypeSelector())
                tapOn(dealType.matcher.invoke(lookup))
                tapOn(lookup.matchesPropertyTypeSelector())
                tapOn(propertyType.matcher.invoke(lookup))
                apply(extraConfiguration.actionConfiguration)
                scrollToPosition(lookup.matchesFieldCommute()).tapOn()

                performOnAddressSelectScreen {
                    waitUntil { isAddressContainerShown() }
                    tapOn(lookup.matchesConfirmAddressButton())
                }
                performOnCommuteParamsScreen {
                    waitUntil { isCommuteParamsShown() }
                    tapOn(transport.matcher.invoke(lookup))
                    tapOn(time.matcher.invoke(lookup))
                    pressBack()
                }

                isCommuteValueEquals(
                    time.expected,
                    transport.expected,
                    "Садовническая улица, 82с2"
                )
            },
            params = arrayOf(*searchParams)
        )
    }

    private fun DispatcherRegistry.registerGetAddress(
        vararg params: Pair<String, String?>?
    ) {
        register(
            request {
                path("1.0/addressGeocoder.json")
                for (item in params) {
                    item?.let { (name, value) ->
                        value?.let { queryParam(name, it) }
                    }
                }
            },
            response {
                assetBody("geocoderAddressAurora.json")
            }
        )
    }

    private fun DispatcherRegistry.registerGetPolygonAndCount(
        vararg params: Pair<String, String?>?
    ) {
        register(
            request {
                path("1.0/offerWithSiteSearch.json")
                queryParam("polygonAndCount", "YES")
                for (item in params) {
                    item?.let { (name, value) ->
                        value?.let { queryParam(name, it) }
                    }
                }
            },
            response {
                assetBody("commutePolygonAndCount.json")
            }
        )
    }

    interface ExtraConfiguration {
        val params: Array<Pair<String, String>>
        val actionConfiguration: FiltersRobot.() -> Unit
    }

    private companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0} {4} {1} commute {3} {2}")
        fun params(): Iterable<Array<*>> {
            return listOf(
                arrayOf(
                    DealType.SELL,
                    PropertyType.APARTMENT,
                    CommuteTransport.BY_FOOT,
                    CommuteTime.TEN_MIN,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.APARTMENT,
                    CommuteTransport.PUBLIC,
                    CommuteTime.FIFTEEN_MIN,
                    secondaryApartmentConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.ROOM,
                    CommuteTransport.AUTO,
                    CommuteTime.TWENTY_MIN,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.HOUSE,
                    CommuteTransport.BY_FOOT,
                    CommuteTime.THIRTY_MIN,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.LOT,
                    CommuteTransport.PUBLIC,
                    CommuteTime.FORTY_FIVE_MIN,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.COMMERCIAL,
                    CommuteTransport.AUTO,
                    CommuteTime.TEN_MIN,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.GARAGE,
                    CommuteTransport.BY_FOOT,
                    CommuteTime.FIFTEEN_MIN,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.RENT,
                    PropertyType.APARTMENT,
                    CommuteTransport.PUBLIC,
                    CommuteTime.TWENTY_MIN,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.RENT,
                    PropertyType.ROOM,
                    CommuteTransport.AUTO,
                    CommuteTime.THIRTY_MIN,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.RENT,
                    PropertyType.HOUSE,
                    CommuteTransport.BY_FOOT,
                    CommuteTime.FORTY_FIVE_MIN,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.RENT,
                    PropertyType.COMMERCIAL,
                    CommuteTransport.PUBLIC,
                    CommuteTime.TEN_MIN,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.RENT,
                    PropertyType.GARAGE,
                    CommuteTransport.AUTO,
                    CommuteTime.FIFTEEN_MIN,
                    defaultConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.APARTMENT,
                    CommuteTransport.BY_FOOT,
                    CommuteTime.TWENTY_MIN,
                    siteConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.HOUSE,
                    CommuteTransport.PUBLIC,
                    CommuteTime.THIRTY_MIN,
                    villageHouseConfiguration
                ),
                arrayOf(
                    DealType.SELL,
                    PropertyType.LOT,
                    CommuteTransport.AUTO,
                    CommuteTime.FORTY_FIVE_MIN,
                    villageLotConfiguration
                )
            )
        }

        val defaultConfiguration = object : ExtraConfiguration {

            override val params: Array<Pair<String, String>> = emptyArray()

            override val actionConfiguration: FiltersRobot.() -> Unit = {}

            override fun toString(): String {
                return ""
            }
        }

        val secondaryApartmentConfiguration = object : ExtraConfiguration {
            private val category = OfferCategory.SECONDARY.invoke(PropertyType.APARTMENT)

            override val params: Array<Pair<String, String>> = category.params

            override val actionConfiguration: FiltersRobot.() -> Unit = {
                tapOn(category.matcher.invoke(lookup))
            }

            override fun toString(): String {
                return "SECONDARY"
            }
        }

        val siteConfiguration = object : ExtraConfiguration {
            private val category = OfferCategory.PRIMARY.invoke(PropertyType.APARTMENT)

            override val params: Array<Pair<String, String>> = category.params

            override val actionConfiguration: FiltersRobot.() -> Unit = {
                tapOn(category.matcher.invoke(lookup))
            }

            override fun toString(): String {
                return "SITE"
            }
        }

        val villageHouseConfiguration = object : ExtraConfiguration {
            private val category = OfferCategory.PRIMARY.invoke(PropertyType.HOUSE)

            override val params: Array<Pair<String, String>> = category.params

            override val actionConfiguration: FiltersRobot.() -> Unit = {
                tapOn(category.matcher.invoke(lookup))
            }

            override fun toString(): String {
                return "VILLAGE"
            }
        }

        val villageLotConfiguration = object : ExtraConfiguration {
            private val category = OfferCategory.PRIMARY.invoke(PropertyType.LOT)

            override val params: Array<Pair<String, String>> = category.params

            override val actionConfiguration: FiltersRobot.() -> Unit = {
                tapOn(category.matcher.invoke(lookup))
            }

            override fun toString(): String {
                return "VILLAGE"
            }
        }
    }
}
