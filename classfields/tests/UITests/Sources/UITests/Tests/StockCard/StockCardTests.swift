//
//  StockCardTests.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 12.04.2021.
//

import XCTest
import AutoRuProtoModels
import SwiftProtobuf
import Snapshots

/// @depends_on AutoRuStockCard
class StockCardTests: BaseTest {
    private static let requestTimeout = 10.0
    let suiteName = SnapshotIdentifier.suiteName(from: #file)

    lazy var mainSteps = MainSteps(context: self)
    override var appSettings: [String: Any] {
        var value = super.appSettings
        value["otherDealerOffersCount"] = 1
        return value
    }

    override func setUp() {
        super.setUp()
        setupServer()
        launch()
    }

    // MARK: - Helpers

    private func setupServer() {
        server.forceLoginMode = .forceLoggedIn

        server.addHandler("POST /device/hello") { (request, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        server.addHandler("POST /search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc") { (request, _) -> Response? in
            return Response.okResponse(fileName: "best_offers_search_cars")
        }

        server.addHandler("POST /search/cars/mark-model-filters?geo_radius=0&&search_tag=match_applications", { _, _ in
            return Response.okResponse(fileName: "best_offers_mark_model_filters")
        })

        server.addHandler("POST /search/cars?context=default&group_by=CONFIGURATION&page=1&page_size=1&sort=fresh_relevance_1-desc", { _, _  in
            return Response.okResponse(fileName: "best_offers_search_cars_configuration")
        })

        server.addHandler("POST /search/cars?context=default&group_by=COMPLECTATION_NAME&page=1&page_size=100&sort=fresh_relevance_1-desc", { _, _ in
            return Response.okResponse(fileName: "best_offers_search_complectations")
        })

        server.addHandler("POST /search/cars?context=group_card&page=1&page_size=20&sort=fresh_relevance_1-desc", { _, _  in
            return Response.okResponse(fileName: "best_offers_search_cars_listing")
        })

        server.addHandler("GET /reference/catalog/cars/configurations/subtree?configuration_id=21574746", { _, _ in
            return Response.okResponse(fileName: "best_offers_reference_catalog_cars_configurations_subtree")
        })

        server.addHandler("GET /reference/catalog/cars/all-options", { _, _ in
            return Response.okResponse(fileName: "best_offers_reference_catalog_cars_all-options")
        })

        server.addHandler("POST /search/CARS/equipment-filters", { _, _ in
            return Response.okResponse(fileName: "best_offers_search_cars_equipment-filters")
        })

        server.addHandler("GET /offer/cars/1/phones", { _, _ in
            return Response.okResponse(fileName: "best_offers_phones")
        })

        try! server.start()
    }

    func test_inStock() {
        checkFilterWithQuickFilterRepresentation(filterType: .inStock,
                                                 quickFilterType: .inStock,
                                                 selectedValue: nil,
                                                 expectedQuickFilterTitle: "В наличии")
    }

    func test_color() {
        checkFilterWithQuickFilterRepresentation(filterType: .color,
                                                 quickFilterType: .color,
                                                 selectedValue: "Синий",
                                                 expectedQuickFilterTitle: "Синий")
    }

    func test_transmission() {
        checkFilterWithQuickFilterRepresentation(filterType: .transmission,
                                                 quickFilterType: .transmission,
                                                 selectedValue: "Робот",
                                                 expectedQuickFilterTitle: "Робот")
    }

    func test_gear() {
        checkFilterWithQuickFilterRepresentation(filterType: .gear,
                                                 quickFilterType: .gear,
                                                 selectedValue: "Передний",
                                                 expectedQuickFilterTitle: "Передний")
    }

    func test_engine() {
        checkFilterWithQuickFilterRepresentation(filterType: .engine,
                                                 quickFilterType: .engine,
                                                 selectedValue: "Бензин 1.5 л, 140 л.с.",
                                                 confirmFilterLabel: "callButtonHelper",
                                                 expectedQuickFilterTitle: "Бензин\u{00a0}/\u{00a0}1.5 л\u{00a0}/\u{00a0}140 л.с.")
    }

    func test_complectation() {
        checkFilterWithQuickFilterRepresentation(filterType: .equipment,
                                                 quickFilterType: .complectation,
                                                 selectedValue: "Рейлинги на крыше",
                                                 confirmFilterLabel: "OptionFilterApplyButton",
                                                 checkScreen: false,
                                                 expectedQuickFilterTitle: "1\u{00a0}опция")
    }

    func test_mapFuelRateToTechParam() {
        routeToListing()
            .openFilters()
            .scrollToField(.fuelRate)
            .tapOnField(.fuelRate)
            .enterToRangeInput(.to, "5")
            .confirmFilters()
            .showResultsTap()
            .openStockCardOffer(offersTitle: "88 предложений")
            .scrollToFilterButton(.engine)
            .exist(selector: "Дизель\u{00a0}/\u{00a0}2.0 л\u{00a0}/\u{00a0}150 л.с.")
    }

    func test_mapAccelerationToTechParam() {
        routeToListing()
            .openFilters()
            .scrollToField(.acceleration)
            .tapOnField(.acceleration)
            .selectRange(.to, "9")
            .confirmFilters()
            .showResultsTap()
            .openStockCardOffer(offersTitle: "88 предложений")
            .scrollToFilterButton(.engine)
            .exist(selector: "Бензин\u{00a0}/\u{00a0}2.0 л\u{00a0}/\u{00a0}192 л.с.")
    }

    func test_mapEngineTypeToTechParam() {
        routeToListing()
            .openFilters()
            .scrollToField(.engine)
            .tapOnField(.engine)
            .tap("Дизель").as(FiltersSteps.self)
            .confirmFilters()
            .showResultsTap()
            .openStockCardOffer(offersTitle: "88 предложений")
            .scrollToFilterButton(.engine)
            .exist(selector: "Дизель\u{00a0}/\u{00a0}2.0 л\u{00a0}/\u{00a0}150 л.с.")
    }

    func test_mapEngineVolumeTypeToTechParam() {
        routeToListing()
            .openFilters()
            .scrollToField(.engineVolume)
            .tapOnField(.engineVolume)
            .selectRange(.to, "1.5")
            .confirmFilters()
            .showResultsTap()
            .openStockCardOffer(offersTitle: "88 предложений")
            .scrollToFilterButton(.engine)
            .exist(selector: "Бензин\u{00a0}/\u{00a0}1.5 л\u{00a0}/\u{00a0}140 л.с.")
    }

    func test_mapEnginePowerTypeToTechParam() {
        routeToListing()
            .openFilters()
            .scrollToField(.power)
            .tapOnField(.power)
            .selectRange(.to, "140")
            .confirmFilters()
            .showResultsTap()
            .openStockCardOffer(offersTitle: "88 предложений")
            .scrollToFilterButton(.engine)
            .exist(selector: "Бензин\u{00a0}/\u{00a0}1.5 л\u{00a0}/\u{00a0}140 л.с.")
    }

    func test_shouldLogFrontlogWhenOpenCardFromInStockListing() {
        mocker.mock_eventsLog()
        let frontlogExpectation = expectationForRequest(
            method: "POST",
            uri: "/events/log",
            requestChecker: { (req: Auto_Api_EventsReportRequest) -> Bool in
                return req.events.contains(
                    where: {
                        $0.cardShowEvent.cardID == "1"
                            && $0.cardShowEvent.hasAppVersion
                            && $0.cardShowEvent.category == AutoRuProtoModels.Auto_Api_Category.cars
                            && $0.cardShowEvent.contextBlock == AutoRuProtoModels.Auto_Api_ContextBlock.blockListing
                            && $0.cardShowEvent.contextPage == AutoRuProtoModels.Auto_Api_ContextPage.pageGroup
                            && $0.cardShowEvent.contextService == AutoRuProtoModels.Auto_Api_ContextService.serviceAutoru
                            && $0.cardShowEvent.index == 0
                            && $0.cardShowEvent.groupSize == 93
                            && $0.cardShowEvent.groupingID == "mark=BMW,model=X1,generation=21574693,configuration=21574746"
                            && $0.cardShowEvent.hasSearchQueryID
                            && $0.cardShowEvent.section == AutoRuProtoModels.Auto_Api_Section.new
                    }
                )
            }
        )

        routeToStockListing()
            .openOffer(with: "1")

        Step("Проверяем, что отправили событие card_view_event во фронтлог с ожидаемыми параметрами") {
            wait(for: [frontlogExpectation], timeout: 2.0)
        }
    }

    func test_stockCardElements() {
        api.search.cars
            .post(parameters: .parameters([
                .context("group_card"),
                .page(1),
                .pageSize(20),
                .sort("fresh_relevance_1-desc")
            ]))
            .ok(mock: .file("stock_card_search_cars"))

        api.reference.catalog.cars.configurations.subtree
            .get(parameters: .parameters([.configurationId(["21574746"])]))
            .ok(mock: .file("stock_card_subtree"))

        api.search.category(.cars).related
            .post(parameters: .parameters([._unknown("group_by", "TECHPARAM"), ._unknown("group_by", "COMPLECTATION"), .page(1), .pageSize(10)]))
            .ok(mock: .file("stock_card_related"))

        api.search.category(.cars).specials
            .post(parameters: .parameters([.page(1), .pageSize(4)]))
            .ok(mock: .file("stock_card_specials"))

        api.user.favorites.category(.cars).subscriptions
            .post
            .ok(mock: .file("success"))

        api.user.compare.category(.cars).catalogCardId(["21574746__21660669"])
            .put
            .ok(mock: .file("success"))


        routeToStockListing()
            .step("Проверяем иконку для добавления в сравнение моделей") { screen in
                screen
                    .focus(on: .compareButton(compare: false), ofType: .stockCardScreen) {
                        $0.validateSnapshot(snapshotId: "test_stockCardElements_compareButton_gray")
                    }
                    .do {
                        api.user.compare.category(.cars).models
                            .get(parameters: .parameters([]))
                            .ok(mock: .file("stock_card_compare_model"))
                    }
                    .tap(.compareButton(compare: false))
                    .focus(on: .compareButton(compare: true), ofType: .stockCardScreen) {
                        $0.validateSnapshot(snapshotId: "test_stockCardElements_compareButton_red")
                    }
            }
            .step("Проверяем сердечко для добавления в сохраненные поиски") { screen in
                screen
                    .focus(on: .saveSearchButton(saved: false), ofType: .stockCardScreen) {
                        $0.validateSnapshot(snapshotId: "test_stockCardElements_searchButton_gray")
                        $0.tap()
                    }
                    .focus(on: .saveSearchButton(saved: true), ofType: .stockCardScreen) {
                        $0.validateSnapshot(snapshotId: "test_stockCardElements_searchButton_red")
                    }
            }
            .step("Проверяем количество предложений") { screen in
                screen
                    .should(.offerCount(4), ofType: .stockCardScreen, .exist)
            }
            .step("Проверяем наличие сортировки") { screen in
                screen
                    .should(.sorting, ofType: .stockCardScreen, .exist)
            }
            .step("Проверяем отображение Предложения дня") { screen in
                screen
                    .scroll(to: .offerOfTheDay, ofType: .stockCardScreen)
                    .should(.offerOfTheDay, .exist)
            }
            .step("Проверяем отображение похожих автомобилей и скролл") { screen in
                screen
                    .should(.relatedCars, ofType: .stockCardScreen, .exist)
                    .focus(on: .relatedCar("BMW 5 серии"), ofType: .stockCardScreen) {
                        $0.swipe(.left)
                    }
                    .should(.relatedCar("Lexus ES"), .exist)
            }
    }

    private func checkFilterWithQuickFilterRepresentation(filterType: FiltersScreen.Field,
                                                          quickFilterType: StockCardScreen.FilterType,
                                                          selectedValue: String?,
                                                          confirmFilterLabel: String = "Готово",
                                                          checkScreen: Bool = true,
                                                          expectedQuickFilterTitle: String) {
        let step = routeToStockListing()
            .openFullFilter()
            .scrollToField(filterType)
            .tapOnField(filterType)
        if let selectedValue = selectedValue {
            step
                .tap(selectedValue)
            if checkScreen {
                step
                    .validateSnapshot(of: "FiltersViewController", snapshotId: "open_\(filterType.rawValue)_onFiltersViewController")
            }
            step
                .tap(confirmFilterLabel)
        }
        step
            .showResultsTap().as(StockCardSteps.self)
            .scrollToFilterButton(quickFilterType)
            .exist(selector: expectedQuickFilterTitle)
    }

    @discardableResult
    private func routeToStockListing() -> StockCardSteps {
        return routeToListing()
            .openStockCardOffer(offersTitle: "88 предложений")
    }

    private func routeToListing() -> SaleCardListSteps {
        return mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .wait(for: 1)
            .scrollToStockOffer(with: "1", position: .body)
    }
}
