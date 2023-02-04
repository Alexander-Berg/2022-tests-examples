//
//  FiltersTests+commercial+buy.swift
//  UI Tests
//
//  Created by Erik Burygin on 21.09.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

// swiftlint:disable file_length
extension FiltersTests {

    // MARK: - Buy. Commercial

    func testBuyCommercialAll() {
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
            .category(.commercial),
            .commercialTypeCancellation,
            .commercialType(.noMatter),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perMeter),
                Factory.makePrice(.perOffer)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),
            // TODO: @d0br0 subtest 'Время до метро на транспорте' is missed

            // Объект
            .singleSelectParameters([.entranceType]),

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

    func testBuyCommercialLand() {
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
            .category(.commercial),
            .commercialTypeCancellation,
            .commercialType(.land),
            .numberRange([
                Factory.makeNumberRange(.commercialLotArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perOffer),
                Factory.makePrice(.perAre)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),
            // TODO: @d0br0 subtest 'Время до метро на транспорте' is missed

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

    func testBuyCommercialOthers() {
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
            .category(.commercial),
            .commercialType(.others([
                .office,
                .retail,
                .freePurpose,
                .warehouse,
                .manufacturing,
                .publicCatering,
                .autoRepair,
                .hotel,
                .business,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perOffer),
                Factory.makePrice(.perMeter)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),
            // TODO: @d0br0 subtest 'Время до метро на транспорте' is missed

            // Объект
            .singleSelectParameters([.entranceType]),
            .multipleSelectParameters([.commercialRenovation]),
            .singleSelectParameters([.commercialPlanType]),
            .boolParameters([.hasVentilation]),
            .singleSelectParameters([.furniture]),
            .boolParameters([.aircondition]),

            // Здание
            .singleSelectParameters([.entranceType]),
            .singleSelectParameters([
                .commercialBuildingType([
                    .noMatter,
                    .warehouse,
                    .shoppingCenter,
                    .detachedBuilding,
                    .residentalBuilding,
                ])
            ]),
            .multipleSelectParameters([.officeClass]),
            .boolParameters([
                .hasTwentyFourSeven,
                .hasParking
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

    func testBuyCommercialOffice() {
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
            .category(.commercial),
            .commercialType(.others([
                .office,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perOffer),
                Factory.makePrice(.perMeter)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),
            // TODO: @d0br0 subtest 'Время до метро на транспорте' is missed

            // Объект
            .multipleSelectParameters([.commercialRenovation]),
            .singleSelectParameters([.commercialPlanType]),
            .boolParameters([.hasVentilation]),
            .singleSelectParameters([.furniture]),
            .boolParameters([.aircondition]),

            // Здание
            .singleSelectParameters([
                .commercialBuildingType([
                    .noMatter,
                    .warehouse,
                    .shoppingCenter,
                    .detachedBuilding,
                    .residentalBuilding,
                ])
            ]),
            .multipleSelectParameters([.officeClass]),
            .boolParameters([
                .hasTwentyFourSeven,
                .hasParking
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

    func testBuyCommercialRetail() {
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
            .category(.commercial),
            .commercialType(.others([
                .retail,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perOffer),
                Factory.makePrice(.perMeter)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),
            // TODO: @d0br0 subtest 'Время до метро на транспорте' is missed

            // Объект
            .singleSelectParameters([.entranceType]),
            .multipleSelectParameters([.commercialRenovation]),
            .boolParameters([.hasVentilation]),
            .singleSelectParameters([.furniture]),
            .boolParameters([.aircondition]),

            // Здание
            .singleSelectParameters([
                .commercialBuildingType([
                    .noMatter,
                    .warehouse,
                    .shoppingCenter,
                    .detachedBuilding,
                    .residentalBuilding,
                ])
            ]),
            .boolParameters([
                .hasTwentyFourSeven,
                .hasParking
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

    func testBuyCommercialFreePurpose() {
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
            .category(.commercial),
            .commercialType(.others([
                .freePurpose,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perOffer),
                Factory.makePrice(.perMeter)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),
            // TODO: @d0br0 subtest 'Время до метро на транспорте' is missed

            // Объект
            .singleSelectParameters([.entranceType]),
            .multipleSelectParameters([.commercialRenovation]),
            .singleSelectParameters([.commercialPlanType]),
            .boolParameters([.hasVentilation]),
            .singleSelectParameters([.furniture]),
            .boolParameters([.aircondition]),

            // Здание
            .singleSelectParameters([
                .commercialBuildingType([
                    .noMatter,
                    .warehouse,
                    .shoppingCenter,
                    .detachedBuilding,
                    .residentalBuilding,
                ])
            ]),
            .boolParameters([
                .hasTwentyFourSeven,
                .hasParking
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

    func testBuyCommercialWarehouse() {
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
            .category(.commercial),
            .commercialType(.others([
                .warehouse,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perOffer),
                Factory.makePrice(.perMeter)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),
            // TODO: @d0br0 subtest 'Время до метро на транспорте' is missed

            // Объект
            .singleSelectParameters([.entranceType]),
            .boolParameters([
                .hasVentilation,
                .aircondition,
            ]),

            // Здание
            .singleSelectParameters([
                .commercialBuildingType([
                    .noMatter,
                    .warehouse,
                    .shoppingCenter,
                    .detachedBuilding,
                    .residentalBuilding,
                ])
            ]),
            .boolParameters([
                .hasTwentyFourSeven,
                .hasParking
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

    func testBuyCommercialManufacturing() {
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
            .category(.commercial),
            .commercialType(.others([
                .manufacturing,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perOffer),
                Factory.makePrice(.perMeter)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),
            // TODO: @d0br0 subtest 'Время до метро на транспорте' is missed

            // Объект
            .singleSelectParameters([.entranceType]),
            .boolParameters([
                .hasVentilation,
                .aircondition,
            ]),

            // Здание
            .singleSelectParameters([
                .commercialBuildingType([
                    .noMatter,
                    .warehouse,
                    .shoppingCenter,
                    .detachedBuilding,
                    .residentalBuilding,
                ])
            ]),
            .boolParameters([
                .hasTwentyFourSeven,
                .hasParking
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

    func testBuyCommercialPublicCatering() {
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
            .category(.commercial),
            .commercialType(.others([
                .publicCatering,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perOffer),
                Factory.makePrice(.perMeter)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),
            // TODO: @d0br0 subtest 'Время до метро на транспорте' is missed

            // Объект
            .singleSelectParameters([.entranceType]),
            .singleSelectParameters([.furniture]),
            .boolParameters([.aircondition]),

            // Здание
            .singleSelectParameters([
                .commercialBuildingType([
                    .noMatter,
                    .warehouse,
                    .shoppingCenter,
                    .detachedBuilding,
                    .residentalBuilding,
                ])
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

    func testBuyCommercialAutoRepair() {
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
            .category(.commercial),
            .commercialType(.others([
                .autoRepair,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perOffer),
                Factory.makePrice(.perMeter)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),
            // TODO: @d0br0 subtest 'Время до метро на транспорте' is missed

            // Объект
            .singleSelectParameters([.entranceType]),
            .boolParameters([.aircondition]),

            // Здание
            .singleSelectParameters([
                .commercialBuildingType([
                    .noMatter,
                    .warehouse,
                    .shoppingCenter,
                    .detachedBuilding,
                    .residentalBuilding,
                ])
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

    func testBuyCommercialHotel() {
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
            .category(.commercial),
            .commercialType(.others([
                .hotel,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perOffer),
                Factory.makePrice(.perMeter)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),
            // TODO: @d0br0 subtest 'Время до метро на транспорте' is missed

            // Объект
            .singleSelectParameters([.entranceType]),
            .singleSelectParameters([.furniture]),
            .boolParameters([.aircondition]),

            // Здание
            .singleSelectParameters([
                .commercialBuildingType([
                    .noMatter,
                    .warehouse,
                    .shoppingCenter,
                    .detachedBuilding,
                    .residentalBuilding,
                ])
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

    func testBuyCommercialBusiness() {
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
            .category(.commercial),
            .commercialType(.others([
                .business,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perOffer),
                Factory.makePrice(.perMeter)
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),
            // TODO: @d0br0 subtest 'Время до метро на транспорте' is missed

            // Объект
            .singleSelectParameters([.entranceType]),

            // Здание
            .singleSelectParameters([
                .commercialBuildingType([
                    .noMatter,
                    .warehouse,
                    .shoppingCenter,
                    .detachedBuilding,
                    .residentalBuilding,
                ])
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
