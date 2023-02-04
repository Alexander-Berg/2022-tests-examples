package ru.auto.ara.test.deeplink

import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.R
import ru.auto.ara.core.dispatchers.device.copyWithStateGroup
import ru.auto.ara.core.dispatchers.device.copyWithoutGeo
import ru.auto.ara.core.dispatchers.device.getParsedDeeplink
import ru.auto.ara.core.dispatchers.search_offers.PostSearchSpecialsDispatcher
import ru.auto.ara.core.dispatchers.search_offers.getOfferCount
import ru.auto.ara.core.dispatchers.search_offers.postSearchOffers
import ru.auto.ara.core.dispatchers.search_offers.postSearchOffersGroup
import ru.auto.ara.core.dispatchers.shark.getOneCreditProduct
import ru.auto.ara.core.robot.searchfeed.checkFilter
import ru.auto.ara.core.robot.searchfeed.performComplectation
import ru.auto.ara.core.robot.searchfeed.performFilter
import ru.auto.ara.core.robot.searchfeed.performSearchFeed
import ru.auto.ara.core.routing.delegateDispatcher
import ru.auto.ara.core.rules.DisableAdsRule
import ru.auto.ara.core.rules.SetPreferencesRule
import ru.auto.ara.core.rules.baseRuleChain
import ru.auto.ara.core.rules.lazyActivityScenarioRule
import ru.auto.ara.core.rules.mock.WebServerRule
import ru.auto.ara.core.utils.launchDeepLinkActivity
import ru.auto.ara.deeplink.DeeplinkActivity
import ru.auto.data.model.filter.GroupBy
import ru.auto.data.model.network.scala.search.NWStateGroup
import ru.auto.data.network.scala.response.DeeplinkParseResponse
import java.text.NumberFormat

@RunWith(Parameterized::class)
class FilterScreenDeeplinkTest(private val testParameter: TestParameter) {

    private val webServerRule = WebServerRule {
        getOfferCount()
        postSearchOffers()
        postSearchOffersGroup("listing_offers/generic_feed_cars_20_offers.json", GroupBy.CONFIGURATION)
        getParsedDeeplink(testParameter.parsedDeeplinkAssetName, mapper = testParameter.parsedDeeplinkTransform)
        delegateDispatcher(PostSearchSpecialsDispatcher.getLadaPresetOffers())
        getOneCreditProduct()
    }

    private val activityRule = lazyActivityScenarioRule<DeeplinkActivity>()

    @JvmField
    @Rule
    var ruleChain = baseRuleChain(
        webServerRule,
        activityRule,
        DisableAdsRule(),
        SetPreferencesRule()
    )

    @Test
    fun shouldOpenDeeplink() {
        activityRule.launchDeepLinkActivity(testParameter.uri)
        performSearchFeed {
            waitSearchFeed()
            openParameters()
        }
        testParameter.check()
    }

    companion object {
        private val fractionalNumberFormat = NumberFormat.getInstance().apply { maximumFractionDigits = 1 }

        @JvmStatic
        @Parameterized.Parameters(name = "index={index} {0}")
        fun data(): Collection<Array<out Any?>> =
            listOf(
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/?mark_model_nameplate=BMW%233ER%23%237744658",
                    check = {
                        checkFilter {
                            isRegion("Россия")
                            isToggleButtonChecked(R.string.category_auto)
                            isToggleButtonChecked(R.string.old_auto)
                            isDMRLContainer(R.string.field_mark_label, "BMW")
                            isDMRLContainer(R.string.field_model_label, "3 серия")
                            isDMRLContainer(R.string.field_generation_label, "2011 - 2016 VI (F3x)")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_bmw_3_series"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/?year_from=2010&year_to=2018",
                    check = {
                        checkFilter {
                            isContainer(R.string.field_year_label, "от 2010 до 2018")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_year_from"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/?body_type_group=SEDAN",
                    check = {
                        checkFilter {
                            isContainer(R.string.field_bodytype_label, "Седан")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_body_type_group"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/?engine_group=DIESEL&engine_group=GASOLINE",
                    check = {
                        checkFilter {
                            isContainer(R.string.field_engine_label, "Бензин, Дизель")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_engine_group"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/?displacement_from=1600&displacement_to=5500",
                    check = {
                        checkFilter {
                            isContainer(
                                "Объём двигателя, л",
                                "от ${fractionalNumberFormat.format(1.6)} до ${fractionalNumberFormat.format(5.5)}"
                            )
                        }
                    },
                    parsedDeeplinkAssetName = "cars_displacement"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/?gear_type=ALL_WHEEL_DRIVE&gear_type=REAR_DRIVE",
                    check = {
                        checkFilter {
                            isContainer(R.string.field_drive_label, "Задний, Полный")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_gear_type"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/?clearance_from=10",
                    check = {
                        checkFilter {
                            isContainer(R.string.field_clearance_label, "от 10")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_clearance"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/?trunk_volume_from=30",
                    check = {
                        checkFilter {
                            isContainer(R.string.field_trunk_volume_label, "от 30")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_trunk_volume"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/" +
                            "?color=FFD600&color=040001&color=CACECB&color=EE1D19&color=22A0F8&color=FAFBFB",
                    check = {
                        checkFilter {
                            isContainer(R.string.field_color_label, "Черный, Серебряный, Белый, Красный, Желтый, Голубой")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_color"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/?steering_wheel=LEFT",
                    check = {
                        checkFilter {
                            isInputContainer(R.string.field_wheel_position_label, "Левый")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_steering_wheel"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/?seller_group=PRIVATE",
                    check = {
                        checkFilter {
                            isToggleButtonCheckedWithOverScroll("Владельцев по ПТС", "Частник")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_seller_group"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/?owners_count_group=LESS_THAN_TWO",
                    check = {
                        checkFilter {
                            isInputContainer(R.string.field_owners_label, "Не более двух")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_owners_count_group"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/?owning_time_group=FROM_1_TO_3",
                    check = {
                        checkFilter {
                            isInputContainer(R.string.field_years_age_label, "От 1 до 3 лет")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_owning_time_group"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/?customs_state_group=DOESNT_MATTER",
                    check = {
                        checkFilter {
                            isInputContainer(R.string.field_custom_label, "Неважно")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_customs_state"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/?pts_status=1",
                    check = {
                        checkFilter {
                            isChecked(R.string.field_pts_label, true)
                            isChecked(R.string.field_pts_label, true)
                        }
                    },
                    parsedDeeplinkAssetName = "cars_pts_status"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/?with_warranty=true",
                    check = {
                        checkFilter {
                            isChecked(R.string.field_warranty_label, true)
                        }
                    },
                    parsedDeeplinkAssetName = "cars_with_warranty"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/?exchange_group=POSSIBLE",
                    check = {
                        checkFilter {
                            isChecked(R.string.field_exchange_label, true)
                        }
                    },
                    parsedDeeplinkAssetName = "cars_exchange_group"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/?has_image=false",
                    check = {
                        checkFilter {
                            isChecked(R.string.field_photo_label, false)
                        }
                    },
                    parsedDeeplinkAssetName = "cars_has_image"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/?has_video=true",
                    check = {
                        checkFilter {
                            isChecked(R.string.field_video_label, true)
                        }
                    },
                    parsedDeeplinkAssetName = "cars_has_video"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/?catalog_equipment=light-sensor&catalog_equipment=rain-sensor",
                    check = {
                        checkFilter {
                            isExtrasValues("Выбрано 2 параметра")
                        }
                        performFilter {
                            openComplectation(R.string.change_complectation)
                        }
                        performComplectation {}.checkResult {
                            isChecked("Датчик дождя", true)
                            isChecked("Датчик света", true)
                        }
                    },
                    parsedDeeplinkAssetName = "cars_catalog_equipment"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/skutery/new/",
                    check = {
                        checkFilter {
                            isRegion("Россия")
                            isToggleButtonChecked(R.string.category_moto)
                            isInputContainer(R.string.field_sub_category_label, "Скутеры")
                            isToggleButtonChecked(R.string.new_vehicles)
                        }
                    },
                    parsedDeeplinkAssetName = "moto_scooters_new"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/motovezdehody/all/?geo_id=225",
                    check = {
                        checkFilter {
                            isRegion("Россия")
                            isToggleButtonChecked(R.string.category_moto)
                            isInputContainer(R.string.field_sub_category_label, "Мотовездеходы")
                            isToggleButtonChecked(R.string.all)
                        }
                    },
                    parsedDeeplinkAssetName = "moto_motovezdehody"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/motovezdehody/all/?moto_type=SPORTS&moto_type=TOURIST",
                    check = {
                        checkFilter {
                            isContainer("Тип мотовездехода", "Спортивный, Туристический")
                        }
                    },
                    parsedDeeplinkAssetName = "moto_moto_type"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/motovezdehody/all/?cylinders_type=LINE&cylinders_type=OPPOSITE",
                    check = {
                        checkFilter {
                            isContainer("Расположение цилиндров", "Оппозитное, Рядное")
                        }
                    },
                    parsedDeeplinkAssetName = "moto_cylinders"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/motovezdehody/all/?strokes=2",
                    check = {
                        checkFilter {
                            isInputContainer("Число тактов", "2-тактный")
                        }
                    },
                    parsedDeeplinkAssetName = "moto_strokes"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/legkie-gruzoviki/citroen/all/",
                    check = {
                        checkFilter {
                            isRegion("Россия")
                            isToggleButtonChecked(R.string.category_comm)
                            isInputContainer(R.string.field_sub_category_label, "Лёгкие коммерческие")
                            isToggleButtonChecked(R.string.all)
                            isDMRLContainer(R.string.field_mark_label, "Citroen")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_lcv_citroen"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/legkie-gruzoviki/citroen/all/?loading=0_1000",
                    check = {
                        checkFilter {
                            isContainer("Грузоподъёмность, тонн", "до 1")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_lcv_loading"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/legkie-gruzoviki/citroen/all/?body_key=ISOTHERMAL_BODY&body_key=ONBOARD_TRUCK",
                    check = {
                        checkFilter {
                            isContainer(R.string.field_truck_bodytype_label, "Бортовой грузовик, Изотермический фургон")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_lcv_body_key"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/legkie-gruzoviki/citroen/all/?seats_from=2",
                    check = {
                        checkFilter {
                            isContainer("Число мест", "от 2 ")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_lcv_seats"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/legkie-gruzoviki/citroen/all/" +
                            "?transmission_full=AUTOMATIC&transmission_full=MECHANICAL",
                    check = {
                        checkFilter {
                            isContainer(R.string.field_gearbox_label, "Механическая, АКПП")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_lcv_transmission"
                ),
                TestParameter(
                    uri = "https://auto.ru/sankt-peterburg/artic/daf/used/",
                    check = {
                        checkFilter {
                            isRegion("Санкт-Петербург")
                            isRadius("+ 200 км")
                            isToggleButtonChecked(R.string.category_comm)
                            isInputContainer(R.string.field_sub_category_label, "Седельные тягачи")
                            isToggleButtonChecked(R.string.old_auto)
                            isDMRLContainer(R.string.field_mark_label, "DAF")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_artic_piter-daf"
                ),
                TestParameter(
                    uri = "https://auto.ru/sankt-peterburg/artic/daf/used/?saddle_height=150&saddle_height=185",
                    check = {
                        checkFilter {
                            isContainer("Высота седельного устройства", "150, 185")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_artic_saddle_height"
                ),
                TestParameter(
                    uri = "https://auto.ru/sankt-peterburg/artic/daf/used/?top_days=7",
                    check = {
                        checkFilter {
                            isInputContainer(R.string.field_period_label, "За неделю")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_artic_top_days"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/bus/mercedes/new/",
                    check = {
                        checkFilter {
                            isRegion("Россия")
                            isToggleButtonChecked(R.string.category_comm)
                            isInputContainer(R.string.field_sub_category_label, "Автобусы")
                            isToggleButtonChecked(R.string.new_vehicles)
                            isDMRLContainer(R.string.field_mark_label, "Mercedes-Benz")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_bus_mercedes"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/bus/mercedes/new/?bus_type=INTERCITY&bus_type=URBAN",
                    check = {
                        checkFilter {
                            isContainer(R.string.field_bus_type_label, "Городской, Междугородный")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_bus_bus_type"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/all/?search_tag=allowed_for_credit",
                    check = {
                        checkFilter {
                            isChecked(R.string.loan_available, true)
                        }
                    },
                    parsedDeeplinkAssetName = "cars_allowed_for_credit"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/?on_credit=true",
                    check = {
                        checkFilter {
                            isChecked(R.string.loan_available, true)
                        }
                    },
                    parsedDeeplinkAssetName = "cars_on_credit"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/lcv/new/",
                    check = {
                        checkFilter {
                            isToggleButtonChecked(R.string.category_comm)
                            isInputContainer(R.string.field_sub_category_label, "Лёгкие коммерческие")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_lcv_new"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/truck/new/",
                    check = {
                        checkFilter {
                            isToggleButtonChecked(R.string.category_comm)
                            isInputContainer(R.string.field_sub_category_label, "Грузовики")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_truck_new"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/trailer/new/",
                    check = {
                        checkFilter {
                            isToggleButtonChecked(R.string.category_comm)
                            isInputContainer(R.string.field_sub_category_label, "Прицепы и съёмные кузова")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_trailer_new"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/motorcycle/new/",
                    check = {
                        checkFilter {
                            isToggleButtonChecked(R.string.category_moto)
                            isInputContainer(R.string.field_sub_category_label, "Мотоциклы")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_motorcycle_new"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/scooters/new/",
                    check = {
                        checkFilter {
                            isToggleButtonChecked(R.string.category_moto)
                            isInputContainer(R.string.field_sub_category_label, "Скутеры")
                        }
                    },
                    parsedDeeplinkAssetName = "moto_scooters"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/atv/new/",
                    check = {
                        checkFilter {
                            isToggleButtonChecked(R.string.category_moto)
                            isInputContainer(R.string.field_sub_category_label, "Мотовездеходы")
                        }
                    },
                    parsedDeeplinkAssetName = "moto_atv"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/snowmobile/new/",
                    check = {
                        checkFilter {
                            isToggleButtonChecked(R.string.category_moto)
                            isInputContainer(R.string.field_sub_category_label, "Снегоходы")
                        }
                    },
                    parsedDeeplinkAssetName = "moto_snowmobile"
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/crane/all/",
                    check = {
                        checkFilter {
                            isToggleButtonChecked(R.string.category_comm)
                            isInputContainer(R.string.field_sub_category_label, "Автокраны")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_crane"
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/bulldozers/all/",
                    check = {
                        checkFilter {
                            isToggleButtonChecked(R.string.category_comm)
                            isInputContainer(R.string.field_sub_category_label, "Бульдозеры")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_bulldozers"
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/autoloader/all/",
                    check = {
                        checkFilter {
                            isToggleButtonChecked(R.string.category_comm)
                            isInputContainer(R.string.field_sub_category_label, "Погрузчики")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_autoloader"
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/construction/all/",
                    check = {
                        checkFilter {
                            isToggleButtonChecked(R.string.category_comm)
                            isInputContainer(R.string.field_sub_category_label, "Строительная")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_construction"
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/agricultural/all/",
                    check = {
                        checkFilter {
                            isToggleButtonChecked(R.string.category_comm)
                            isInputContainer(R.string.field_sub_category_label, "Сельскохозяйственная")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_agricultural"
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/dredge/all/",
                    check = {
                        checkFilter {
                            isToggleButtonChecked(R.string.category_comm)
                            isInputContainer(R.string.field_sub_category_label, "Экскаваторы")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_dredge"
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/municipal/all/",
                    check = {
                        checkFilter {
                            isToggleButtonChecked(R.string.category_comm)
                            isInputContainer(R.string.field_sub_category_label, "Коммунальная")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_municipal"
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/agricultural/all/?operating_hours_from=5000&operating_hours_to=20000",
                    check = {
                        checkFilter {
                            isContainer("Моточасы", "от 5000 до 20000")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_agricultural_operating_hours"
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/construction/all/?body_key=RINK&body_key=PIPELAYER",
                    check = {
                        checkFilter {
                            isContainer(R.string.field_body_key_label, "Трубоукладчик, Каток")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_construction_body_key"
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/autoloader/all/?body_key=UNIVERSAL_FORKLIFTS",
                    check = {
                        checkFilter {
                            isContainer("Тип погрузчика", "Универсальный погрузчик")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_autoloader_body_key"
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/autoloader/all/?load_height_from=1&load_height_to=20",
                    check = {
                        checkFilter {
                            isContainer("Высота подъёма, м", "от 1 до 20")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_autoloader_load_height"
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/crane/all/?crane_radius_from=1&crane_radius_to=11",
                    check = {
                        checkFilter {
                            isContainer("Вылет стрелы, м", "от 1 до 11")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_crane_crane_radius"
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/dredge/all/?body_key=CRAWLER_EXCAVATOR",
                    check = {
                        checkFilter {
                            isContainer("Тип экскаватора", "Гусеничный экскаватор")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_dredge_body_key"
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/dredge/all/?bucket_volume_from=1",
                    check = {
                        checkFilter {
                            isContainer("Объём ковша, м³", "от 1 ")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_dredge_bucket_volume"
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/bulldozers/all/?body_key=WHEELS_BULLDOZER",
                    check = {
                        checkFilter {
                            isContainer("Тип бульдозера", "Колёсный бульдозер")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_bulldozers_body_key"
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/municipal/all/?body_key=SNOWPLOW",
                    check = {
                        checkFilter {
                            isContainer("Тип техники", "Снегоочиститель")
                        }
                    },
                    parsedDeeplinkAssetName = "trucks_municipal_body_key"
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/cars/bmw/3er/20548423/all/",
                    check = {
                        checkFilter {
                            isRegion("Москва")
                            isToggleButtonChecked(R.string.category_auto)
                            isToggleButtonChecked(R.string.all)
                            isDMRLContainer(R.string.field_mark_label, "BMW")
                            isDMRLContainer(R.string.field_model_label, "3 серия")
                            isDMRLContainer(R.string.field_generation_label, "2015 - 2020 VI (F3x) Рестайлинг")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_bmw_3_series_with_generation",
                    parsedDeeplinkTransform = { it.copyWithStateGroup(NWStateGroup.ALL) },
                    name = "geo_cars_mark_model_generation_all"
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/cars/bmw/3er/20548423/new/",
                    check = {
                        checkFilter {
                            isRegion("Москва")
                            isToggleButtonChecked(R.string.category_auto)
                            isToggleButtonChecked(R.string.section_new)
                            isDMRLContainer(R.string.field_mark_label, "BMW")
                            isDMRLContainer(R.string.field_model_label, "3 серия")
                            isDMRLContainer(R.string.field_generation_label, "2015 - 2020 VI (F3x) Рестайлинг")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_bmw_3_series_with_generation",
                    parsedDeeplinkTransform = { it.copyWithStateGroup(NWStateGroup.NEW) },
                    name = "geo_cars_mark_model_generation_new"
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/cars/bmw/3er/20548423/used/",
                    check = {
                        checkFilter {
                            isRegion("Москва")
                            isToggleButtonChecked(R.string.category_auto)
                            isToggleButtonChecked(R.string.section_used)
                            isDMRLContainer(R.string.field_mark_label, "BMW")
                            isDMRLContainer(R.string.field_model_label, "3 серия")
                            isDMRLContainer(R.string.field_generation_label, "2015 - 2020 VI (F3x) Рестайлинг")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_bmw_3_series_with_generation",
                    parsedDeeplinkTransform = { it.copyWithStateGroup(NWStateGroup.USED) },
                    name = "geo_cars_mark_model_generation_used"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/bmw/3er/all/",
                    check = {
                        checkFilter {
                            isRegion("Россия")
                            isToggleButtonChecked(R.string.category_auto)
                            isToggleButtonChecked(R.string.all)
                            isDMRLContainer(R.string.field_mark_label, "BMW")
                            isDMRLContainer(R.string.field_model_label, "3 серия")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_bmw_3_series",
                    parsedDeeplinkTransform = { it.copyWithStateGroup(NWStateGroup.ALL) },
                    name = "geo_cars_mark_model_all"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/bmw/3er/new/",
                    check = {
                        checkFilter {
                            isRegion("Россия")
                            isToggleButtonChecked(R.string.category_auto)
                            isToggleButtonChecked(R.string.section_new)
                            isDMRLContainer(R.string.field_mark_label, "BMW")
                            isDMRLContainer(R.string.field_model_label, "3 серия")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_bmw_3_series",
                    parsedDeeplinkTransform = { it.copyWithStateGroup(NWStateGroup.NEW) },
                    name = "geo_cars_mark_model_new"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/bmw/3er/used/",
                    check = {
                        checkFilter {
                            isRegion("Россия")
                            isToggleButtonChecked(R.string.category_auto)
                            isToggleButtonChecked(R.string.section_used)
                            isDMRLContainer(R.string.field_mark_label, "BMW")
                            isDMRLContainer(R.string.field_model_label, "3 серия")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_bmw_3_series",
                    parsedDeeplinkTransform = { it.copyWithStateGroup(NWStateGroup.USED) },
                    name = "geo_cars_mark_model_used"
                ),
                TestParameter(
                    uri = "https://auto.ru/cars/bmw/3er/20548423/new/",
                    check = {
                        checkFilter {
                            isToggleButtonChecked(R.string.category_auto)
                            isToggleButtonChecked(R.string.section_new)
                            isDMRLContainer(R.string.field_mark_label, "BMW")
                            isDMRLContainer(R.string.field_model_label, "3 серия")
                            isDMRLContainer(R.string.field_generation_label, "2015 - 2020 VI (F3x) Рестайлинг")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_bmw_3_series_with_generation",
                    parsedDeeplinkTransform = { it.copyWithStateGroup(NWStateGroup.NEW).copyWithoutGeo() },
                    name = "cars_mark_model_generation_new"
                ),
                TestParameter(
                    uri = "https://auto.ru/cars/bmw/3er/20548423/used/",
                    check = {
                        checkFilter {
                            isToggleButtonChecked(R.string.category_auto)
                            isToggleButtonChecked(R.string.section_used)
                            isDMRLContainer(R.string.field_mark_label, "BMW")
                            isDMRLContainer(R.string.field_model_label, "3 серия")
                            isDMRLContainer(R.string.field_generation_label, "2015 - 2020 VI (F3x) Рестайлинг")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_bmw_3_series_with_generation",
                    parsedDeeplinkTransform = { it.copyWithStateGroup(NWStateGroup.USED).copyWithoutGeo() },
                    name = "cars_mark_model_generation_used"
                ),
                TestParameter(
                    uri = "https://auto.ru/cars/bmw/3er/20548423/all/",
                    check = {
                        checkFilter {
                            isToggleButtonChecked(R.string.category_auto)
                            isToggleButtonChecked(R.string.all)
                            isDMRLContainer(R.string.field_mark_label, "BMW")
                            isDMRLContainer(R.string.field_model_label, "3 серия")
                            isDMRLContainer(R.string.field_generation_label, "2015 - 2020 VI (F3x) Рестайлинг")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_bmw_3_series_with_generation",
                    parsedDeeplinkTransform = { it.copyWithStateGroup(NWStateGroup.ALL).copyWithoutGeo() },
                    name = "cars_mark_model_generation_all"
                ),
                TestParameter(
                    uri = "https://auto.ru/cars/bmw/3er/new/",
                    check = {
                        checkFilter {
                            isToggleButtonChecked(R.string.category_auto)
                            isToggleButtonChecked(R.string.section_new)
                            isDMRLContainer(R.string.field_mark_label, "BMW")
                            isDMRLContainer(R.string.field_model_label, "3 серия")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_bmw_3_series",
                    parsedDeeplinkTransform = { it.copyWithStateGroup(NWStateGroup.NEW).copyWithoutGeo() },
                    name = "cars_mark_model_new"
                ),
                TestParameter(
                    uri = "https://auto.ru/cars/bmw/3er/used/",
                    check = {
                        checkFilter {
                            isToggleButtonChecked(R.string.category_auto)
                            isToggleButtonChecked(R.string.section_used)
                            isDMRLContainer(R.string.field_mark_label, "BMW")
                            isDMRLContainer(R.string.field_model_label, "3 серия")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_bmw_3_series",
                    parsedDeeplinkTransform = { it.copyWithStateGroup(NWStateGroup.USED).copyWithoutGeo() },
                    name = "cars_mark_model_used"
                ),
                TestParameter(
                    uri = "https://auto.ru/cars/bmw/3er/all/",
                    check = {
                        checkFilter {
                            isToggleButtonChecked(R.string.category_auto)
                            isToggleButtonChecked(R.string.all)
                            isDMRLContainer(R.string.field_mark_label, "BMW")
                            isDMRLContainer(R.string.field_model_label, "3 серия")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_bmw_3_series",
                    parsedDeeplinkTransform = { it.copyWithStateGroup(NWStateGroup.ALL).copyWithoutGeo() },
                    name = "cars_mark_model_all"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/new/",
                    check = {
                        checkFilter {
                            isRegion("Россия")
                            isToggleButtonChecked(R.string.category_auto)
                            isToggleButtonChecked(R.string.section_new)
                            onDmrlContainerWithTitleSubtitleView("BMW", "1 серия (2017 - 2020 II (F20/F21) Рестайлинг 2)")
                            isDMRLContainer(R.string.field_mark_label, "LADA (ВАЗ)")
                            isDMRLContainer(R.string.field_model_label, "1111 ОКА")
                        }
                    },
                    parsedDeeplinkAssetName = "rossiya_cars_BMW_VAZ",
                    parsedDeeplinkTransform = { it.copyWithStateGroup(NWStateGroup.NEW) },
                    name = "new_rossiya_cars_BMW_VAZ"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/used/",
                    check = {
                        checkFilter {
                            isRegion("Россия")
                            isToggleButtonChecked(R.string.category_auto)
                            isToggleButtonChecked(R.string.section_used)
                            onDmrlContainerWithTitleSubtitleView("BMW", "1 серия (2017 - 2020 II (F20/F21) Рестайлинг 2)")
                            isDMRLContainer(R.string.field_mark_label, "LADA (ВАЗ)")
                            isDMRLContainer(R.string.field_model_label, "1111 ОКА")
                        }
                    },
                    parsedDeeplinkAssetName = "rossiya_cars_BMW_VAZ",
                    parsedDeeplinkTransform = { it.copyWithStateGroup(NWStateGroup.USED) },
                    name = "used_rossiya_cars_BMW_VAZ"
                ),
                TestParameter(
                    uri = "https://auto.ru/rossiya/cars/all/",
                    check = {
                        checkFilter {
                            isRegion("Россия")
                            isToggleButtonChecked(R.string.category_auto)
                            isToggleButtonChecked(R.string.all)
                            onDmrlContainerWithTitleSubtitleView("BMW", "1 серия (2017 - 2020 II (F20/F21) Рестайлинг 2)")
                            isDMRLContainer(R.string.field_mark_label, "LADA (ВАЗ)")
                            isDMRLContainer(R.string.field_model_label, "1111 ОКА")
                        }
                    },
                    parsedDeeplinkAssetName = "rossiya_cars_BMW_VAZ",
                    parsedDeeplinkTransform = { it.copyWithStateGroup(NWStateGroup.ALL) },
                    name = "all_rossiya_cars_BMW_VAZ"
                ),
                TestParameter(
                    uri = "https://auto.ru/moskva/",
                    check = {
                        checkFilter {
                            isRegion("Москва")
                        }
                    },
                    parsedDeeplinkAssetName = "cars_bmw_3_series_with_generation"
                )
            ).map { arrayOf(it) }

        data class TestParameter(
            val uri: String,
            val check: () -> Unit,
            val parsedDeeplinkAssetName: String,
            val parsedDeeplinkTransform: (DeeplinkParseResponse) -> DeeplinkParseResponse = { it },
            val name: String = parsedDeeplinkAssetName,
        ) {
            override fun toString(): String = name
        }
    }
}
