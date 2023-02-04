//
//  FiltersTests+garage.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 11.08.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

extension FiltersTests {

    // MARK: - Rent. Garage

    func testRentGarage() {
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
            .category(.garage),
            .garageTypes,
            .price(Factory.makeSingleCasePrice()),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),

            // Удобства
            .boolParameters([
                .hasElectricitySupply,
                .hasHeatingSupply,
                .hasWaterSupply,
                .hasSecurity
            ]),

            // Условия сделки
            .boolParameters([
                .offerPropertyType,
                .noFee,
                .hasUtilitiesIncluded,
                .hasElectricityIncluded
            ]),

            // Дополнительно
            .boolParameters([
                .photoRequired,
                .hasVideo,
                .supportsOnlineView
            ]),

            .tagsToInclude,
            .tagsToExclude
        )
    }

    // MARK: - Buy. Garage

    func testBuyGarage() {
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
            .category(.garage),
            .garageTypes,
            .price(.multiple([
                Factory.makePrice(.perMeter),
                Factory.makePrice(.perOffer)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),

            // Удобства
            .boolParameters([
                .hasElectricitySupply,
                .hasHeatingSupply,
                .hasWaterSupply,
                .hasSecurity
            ]),

            // Условия сделки
            .boolParameters([
                .offerPropertyType
            ]),

            // Дополнительно
            .boolParameters([
                .photoRequired,
                .hasVideo,
                .supportsOnlineView
            ]),

            .tagsToInclude,
            .tagsToExclude
        )
    }
}
