//
//  FiltersTests+house.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 11.08.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

extension FiltersTests {

    // MARK: - Buy. House

    func testBuyHouse() {
        GeoAPIStubConfigurator.setupRegionInfo_Moscow(using: self.dynamicStubs)

        self.relaunchApp(with: .filterUITests)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let subtests = FiltersSubtests(stubs: self.dynamicStubs, anyOfferKind: .offer)

        searchResultsSteps.openFilters()
        filtersSteps
            .isScreenPresented()

        subtests.run(
            // Главный блок
            .action(.buy),
            .category(.house),
            .price(.multiple([
                Factory.makePrice(.perOffer),
                Factory.makePrice(.perMeter)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),

            // Дом, участок
            .multipleSelectParameters([.houseType, .buyRenovation]),
            .numberRange([
                Factory.makeNumberRange(.houseArea),
                Factory.makeNumberRange(.lotArea),
            ]),
            .multipleSelectParameters([.buildingType]),
            .numberRange([
                Factory.makeNumberRange(.houseFloors),
            ]),

            // Коммуникации
            .boolParameters([
                .hasElectricitySupply,
                .hasGasSupply,
                .hasWaterSupply,
                .hasSewerageSupply,
                .hasHeatingSupply
            ]),

            // Район
            .multipleSelectParameters([.parkType, .pondType]),
            .boolParameters([.expectMetro]),

            // Дополнительно
            .boolParameters([
                .offerPropertyType,
                .photoRequired,
                .hasVideo,
                .supportsOnlineView
            ]),

            .tagsToInclude,
            .tagsToExclude
        )
    }

    func testBuyVillageHouse() {
        GeoAPIStubConfigurator.setupRegionInfo_Moscow(using: self.dynamicStubs)

        self.relaunchApp(with: .filterUITests)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        let offerSubtests = FiltersSubtests(stubs: self.dynamicStubs, anyOfferKind: .offer)
        let villageSubtests = FiltersSubtests(stubs: self.dynamicStubs, anyOfferKind: .village)

        searchResultsSteps.openFilters()
        filtersSteps
            .isScreenPresented()

        offerSubtests.run(
            .action(.buy),
            .category(.house)
        )
        villageSubtests.run(
            .objectType(.village),
            .villageOfferTypes(
                FiltersSubtests.VillageOfferType.allCases,
                withPriceConfigurations: [
                    Factory.makePrice(.perOffer)
                ]
            ),
            .villageOfferTypes(
                [.cottage, .townhouse],
                withPriceConfigurations: [
                    Factory.makePrice(.perMeter),
                    Factory.makePrice(.perOffer, from: "100", to: "200")
                ]
            ),
            .villageOfferTypes(
                [.cottage, .land],
                withPriceConfigurations: [
                    // different price to avoid test failure
                    // @see https://st.yandex-team.ru/VSAPPS-7946
                    Factory.makePrice(.perOffer, from: "101", to: "201")
                ]
            ),
            .villageOfferTypes(
                [.land],
                withPriceConfigurations: [
                    Factory.makePrice(.perAre),
                    Factory.makePrice(.perOffer)
                ]
            ),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),

            // Объект
            .numberRange([
                Factory.makeNumberRange(.villageHouseArea)
            ]),
            .multipleSelectParameters([.wallsType]),
            .numberRange([
                Factory.makeNumberRange(.lotArea)
            ]),

            // Коммуникации
            .boolParameters([
                .hasElectricitySupply,
                .hasGasSupply,
                .hasWaterSupply,
                .hasSewerageSupply,
                .hasHeatingSupply
            ]),

            // Посёлок
            .singleSelectParameters([.deliveryDate]),
            .multipleSelectParameters([.villageClass]),
            .villageDeveloper,
            .multipleSelectParameters([.landType]),

            // Район
            .multipleSelectParameters([.parkType, .pondType]),
            .boolParameters([.hasRailwayStation])
        )
    }

    func testBuySecondaryHouse() {
        GeoAPIStubConfigurator.setupRegionInfo_Moscow(using: self.dynamicStubs)

        self.relaunchApp(with: .filterUITests)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let subtests = FiltersSubtests(stubs: self.dynamicStubs, anyOfferKind: .offer)

        searchResultsSteps.openFilters()
        filtersSteps
            .isScreenPresented()

        subtests.run(
            // Главный блок
            .action(.buy),
            .category(.house),
            .objectType(.secondary),
            .price(.multiple([
                Factory.makePrice(.perOffer),
                Factory.makePrice(.perMeter)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),

            // Дом, участок
            .multipleSelectParameters([.houseType, .buyRenovation]),
            .numberRange([
                Factory.makeNumberRange(.houseArea),
                Factory.makeNumberRange(.lotArea),
            ]),
            .multipleSelectParameters([.buildingType]),
            .numberRange([
                Factory.makeNumberRange(.houseFloors),
            ]),

            // Коммуникации
            .boolParameters([
                .hasElectricitySupply,
                .hasGasSupply,
                .hasWaterSupply,
                .hasSewerageSupply,
                .hasHeatingSupply
            ]),

            // Район
            .multipleSelectParameters([.parkType, .pondType]),
            .boolParameters([.expectMetro]),

            // Дополнительно
            .boolParameters([
                .offerPropertyType,
                .photoRequired,
                .hasVideo,
                .supportsOnlineView
            ]),

            .tagsToInclude,
            .tagsToExclude
        )
    }

    // MARK: - Rent. House

    func testLongRentHouse() {
        self.relaunchApp(with: .filterUITests)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let subtests = FiltersSubtests(stubs: self.dynamicStubs, anyOfferKind: .offer)

        searchResultsSteps.openFiltersIfNeeded()
        filtersSteps
            .isScreenPresented()

        subtests.run(
            // Главный блок
            .action(.rent),
            .category(.house),
            // .rentTime(.long), Default value, not set directly
            .price(Factory.makeSingleCasePrice()),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),

            // Дом, участок
            .multipleSelectParameters([.houseType]),
            .numberRange([
                Factory.makeNumberRange(.houseArea),
                Factory.makeNumberRange(.lotArea),
            ]),
            .multipleSelectParameters([.buildingType]),
            .numberRange([
                Factory.makeNumberRange(.houseFloors),
            ]),

            // Район
            .multipleSelectParameters([.parkType, .pondType]),
            .boolParameters([.expectMetro]),

            // Дополнительно
            .boolParameters([
                .offerPropertyType,
                .hasVideo,
                .supportsOnlineView
            ]),

            .tagsToInclude,
            .tagsToExclude
        )
    }

    func testShortRentHouse() {
        self.relaunchApp(with: .filterUITests)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let subtests = FiltersSubtests(stubs: self.dynamicStubs, anyOfferKind: .offer)

        searchResultsSteps.openFilters()
        filtersSteps
            .isScreenPresented()

        subtests.run(
            // Главный блок
            .action(.rent),
            .category(.house),
            .rentTime(.short),
            .price(Factory.makeSingleCasePrice()),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),

            // Дом, участок
            .multipleSelectParameters([.houseType, .buyRenovation]),
            .numberRange([
                Factory.makeNumberRange(.houseArea),
                Factory.makeNumberRange(.lotArea),
            ]),
            .multipleSelectParameters([.buildingType]),
            .numberRange([
                Factory.makeNumberRange(.houseFloors),
            ]),

            // Район
            .multipleSelectParameters([.parkType, .pondType]),
            .boolParameters([.expectMetro]),

            // Дополнительно
            .boolParameters([
                .offerPropertyType,
                .hasVideo,
                .supportsOnlineView
            ]),

            .tagsToInclude,
            .tagsToExclude
        )
    }
}
