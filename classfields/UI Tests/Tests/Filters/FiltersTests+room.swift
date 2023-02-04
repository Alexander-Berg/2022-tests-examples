//
//  FiltersTests+room.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 11.08.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

extension FiltersTests {

    // MARK: - Rent. Room

    func testShortRentRoom() {
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
            .category(.room),
            .rentTime(.short),
            .price(Factory.makeSingleCasePrice()),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),

            // Квартира
            .multipleSelectParameters([.roomsCount]),
            .numberRange([
                Factory.makeNumberRange(.livingSpace)
            ]),
            .singleSelectParameters([.minCeilingHeight]),
            .multipleSelectParameters([.rentRenovation]),
            .numberRange([
                Factory.makeNumberRange(.floor)
            ]),
            .singleSelectParameters([.lastFloor]),
            .boolParameters([.exceptFirstFloor]),
            .singleSelectParameters([.hasFurniture]),

            // Удобства
            .boolParameters([
                .hasFridge,
                .dishwasher,
                .aircondition,
                .hasTV,
                .hasWashingMachine,
                .animalAllowed,
                .kidsAllowed
            ]),

            // Дом
            .multipleSelectParameters([
                .buildingConstructionType,
                .buildingEpochType
            ]),
            .numberRange([
                Factory.makeNumberRange(.builtYear),
                Factory.makeNumberRange(.houseFloors),
            ]),
            .multipleSelectParameters([.parkingType]),
            .singleSelectParameters([.cityRenovation]),

            // Район
            .multipleSelectParameters([.parkType, .pondType]),
            .boolParameters([.expectMetro]),

            // Дополнительно
            .boolParameters([
                .offerPropertyType,
                .noFee,
                .hasVideo,
                .supportsOnlineView
            ]),

            .tagsToInclude,
            .tagsToExclude
        )
    }

    func testLongRentRoom() {
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
            .category(.room),
            // .rentTime(.long), Default value, not set directly
            .price(Factory.makeSingleCasePrice()),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),

            // Квартира
            .multipleSelectParameters([.roomsCount]),
            .numberRange([
                Factory.makeNumberRange(.livingSpace)
            ]),
            .singleSelectParameters([.minCeilingHeight]),
            .multipleSelectParameters([.rentRenovation]),
            .numberRange([
                Factory.makeNumberRange(.floor)
            ]),
            .singleSelectParameters([.lastFloor]),
            .boolParameters([.exceptFirstFloor]),
            .singleSelectParameters([.hasFurniture]),

            // Удобства
            .boolParameters([
                .hasFridge,
                .dishwasher,
                .aircondition,
                .hasTV,
                .hasWashingMachine,
                .animalAllowed,
                .kidsAllowed
            ]),

            // Дом
            .multipleSelectParameters([
                .buildingConstructionType,
                .buildingEpochType
            ]),
            .numberRange([
                Factory.makeNumberRange(.builtYear),
                Factory.makeNumberRange(.houseFloors),
            ]),
            .multipleSelectParameters([.parkingType]),
            .singleSelectParameters([.cityRenovation]),

            // Район
            .multipleSelectParameters([.parkType, .pondType]),
            .boolParameters([.expectMetro]),

            // Дополнительно
            .boolParameters([
                .noFee,
                .hasVideo,
                .supportsOnlineView,
                .offerPropertyType
            ]),

            .tagsToInclude,
            .tagsToExclude
        )
    }

    // MARK: - Buy. Room

    func testBuyRoom() {
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
            .category(.room),
            .price(.multiple([
                Factory.makePrice(.perOffer),
                Factory.makePrice(.perMeter)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),

            // Квартира
            .numberRange([
                Factory.makeNumberRange(.livingSpace)
            ]),
            .multipleSelectParameters([.roomsCount]),
            .numberRange([
                Factory.makeNumberRange(.kitchenArea),
            ]),
            .singleSelectParameters([.minCeilingHeight]),
            .multipleSelectParameters([.buyRenovation]),
            .singleSelectParameters([.bathroomType]),
            .numberRange([
                Factory.makeNumberRange(.floor)
            ]),
            .singleSelectParameters([.lastFloor]),
            .numberRange([
                Factory.makeNumberRange(.builtYear)
            ]),
            .boolParameters([.exceptFirstFloor]),

            // Дом
            .multipleSelectParameters([
                .buildingConstructionType,
                .buildingEpochType
            ]),
            .numberRange([
                Factory.makeNumberRange(.houseFloors),
            ]),
            .multipleSelectParameters([.parkingType]),
            .singleSelectParameters([.cityRenovation]),

            // Район
            .multipleSelectParameters([.parkType, .pondType]),
            .boolParameters([.expectMetro]),

            // Дополнительно
            .boolParameters([
                .photoRequired,
                .offerPropertyType,
                .hasVideo,
                .supportsOnlineView
            ]),

            .tagsToInclude,
            .tagsToExclude
        )
    }
}
