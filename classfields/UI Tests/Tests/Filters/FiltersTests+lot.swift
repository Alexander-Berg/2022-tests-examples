//
//  FiltersTests+lot.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 11.08.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

extension FiltersTests {

    // MARK: - Buy. Lot

    func testBuyLot() {
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
            .category(.lot),
            .price(.multiple([
                Factory.makePrice(.perOffer),
                Factory.makePrice(.perAre)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),

            // Участок
            .numberRange([
                Factory.makeNumberRange(.lotAreaWithSeparator)
            ]),
            .multiParameters([.lotType]),

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

    func testBuyVillageLot() {
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
            .category(.lot)
        )
        villageSubtests.run(
            .objectType(.village),
            .price(.multiple([
                Factory.makePrice(.perOffer),
                Factory.makePrice(.perAre)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),

            // Объект
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
            .boolParameters([
                .hasRailwayStation,
            ])
        )
    }

    func testBuySecondaryLot() {
        GeoAPIStubConfigurator.setupRegionInfo_Moscow(using: self.dynamicStubs)

        self.relaunchApp(with: .filterUITests)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let subtests = FiltersSubtests(stubs: self.dynamicStubs, anyOfferKind: .offer)

        searchResultsSteps.openFilters()
        filtersSteps
            .isScreenPresented()

        subtests.run(
            .action(.buy),
            .category(.lot),
            .objectType(.secondary),
            .price(.multiple([
                Factory.makePrice(.perOffer),
                Factory.makePrice(.perAre)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),

            // Участок
            .numberRange([
                Factory.makeNumberRange(.lotAreaWithSeparator)
            ]),
            .multiParameters([.lotType]),

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
