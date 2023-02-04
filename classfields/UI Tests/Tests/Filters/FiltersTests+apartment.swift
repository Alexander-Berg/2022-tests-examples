//
//  FiltersTests+apartment.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 11.08.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig

extension FiltersTests {

    // MARK: - Rent. Apartment

    func testLongRentApartment() {
        let configuration = ExternalAppConfiguration.filterUITests
        configuration.geoData.geoIntent = .empty
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let subtests = FiltersSubtests(stubs: self.dynamicStubs, anyOfferKind: .offer)

        searchResultsSteps.openFilters()
        filtersSteps
            .isScreenPresented()

        subtests.run(
            // Главный блок
            .action(.rent),
            .category(.apartment),
            // .rentTime(.long), Default value, not set directly
            .totalRooms,
            .price(Factory.makeSingleCasePrice()),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),

            // Квартира
            .numberRange([
                Factory.makeNumberRange(.totalArea),
                Factory.makeNumberRange(.kitchenArea),
            ]),
            .singleSelectParameters([.minCeilingHeight]),
            .multipleSelectParameters([.rentRenovation]),
            .singleSelectParameters([.balconyType]),
            .numberRange([
                Factory.makeNumberRange(.floor),
            ]),
            .singleSelectParameters([.lastFloor]),
            .boolParameters([.exceptFirstFloor]),
            .singleSelectParameters([.furniture]),

            // Удобства
            .boolParameters([
                .fridge,
                .dishwasher,
                .aircondition,
                .tv,
                .washingMachine,
                .animal,
                .kids
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

    func testShortRentApartment() {
        let configuration = ExternalAppConfiguration.filterUITests
        configuration.geoData.geoIntent = .fromRegion
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let subtests = FiltersSubtests(stubs: self.dynamicStubs, anyOfferKind: .offer)

        searchResultsSteps.openFilters()
        filtersSteps
            .isScreenPresented()

        subtests.run(
            // Главный блок
            .action(.rent),
            .category(.apartment),
            .rentTime(.short),
            .price(Factory.makeSingleCasePrice()),
            .totalRooms,

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [.drawGeoIntent, .commuteGeoIntent])),
            .metroDistance(Factory.metroDistanceOptions),

            // Квартира
            .numberRange([
                Factory.makeNumberRange(.totalArea),
                Factory.makeNumberRange(.kitchenArea),
            ]),
            .singleSelectParameters([.minCeilingHeight]),
            .multipleSelectParameters([.rentRenovation]),
            .singleSelectParameters([.balconyType]),
            .numberRange([
                Factory.makeNumberRange(.floor),
            ]),
            .singleSelectParameters([.lastFloor]),
            .boolParameters([.exceptFirstFloor]),
            .singleSelectParameters([.furniture]),

            // Удобства
            .boolParameters([
                .fridge,
                .dishwasher,
                .aircondition,
                .tv,
                .washingMachine,
                .animal,
                .kids
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
            .buildingSeries,

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

    // MARK: - Buy. Apartment

    func testBuyApartment() {
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
            .category(.apartment),
            .totalRooms,
            .price(.multiple([
                Factory.makePrice(.perMeter),
                Factory.makePrice(.perOffer)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),

            // Квартира
            .numberRange([
                Factory.makeNumberRange(.totalArea),
                Factory.makeNumberRange(.kitchenArea)
            ]),
            .singleSelectParameters([.minCeilingHeight]),
            .multipleSelectParameters([.buyRenovation]),
            .singleSelectParameters([.balconyType, .bathroomType]),
            .numberRange([
                Factory.makeNumberRange(.floor),
            ]),
            .singleSelectParameters([.lastFloor]),
            .boolParameters([.exceptFirstFloor]),
            .singleSelectParameters([.furniture]),

            // Дом
            .multipleSelectParameters([
                .buildingConstructionType,
                .buildingEpochType
            ]),
            .numberRange([
                Factory.makeNumberRange(.builtYear),
                Factory.makeNumberRange(.houseFloors),
            ]),
            .singleSelectParameters([.livingApartmentType]),
            .multipleSelectParameters([.parkingType]),
            .singleSelectParameters([.cityRenovation]),

            // Район
            .multipleSelectParameters([.parkType, .pondType]),
            .boolParameters([.expectMetro]),

            // Условия сделки
            .singleSelectParameters([.dealStatusBuyApartament]),

            // Дополнительно
            .boolParameters([
                .photoRequired,
                .supportsOnlineView,
                .hasVideo,
                .offerPropertyType,
                .withExcerptsOnly
            ]),

            .tagsToInclude,
            .tagsToExclude
        )
    }

    func testBuyNewbuildingApartment() {
        self.relaunchApp(with: .filterUITests)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        let offerSubtests = FiltersSubtests(stubs: self.dynamicStubs, anyOfferKind: .offer)
        let siteSubtests = FiltersSubtests(stubs: self.dynamicStubs, anyOfferKind: .site)

        searchResultsSteps.openFilters()
        filtersSteps
            .isScreenPresented()

        offerSubtests.run(
            .action(.buy),
            .category(.apartment)
        )
        siteSubtests.run(
            .apartmentType(.newbuilding),
            .totalRooms,
            .price(.multiple([
                Factory.makePrice(.perMeter),
                Factory.makePrice(.perOffer)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),

            // Квартира
            .numberRange([
                Factory.makeNumberRange(.totalArea),
                Factory.makeNumberRange(.kitchenArea)
            ]),
            .singleSelectParameters([.minCeilingHeight]),
            .multipleSelectParameters([.decoration]),
            .singleSelectParameters([.bathroomType]),
            .numberRange([
                Factory.makeNumberRange(.floor),
            ]),

            // Жилой комплекс
            .singleSelectParameters([.deliveryDate]),
            .multipleSelectParameters([.buildingConstructionType]),
            .multipleSelectParameters([.buildingClass]),
            .numberRange([
                Factory.makeNumberRange(.houseFloors),
            ]),
            .singleSelectParameters([.livingApartmentType]),
            .multipleSelectParameters([.parkingType]),
            .onlySamolet,
            .developer,

            // Район
            .multipleSelectParameters([.parkType, .pondType]),
            .boolParameters([.expectMetro]),

            // Условия сделки
            .boolParameters([
                .hasSpecialProposal,
                .hasSiteMortgage,
                .hasInstallment,
                .fz214,
                .hasSiteMaternityFunds,
                .hasMilitarySiteMortgage
            ]),

            // Дополнительно
            .boolParameters([.showOutdated])
        )
    }

    func testBuySecondaryApartment() {
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
            .category(.apartment),
            .apartmentType(.secondary),
            .totalRooms,
            .price(.multiple([
                Factory.makePrice(.perMeter),
                Factory.makePrice(.perOffer)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),

            // Квартира
            .numberRange([
                Factory.makeNumberRange(.totalArea),
                Factory.makeNumberRange(.kitchenArea)
            ]),
            .singleSelectParameters([.minCeilingHeight]),
            .multipleSelectParameters([.buyRenovation]),
            .singleSelectParameters([.balconyType, .bathroomType]),
            .numberRange([
                Factory.makeNumberRange(.floor),
            ]),
            .singleSelectParameters([.lastFloor]),
            .boolParameters([.exceptFirstFloor]),

            // Дом
            .multipleSelectParameters([
                .buildingConstructionType,
                .buildingEpochType
            ]),
            .numberRange([
                Factory.makeNumberRange(.builtYear),
                Factory.makeNumberRange(.houseFloors),
            ]),
            .singleSelectParameters([.livingApartmentType]),
            .multipleSelectParameters([.parkingType]),
            .singleSelectParameters([.cityRenovation]),

            // Район
            // FIXME: @rinatenikeev add .expectMetro (fails currently)
            .multipleSelectParameters([.parkType, .pondType]),

            // Условия сделки
            .singleSelectParameters([.dealStatusBuyApartament]),

            // Дополнительно
            .boolParameters([
                .photoRequired,
                .hasVideo,
                .supportsOnlineView,
                .offerPropertyType,
                .withExcerptsOnly
            ]),

            .tagsToInclude,
            .tagsToExclude
        )
    }
}
