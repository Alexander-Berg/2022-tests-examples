//
//  FiltersTests.swift
//  UITests
//
//  Created by Dmitry Sinev on 10/27/21.
//

import XCTest
import AutoRuProtoModels

/// @depends_on AutoRuFilters AutoRuCellHelpers
final class FiltersTests: BaseTest {
    private func mockSearch() {
        mocker
            .mock_base()
            .mock_searchOfferLocatorCounters(type: .cars, distances: [])
            .mock_searchCount(count: 123)
            .mock_searchEquipment()
            .startMock()
        api.search.cars.breadcrumbs.get(parameters: .parameters([.bcLookup([""])])).ok(mock: .file("search_CARS_breadcrumbs_ok"))
        api.search.cars.breadcrumbs.get(parameters: .parameters([.bcLookup(["BMW"])])).ok(mock: .file("search_CARS_breadcrumbs_bmw_ok"))
        api.search.cars.breadcrumbs.get(parameters: .parameters([.bcLookup(["BMW#X5"])])).ok(mock: .file("search_CARS_breadcrumbs_bmwx5_ok"))
        api.reference.catalog.tags.format(.v1).get(parameters: .wildcard).ok(mock: .file("reference_catalog_tags_v1"))
    }

    func test_noPhotoFilters() {
        mockSearch()

        let searchWithPhotoExpectation = expectationForRequest(method: "POST", uri: "/search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc") { (request: Auto_Api_Search_SearchRequestParameters) in
            return request.searchTag.contains("real_photo")
        }
        let searchWithoutPhotoExpectation = expectationForRequest(method: "POST", uri: "/search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc") { (request: Auto_Api_Search_SearchRequestParameters) in
            return !request.searchTag.contains("real_photo")
        }

        launch(on: .transportScreen)
            .tap(.filterParametersButton)
            .should(provider: .filtersScreen, .exist)
            .focus { screen in
                screen.scroll(to: .photo, ofType: .filtersScreen)
                    .should(.photo, .be(.off))
                    .tap(.photo)
                    .should(.photo, .be(.on))
                    .tap(.searchButton)
            }
            .should(provider: .saleListScreen, .exist)
            .focus { screen in
                screen.tap(.searchParams)
            }
            .should(provider: .filtersScreen, .exist)
            .focus { screen in
                screen.scroll(to: .photo, ofType: .filtersScreen)
                    .should(.photo, .be(.on))
                    .tap(.photo)
                    .should(.photo, .be(.off))
                    .tap(.searchButton)
            }
            .wait(for: [searchWithPhotoExpectation, searchWithoutPhotoExpectation])
    }

    func test_filters_equipment() {
        mockSearch()
        let screen = launch(on: .transportScreen)
        checkFilters(transportScreen: screen,
                     filterChecks: [
                        optionPresetsFilter,
                        complectationFilter
                     ],
                     queryParams: .parameters([.context("listing"),
                                               .page(1),
                                               .pageSize(20),
                                               .sort("fresh_relevance_1-desc")]))
    }

    func test_filters_used() {
        mockSearch()
        let screen = launch(on: .transportScreen)

        checkFilters(transportScreen: screen,
                     filterChecks: [
                        mmngFilter,
                        usedStateGroupFilter,
                        yearFilter_2019to2020,
                        priceFilter_100_000to1_000_000,
                        transmissionFilter,
                        bodyFilter,
                        engineFilter,
                        engineVolumeFilter,
                        gearFilter,
                        powerFilter,
                        mileageFilter,
                        accelerationFilter,
                        fuelRateFilter,
                        clearanceFilter,
                        trunkVolumeFilter,
                        colorFilter,
                        wheelFilter,
                        sellerFilter,
                        ownersFilter,
                        owningTimeFilter,
                        stateFilter,
                        customsFilter,
                        ptsFilterOn,
                        manufacturerCheckFilterOn,
                        onlineViewFilterOn,
                        warrantyFilterOn,
                        exchangeFilterOn,
                        ndsFilterOn,
                        noPhotoFilterOn,
                        videoFilterOn,
                        panoramaFilterOn,
                        deliveryFilterOn,
                        postingTimeFilter
                     ],
                     queryParams: .parameters([.context("listing"),
                                               .page(1),
                                               .pageSize(20),
                                               .sort("fresh_relevance_1-desc")
                     ])
        )
    }

    func test_filters_new() {
        mockSearch()
        let screen = launch(on: .transportScreen)

        checkFilters(transportScreen: screen, filterChecks: [
            newStateGroupFilter,
            yearFilter_2019to2020,
            priceFilter_100_000to1_000_000,
            transmissionFilter,
            bodyFilter,
            engineFilter,
            engineVolumeFilter,
            gearFilter,
            powerFilter,
            accelerationFilter,
            fuelRateFilter,
            clearanceFilter,
            trunkVolumeFilter,
            colorFilter,
            onlineViewFilterOn,
            inStockFilterOn,
            ndsFilterOn,
            noPhotoFilterOn,
            videoFilterOn,
            panoramaFilterOn,
            postingTimeFilter],
        queryParams: .parameters([.context("listing"),
                                  .groupBy(["CONFIGURATION"]),
                                  .page(1),
                                  .pageSize(20),
                                  .sort("fresh_relevance_1-desc")
                                 ])
        )
    }

    private func checkFilters(transportScreen: TransportScreen, filterChecks: [FilterStateCheck], queryParams: EndpointQueryParametersMatching<PublicAPI.Search.Cars.Post.QueryParameter>) {

        let expectation = api.search.cars.post(parameters: queryParams).expect { request, _ in
            for check in filterChecks {
                guard check.searchCheck(request) else {
                    return .fail(reason: "failed check \(check.name) \r\n request: \(String(describing: try? request.jsonString()))")
                }
            }
            return .ok
        }

        transportScreen
            .tap(.filterParametersButton)
            .should(provider: .filtersScreen, .exist)
            .focus { screen in
                for check in filterChecks {
                    check.selectStrategy(screen)
                }
                screen.tap(.searchButton)
            }
            .should(provider: .saleListScreen, .exist)
            .wait(for: [expectation])
    }

    private let mmngFilter = FilterStateCheck(
        name: "МаркаМодель",
        selectStrategy: { screen in
            screen.scroll(to: .mmng, ofType: .filtersScreen)
                .should(.mmng, .exist)
                .tap(.mmng)
                .should(provider: .mmngPicker, .exist)
                .focus {
                    $0.tap(.mark("BMW"))
                    $0.tap(.model("X5"))
                }
                .scroll(to: .generation, ofType: .filtersScreen)
                .should(.generation, .exist)
                .tap(.generation)
                .should(provider: .mmngPicker, .exist)
                .focus {
                    $0.tap(.generation(21307931))
                }
        },
        searchCheck: { req in
            req.catalogFilter == [.with({
                $0.mark = "BMW"
                $0.model = "X5"
                $0.generation = 21307931
            })]
        })

    private let optionPresetsFilter = FilterStateCheck(
        name: "Пресеты опций",
        selectStrategy: { screen in
            screen.scroll(to: .optionPresets, ofType: .filtersScreen)
                .should(.optionPresets, .exist)
                .focus(on: .optionPresets, ofType: .optionPresetPicker) {
                    $0.tap(.compact)
                    $0.tap(.more)
                    $0.tap(.oversizedCargo)
                }
        },
        searchCheck: { req in
            Set(req.searchTag) == Set(["compact", "oversize"])
        })

    private let complectationFilter = FilterStateCheck(
        name: "Комплектация",
        selectStrategy: { screen in
            screen.scroll(to: .complectation, ofType: .filtersScreen)
                .should(.complectation, .exist)
                .tap(.complectation)
                .should(provider: .complectationPicker, .exist)
                .focus {
                    $0.tap(.rootItem("Фары"))
                    $0.tap(.item("Лазерные"))
                    $0.tap(.item("Омыватель фар"))
                    $0.tap(.doneButton)
                }
        },
        searchCheck: { req in
            req.catalogEquipment == ["laser-lights", "light-cleaner"]
        })

    private let newStateGroupFilter = FilterSpecFactory.segmentSelectFilter(name: "Состояние Новые", filter: .stateGroup, index: 1) { req in
        req.stateGroup == .new
    }

    private let usedStateGroupFilter = FilterSpecFactory.segmentSelectFilter(name: "Состояние С пробегом", filter: .stateGroup, index: 2) { req in
        req.stateGroup == .used
    }

    private let yearFilter_2019to2020 = FilterSpecFactory.rangeFilter(name: "Год",
                                                                      filter: .year,
                                                                      from: "2019",
                                                                      to: "2020",
                                                                      resultString: "от 2019 до 2020 г.") { req in
        req.yearFrom == 2019 && req.yearTo == 2020
    }

    private let priceFilter_100_000to1_000_000 = FilterStateCheck(
        name: "Цена",
        selectStrategy: { screen in
            screen.scroll(to: .price, ofType: .filtersScreen)
                .should(.price, .exist)
                .tap(.price)
                .should(provider: .textRangePicker, .exist)
                .focus({ picker in
                    picker.tap(.to)
                    picker.type("100000", in: .to)
                    picker.tap(.from)
                    picker.type("1000000", in: .from)
                    picker.tap(.doneButton)
                })
                .should(.price, .match("от 100,000 до 1,000,000 ₽"))
        },
        searchCheck: { req in
            return req.priceFrom == 100_000 && req.priceTo == 1_000_000
        })

    private let transmissionFilter = FilterSpecFactory.multiselectFilter(name: "Коробка",
                                                                         filter: .transmission,
                                                                         selectedOptions: ["AUTO"],
                                                                         resultString: "Автомат") { req in
        req.carsParams.transmission == [.automatic, .robot, .variator]
    }

    private let bodyFilter = FilterSpecFactory.multiselectFilter(name: "Кузов",
                                                                 filter: .bodytype,
                                                                 selectedOptions: ["SEDAN", "HATCHBACK_5_DOORS"],
                                                                 resultString: "Седан, Хэтчбек 5 д.") { req in
        req.carsParams.bodyTypeGroup == [.sedan, .hatchback5Doors]
    }

    private let engineFilter = FilterSpecFactory.multiselectFilter(name: "Двигатель",
                                                                   filter: .engine,
                                                                   selectedOptions: ["GASOLINE", "HYBRID"],
                                                                   resultString: "Бензин, Гибрид") { req in
        req.carsParams.engineGroup == [.gasoline, .hybrid]
    }

    private let engineVolumeFilter = FilterSpecFactory.rangeFilter(name: "Объем двигателя",
                                                                   filter: .engineVolume,
                                                                   from: "0.2",
                                                                   to: "0.5",
                                                                   resultString: "от 0.2 до 0.5 л") { req in
        return req.displacementFrom == 200 && req.displacementTo == 500
    }

    private let gearFilter = FilterSpecFactory.multiselectFilter(name: "Привод",
                                                                 filter: .gear,
                                                                 selectedOptions: ["FORWARD_CONTROL", "ALL_WHEEL_DRIVE"],
                                                                 resultString: "Передний, Полный") { req in
        req.carsParams.gearType == [.forwardControl, .allWheelDrive]
    }

    private let powerFilter = FilterSpecFactory.rangeFilter(name: "Мощность",
                                                            filter: .power,
                                                            from: "10",
                                                            to: "30",
                                                            resultString: "от 10 до 30 л.с.") { req in
        return req.powerFrom == 10 && req.powerTo == 30
    }

    private let mileageFilter = FilterSpecFactory.rangeFilter(name: "Пробег",
                                                              filter: .run,
                                                              from: "10,000",
                                                              to: "30,000",
                                                              resultString: "от 10,000 до 30,000 км") { req in
        req.kmAgeFrom == 10_000 && req.kmAgeTo == 30_000
    }

    private let accelerationFilter = FilterSpecFactory.rangeFilter(name: "Разгон",
                                                                   filter: .acceleration,
                                                                   from: "1",
                                                                   to: "5",
                                                                   resultString: "от 1 до 5 с") { req in
        return req.accelerationFrom == 1 && req.accelerationTo == 5
    }

    private let fuelRateFilter = FilterSpecFactory.rangeFilter(name: "Расход",
                                                               filter: .fuelRate,
                                                               to: "5",
                                                               resultString: "до 5 л") { req in
        return req.fuelRateTo == 5
    }

    private let clearanceFilter = FilterSpecFactory.rangeFilter(name: "Клиренс",
                                                                filter: .clearance,
                                                                from: "100",
                                                                resultString: "от 100 мм") { req in
        return req.clearanceFrom == 100
    }

    private let trunkVolumeFilter = FilterSpecFactory.rangeFilter(name: "Объем багажника",
                                                                  filter: .trunkVolume,
                                                                  from: "100",
                                                                  resultString: "от 100 л") { req in
        return req.trunkVolumeFrom == 100
    }

    private let colorFilter = FilterStateCheck(
        name: "Цвет",
        selectStrategy: { screen in
            screen.scroll(to: .colors, ofType: .filtersScreen)
                .should(.colors, .exist)
                .tap(.colors)
                .should(provider: .optionSelectPicker, .exist)
                .focus({ picker in
                    picker.tap(.option("040001"))
                    picker.tap(.option("FF0000"))
                    picker.tap(.doneButton)
                })
                .should(.color(0), .match("Черный"))
                .should(.color(1), .match("Красный"))
        },
        searchCheck: { req in
            req.color == ["040001", "EE1D19"]
        })

    private let wheelFilter = FilterSpecFactory.selectFilter(name: "Расположение руля",
                                                             filter: .wheel,
                                                             selectedOption: "LEFT",
                                                             resultString: "Левый") { req in
        req.carsParams.steeringWheel == .left
    }

    private let sellerFilter = FilterSpecFactory.segmentSelectFilter(name: "Категория продавца", filter: .seller, index: 1) { req in
        req.sellerGroup == [.commercial]
    }

    private let ownersFilter = FilterSpecFactory.selectFilter(name: "Владельцев по ПТС",
                                                              filter: .ownersCount,
                                                              selectedOption: "ONE",
                                                              resultString: "Один") { req in
        req.ownersCountGroup == .one
    }

    private let owningTimeFilter = FilterSpecFactory.selectFilter(name: "Срок владения",
                                                                  filter: .owningTime,
                                                                  selectedOption: "LESS_THAN_YEAR",
                                                                  resultString: "До 1 года") { req in
        req.owningTimeGroup == .lessThanYear
    }

    private let stateFilter = FilterSpecFactory.selectFilter(name: "Состояние",
                                                             filter: .state,
                                                             selectedOption: "BEATEN",
                                                             resultString: "Битые") { req in
        req.damageGroup == .beaten
    }

    private let customsFilter = FilterSpecFactory.selectFilter(name: "Растаможен",
                                                               filter: .customs,
                                                               selectedOption: "NOT_CLEARED",
                                                               resultString: "Не растаможен") { req in
        req.customsStateGroup == .notCleared
    }

    private let ptsFilterOn = FilterSpecFactory.checkFilter(name: "Оригинал ПТС",
                                                            filter: .original_pts,
                                                            state: .on) { req in
        req.ptsStatus == 1
    }

    private let manufacturerCheckFilterOn = FilterSpecFactory.checkFilter(name: "Проверенные производителем",
                                                                          filter: .manufacturerCheck,
                                                                          state: .on) { req in
        req.searchTag.contains("certificate_manufacturer")
    }

    private let onlineViewFilterOn = FilterSpecFactory.checkFilter(name: "Онлайн-показ",
                                                                   filter: .onlineView,
                                                                   state: .on) { req in
        req.searchTag.contains("online_view_available")
    }

    private let inStockFilterOn = FilterSpecFactory.checkFilter(name: "В наличии",
                                                                   filter: .inStock,
                                                                   state: .on) { req in
        req.inStock == .inStock
    }

    private let warrantyFilterOn = FilterSpecFactory.checkFilter(name: "На гарантии",
                                                                 filter: .warranty,
                                                                 state: .on) { req in
        req.withWarranty == true
    }

    private let exchangeFilterOn = FilterSpecFactory.checkFilter(name: "Возможен обмен",
                                                                 filter: .exchange,
                                                                 state: .on) { req in
        req.exchangeGroup == .possible
    }

    private let ndsFilterOn = FilterSpecFactory.checkFilter(name: "Только с НДС",
                                                            filter: .nds,
                                                            state: .on) { req in
        req.onlyNds == true
    }

    private let noPhotoFilterOn = FilterSpecFactory.checkFilter(name: "Без фото вкл",
                                                                filter: .photo,
                                                                state: .on) { req in
        req.searchTag.contains("real_photo")
    }

    private let videoFilterOn = FilterSpecFactory.checkFilter(name: "Только с видео",
                                                              filter: .video,
                                                              state: .on) { req in
        req.searchTag.contains("video")
    }

    private let panoramaFilterOn = FilterSpecFactory.checkFilter(name: "Только с панарамой",
                                                                 filter: .panorama,
                                                                 state: .on) { req in
        req.searchTag.contains("external_panoramas")
    }

    private let deliveryFilterOn = FilterSpecFactory.checkFilter(name: "Без доставки",
                                                                 filter: .delivery,
                                                                 state: .on) { req in
        req.withDelivery == .none
    }

    private let postingTimeFilter = FilterSpecFactory.selectFilter(name: "Срок размещения",
                                                                  filter: .postingTime,
                                                                  selectedOption: "3",
                                                                  resultString: "За 3 дня") { req in
        req.topDays == "3"
    }
}

fileprivate struct FilterStateCheck {
    let name: String
    let selectStrategy: (FiltersScreen_) -> Void
    let searchCheck: (Auto_Api_Search_SearchRequestParameters) -> Bool
}

fileprivate struct FilterSpecFactory {
    static func multiselectFilter(name: String,
                                  filter: FiltersScreen_.Element,
                                  selectedOptions: [String],
                                  resultString: String,
                                  searchCheck: @escaping (Auto_Api_Search_SearchRequestParameters) -> Bool) -> FilterStateCheck {
        FilterStateCheck(
            name: name,
            selectStrategy: { screen in
                screen.scroll(to: filter,
                              ofType: .filtersScreen,
                              windowInsets: .init(top: 0, left: 0, bottom: 168, right: 0))
                    .should(filter, .exist)
                    .tap(filter)
                    .should(provider: .optionSelectPicker, .exist)
                    .focus({ picker in
                        for selectedOption in selectedOptions {
                            picker.tap(.option(selectedOption))
                        }
                        picker.tap(.doneButton)
                    })
                    .should(filter, .match(resultString))
            },
            searchCheck: { req in
                searchCheck(req)
            })
    }

    static func selectFilter(name: String,
                             filter: FiltersScreen_.Element,
                             selectedOption: String,
                             resultString: String,
                             searchCheck: @escaping (Auto_Api_Search_SearchRequestParameters) -> Bool) -> FilterStateCheck {
        FilterStateCheck(
            name: name,
            selectStrategy: { screen in
                screen.scroll(to: filter, ofType: .filtersScreen)
                    .should(filter, .exist)
                    .tap(filter)
                    .should(provider: .optionSelectPicker, .exist)
                    .focus({ picker in
                        picker.tap(.option(selectedOption))
                    })
                    .should(filter, .match(resultString))
            },
            searchCheck: { req in
                searchCheck(req)
            })
    }

    static func segmentSelectFilter(name: String,
                             filter: FiltersScreen_.Element,
                             index: Int,
                             searchCheck: @escaping (Auto_Api_Search_SearchRequestParameters) -> Bool) -> FilterStateCheck {
        FilterStateCheck(
            name: name,
            selectStrategy: { screen in
                screen.scroll(to: filter, ofType: .filtersScreen)
                    .should(filter, .exist)
                    .focus(on: filter, ofType: .segmentSelectPicker, {
                        $0.tap(.segment(index))
                    })
            },
            searchCheck: { req in
                searchCheck(req)
            })
    }

    static func checkFilter(name: String,
                            filter: FiltersScreen_.Element,
                            state: UIElementState.BeState,
                            searchCheck: @escaping (Auto_Api_Search_SearchRequestParameters) -> Bool) -> FilterStateCheck {
        FilterStateCheck(
            name: name,
            selectStrategy: { screen in
                screen.scroll(to: filter, ofType: .filtersScreen)
                    .tap(filter)
                    .should(filter, .be(state))
            },
            searchCheck: { req in
                searchCheck(req)
            })
    }

    static func rangeFilter(name: String,
                            filter: FiltersScreen_.Element,
                            from: String,
                            to: String,
                            resultString: String,
                            searchCheck: @escaping (Auto_Api_Search_SearchRequestParameters) -> Bool) -> FilterStateCheck {
        FilterStateCheck(
            name: name,
            selectStrategy: { screen in
                screen.scroll(to: filter, ofType: .filtersScreen)
                    .should(filter, .exist)
                    .tap(filter)
                    .should(provider: .rangePicker, .exist)
                    .focus({ picker in
                        picker
                            .adjustWheel(to: from, in: .picker, wheelIndex: 0)
                        picker
                            .adjustWheel(to: to, in: .picker, wheelIndex: 1)
                        picker.tap(.doneButton)
                    })
                    .should(filter, .match(resultString))
            },
            searchCheck: { req in
                searchCheck(req)
            })
    }

    static func rangeFilter(name: String,
                            filter: FiltersScreen_.Element,
                            from: String? = nil,
                            to: String? = nil,
                            resultString: String,
                            searchCheck: @escaping (Auto_Api_Search_SearchRequestParameters) -> Bool) -> FilterStateCheck {
        FilterStateCheck(
            name: name,
            selectStrategy: { screen in
                screen.scroll(to: filter, ofType: .filtersScreen)
                    .should(filter, .exist)
                    .tap(filter)
                    .should(provider: .textRangePicker, .exist)
                    .focus({ picker in
                        if let from = from {
                            picker.tap(.from)
                            picker.type(from, in: .from)
                        }
                        if let to = to {
                            picker.tap(.to)
                            picker.type(to, in: .to)
                        }
                        picker.tap(.doneButton)
                    })
                    .should(filter, .match(resultString))
            },
            searchCheck: { req in
                searchCheck(req)
            })
    }
}
