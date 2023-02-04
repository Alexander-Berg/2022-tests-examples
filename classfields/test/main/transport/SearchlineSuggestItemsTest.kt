package ru.auto.ara.test.main.transport

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.MainActivity
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asArray
import ru.auto.ara.core.dispatchers.BodyNode.Companion.asObject
import ru.auto.ara.core.dispatchers.BodyNode.Companion.assertValue
import ru.auto.ara.core.dispatchers.RequestWatcher
import ru.auto.ara.core.dispatchers.search_offers.PostSearchOffersDispatcher
import ru.auto.ara.core.dispatchers.search_offers.getOfferCount
import ru.auto.ara.core.dispatchers.search_offers.getTextSearch
import ru.auto.ara.core.dispatchers.user.userSetup
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.robot.searchline.checkSearchline
import ru.auto.ara.core.robot.searchline.performSearchline
import ru.auto.ara.core.robot.transporttab.performMain
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.routing.watch
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.SetupAuthRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.activityScenarioRule

@RunWith(Parameterized::class)
class SearchlineSuggestItemsTest(val param: TestParam) {

    private val webRule = WebServerRule {
        userSetup()
        delegateDispatcher(PostSearchOffersDispatcher.getGenericFeed())
    }

    private val activityRule = activityScenarioRule<MainActivity>()

    @JvmField
    @Rule
    val rules = baseRuleChain(
        webRule,
        activityRule,
        SetupAuthRule(),
        SetPreferencesRule()
    )

    @Test
    fun shouldShowCorrectTitleAndSubtitle() {
        webRule.routing {
            getTextSearch(param.responseFileName)
            getOfferCount(category = param.category).watch {
                param.requestCheck(this)
            }
        }
        performMain { openSearchline() }
        performSearchline {
            waitScreenLoaded()
            replaceText(param.query)
        }
        checkSearchline {
            isSuggestDisplayed(
                suggest = param.title,
                index = 0,
                subtitle = param.subtitle,
                icon = param.icon
            )
        }
        performSearchline { selectSuggest(0) }
        performSearchFeed {
            waitSearchFeed()
            waitFirstPage()
        }
    }

    class TestParam(
        val query: String,
        val responseFileName: String,
        val title: String,
        val icon: String,
        val subtitle: String,
        val category: String,
        val requestCheck: (RequestWatcher) -> Unit = {}
    )

    companion object {
        @Suppress("MaxLineLength")
        @JvmStatic
        @Parameterized.Parameters(name = "index={index}")
        fun data(): List<Array<out Any>> = listOf(
            arrayOf(
                TestParam(
                    query = "ford focus 4 от 2000 до 2020 от 10000 до 100000 р красная механика седан бензин с пробегом",
                    responseFileName = "suggest_ford_focus_4",
                    title = "Ford Focus 2018 - 2020 IV",
                    icon = "https://avatars.mds.yandex.net/get-verba/1540742/2a0000016d691c30185172465dd1125ee1a5/logo",
                    subtitle = "Легковые • С пробегом • от 2000 г. до 2020 г. • от 10 000 ₽ до 100 000 ₽ • Механическая • Красный • Бензин • Седан",
                    category = "cars",
                    requestCheck = { watcher ->
                        watcher.checkRequestBodyArrayParameter("cars_params.body_type_group", setOf("SEDAN"))
                        watcher.checkRequestBodyArrayParameter("cars_params.engine_group", setOf("GASOLINE"))
                        watcher.checkRequestBodyArrayParameter("cars_params.transmission", setOf("MECHANICAL"))
                        watcher.checkRequestBodyArrayParameter("color", setOf("EE1D19"))
                        watcher.checkBody {
                            asObject {
                                get("catalog_filter").asArray {
                                    first().asObject {
                                        getValue("mark").assertValue("FORD")
                                        getValue("model").assertValue("FOCUS")
                                        getValue("generation").assertValue("21253647")
                                    }
                                }
                            }
                        }
                        watcher.checkRequestBodyParameters(
                            "customs_state_group" to "CLEARED",
                            "damage_group" to "NOT_BEATEN",
                            "has_image" to "true",
                            "in_stock" to "ANY_STOCK",
                            "only_nds" to "false",
                            "only_official" to "false",
                            "price_from" to "10000",
                            "price_to" to "100000",
                            "state_group" to "USED",
                            "with_delivery" to "BOTH",
                            "year_from" to "2000",
                            "year_to" to "2020"
                        )
                    }
                )
            ),
            arrayOf(
                TestParam(
                    query = "снегоход 2002 год 500000 р от 10000 до 1000000 км бу",
                    responseFileName = "snowmobile_2002_used",
                    title = "Снегоходы",
                    icon = R.drawable.ic_moto.toString(),
                    subtitle = "С пробегом • от 10 000 км до 1 000 000 км • 2002 г. • 500 000 ₽",
                    category = "moto",
                    requestCheck = { watcher ->
                        watcher.checkRequestBodyParameters(
                            "moto_params.moto_category" to "SNOWMOBILE",
                            "customs_state_group" to "DOESNT_MATTER",
                            "damage_group" to "NOT_BEATEN",
                            "has_image" to "true",
                            "in_stock" to "ANY_STOCK",
                            "only_nds" to "false",
                            "price_from" to "500000",
                            "price_to" to "500000",
                            "km_age_from" to "10000",
                            "km_age_to" to "1000000",
                            "state_group" to "USED",
                            "year_from" to "2002",
                            "year_to" to "2002"
                        )
                    }
                )
            ),
            arrayOf(
                TestParam(
                    query = "москва омск датчик дождя датчик света клиренс до 5 мм новая",
                    responseFileName = "clearance_5mm_rain_light_moscow",
                    title = "Легковые",
                    icon = R.drawable.ic_car.toString(),
                    subtitle = "Омск, Москва • Новые • Клиренс до 5 мм • Датчик света, Датчик дождя",
                    category = "cars",
                    requestCheck = { watcher ->
                        watcher.checkRequestBodyArrayParameter("catalog_equipment", setOf("light-sensor", "rain-sensor"))
                        watcher.checkRequestBodyArrayParameter("rid", setOf("66", "213"))
                        watcher.checkRequestBodyParameters(
                            "customs_state_group" to "DOESNT_MATTER",
                            "clearance_to" to "5",
                            "damage_group" to "ANY",
                            "has_image" to "true",
                            "in_stock" to "ANY_STOCK",
                            "only_nds" to "false",
                            "state_group" to "NEW"
                        )
                    }
                )
            ),
            arrayOf(
                TestParam(
                    query = "1 владелец от 5 л дилер не растаможен оригинал обмен битая",
                    responseFileName = "one_owner_original_beaten",
                    title = "Легковые",
                    icon = R.drawable.ic_car.toString(),
                    subtitle = "Объём двигателя от 5 л • Один владелец • Продавец компания • Битые • Не растаможен • ПТС оригинал • Обмен",
                    category = "cars",
                    requestCheck = { watcher ->
                        watcher.checkRequestBodyArrayParameter("seller_group", setOf("COMMERCIAL"))
                        watcher.checkRequestBodyParameters(
                            "customs_state_group" to "NOT_CLEARED",
                            "damage_group" to "BEATEN",
                            "displacement_from" to "5000",
                            "exchange_group" to "POSSIBLE",
                            "owners_count_group" to "ONE",
                            "pts_status" to "1",
                            "has_image" to "true",
                            "in_stock" to "ANY_STOCK",
                            "only_nds" to "false",
                            "state_group" to "USED"
                        )
                    }
                )
            ),
            arrayOf(
                TestParam(
                    query = "ммз 2 цилиндра без доставки ABS на гарантии новый до 5 л",
                    responseFileName = "mmz_new_to_5l_warranty",
                    title = "ММЗ",
                    icon = R.drawable.ic_moto.toString(),
                    subtitle = "Мотоциклы • Новые • Объём двигателя до 5 000 см³ • Цилиндров 2 • ABS • На гарантии • Без доставки • Антиблокировочная система (ABS)",
                    category = "moto",
                    requestCheck = { watcher ->
                        watcher.checkRequestBodyArrayParameter("moto_params.cylinders", setOf("CYLINDERS_2"))
                        watcher.checkRequestBodyParameters(
                            "moto_params.moto_category" to "MOTORCYCLE",
                            "customs_state_group" to "DOESNT_MATTER",
                            "damage_group" to "ANY",
                            "displacement_to" to "5000",
                            "has_image" to "true",
                            "in_stock" to "ANY_STOCK",
                            "only_nds" to "false",
                            "state_group" to "NEW"
                        )
                        watcher.checkBody {
                            asObject {
                                get("catalog_filter").asArray {
                                    first().asObject {
                                        getValue("mark").assertValue("MMZ")
                                    }
                                }
                            }
                        }
                    }
                )
            ),
            arrayOf(
                TestParam(
                    query = "бу автовоз 50000 км от 100000 р от 3 мест правый руль",
                    responseFileName = "autovoz_50000km_10000rub",
                    title = "Грузовики",
                    icon = R.drawable.ic_comm.toString(),
                    subtitle = "Автовоз • С пробегом • 50 000 км • от 100 000 ₽ • К-во мест от 3 • Праворульные",
                    category = "trucks",
                    requestCheck = { watcher ->
                        watcher.checkRequestBodyArrayParameter("seller_group", setOf("ANY_SELLER"))
                        watcher.checkRequestBodyArrayParameter("trucks_params.truck_type", setOf("AUTOTRANSPORTER"))
                        watcher.checkRequestBodyParameters(
                            "trucks_params.steering_wheel" to "RIGHT",
                            "trucks_params.trucks_category" to "TRUCK",
                            "customs_state_group" to "DOESNT_MATTER",
                            "damage_group" to "NOT_BEATEN",
                            "has_image" to "true",
                            "in_stock" to "ANY_STOCK",
                            "km_age_from" to "50000",
                            "km_age_to" to "50000",
                            "only_nds" to "false",
                            "state_group" to "USED"
                        )
                    }
                )
            ),
            arrayOf(
                TestParam(
                    query = "передний привод 12 л 2 владельца 50 л.с. правый руль частник битый оригинал на гарантии без доставки обмен",
                    responseFileName = "front_rear_2owners_displacement5",
                    title = "Легковые",
                    icon = R.drawable.ic_car.toString(),
                    subtitle = "Объём двигателя 12 л • Привод передний • Не более двух владельцев • 50 л.с. • Праворульные • Продавец частник • Битые • ПТС оригинал • На гарантии • Без доставки • Обмен",
                    category = "cars",
                    requestCheck = { watcher ->
                        watcher.checkRequestBodyArrayParameter("seller_group", setOf("PRIVATE"))
                        watcher.checkRequestBodyArrayParameter("cars_params.gear_type", setOf("FORWARD_CONTROL"))
                        watcher.checkRequestBodyParameters(
                            "cars_params.steering_wheel" to "RIGHT",
                            "customs_state_group" to "CLEARED",
                            "damage_group" to "BEATEN",
                            "displacement_from" to "12000",
                            "displacement_to" to "12000",
                            "exchange_group" to "POSSIBLE",
                            "only_official" to "false",
                            "owners_count_group" to "LESS_THAN_TWO",
                            "power_from" to "50",
                            "power_to" to "50",
                            "pts_status" to "1",
                            "has_image" to "true",
                            "in_stock" to "ANY_STOCK",
                            "only_nds" to "false",
                            "state_group" to "USED",
                            "with_delivery" to "NONE",
                            "with_warranty" to "true"
                        )
                    }
                )
            ),
            arrayOf(
                TestParam(
                    query = "питер инжектор новый синий дилер один владелец 10 л не растаможен обмен оригинал битый",
                    responseFileName = "piter_new_blue_power_5l_customs_state",
                    title = "Мотоциклы",
                    icon = R.drawable.ic_moto.toString(),
                    subtitle = "Санкт-Петербург • Новые • Синий • Объём двигателя 10 000 см³ • Бензин турбонаддув, Компрессор, Инжектор, Карбюратор, Ротор • Один владелец • Продавец компания • Не растаможен • ПТС оригинал • Обмен",
                    category = "moto",
                    requestCheck = { watcher ->
                        watcher.checkRequestBodyArrayParameter("color", setOf("0000CC"))
                        watcher.checkRequestBodyArrayParameter(
                            "moto_params.engine_type", setOf(
                                "GASOLINE_CARBURETOR",
                                "GASOLINE_ROTOR",
                                "GASOLINE_TURBO",
                                "GASOLINE_COMPRESSOR",
                                "GASOLINE_INJECTOR"
                            )
                        )
                        watcher.checkRequestBodyArrayParameter("rid", setOf("2"))
                        watcher.checkRequestBodyParameters(
                            "moto_params.moto_category" to "MOTORCYCLE",
                            "customs_state_group" to "DOESNT_MATTER",
                            "damage_group" to "ANY",
                            "displacement_from" to "10000",
                            "displacement_to" to "10000",
                            "has_image" to "true",
                            "in_stock" to "ANY_STOCK",
                            "only_nds" to "false",
                            "state_group" to "NEW"
                        )
                    }
                )
            )
        )
    }

}
