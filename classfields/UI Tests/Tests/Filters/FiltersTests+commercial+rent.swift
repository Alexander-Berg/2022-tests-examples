//
//  FiltersTests+commercial.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 11.08.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

extension FiltersTests {

    // MARK: - Rent. Commercial

    // swiftlint:disable file_length
    func testRentCommercialAll() {
        self.relaunchApp(with: .filterUITests)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let subtests = FiltersSubtests(stubs: self.dynamicStubs, anyOfferKind: .offer)

        searchResultsSteps.openFilters()
        filtersSteps
            .isScreenPresented()

        subtests.run(
            .action(.rent),
            .category(.commercial),

            .commercialTypeCancellation,
            .commercialType(.noMatter),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perCommercialOfferPerMonth),
                Factory.makePrice(.perCommercialOfferPerYear),
                Factory.makePrice(.perMeterPerMonth),
                Factory.makePrice(.perMeterPerYear),
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),

            .metroDistance(Factory.metroDistanceOptions),
            // TODO: @d0br0 subtest 'Время до метро на транспорте' is missed

            // Объект
            .singleSelectParameters([.entranceType]),

            // Условие сделки
            .singleSelectParameters([.dealStatusCommercialRent]),
            .multiParameters([
                .taxationForm
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

    func testRentCommercialLand() {
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
            .category(.commercial),
            .commercialTypeCancellation,
            .commercialType(.land),
            .numberRange([
                Factory.makeNumberRange(.commercialLotArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perCommercialOfferPerMonth),
                Factory.makePrice(.perCommercialOfferPerYear),
                Factory.makePrice(.perArePerMonth),
                Factory.makePrice(.perArePerYear),
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),

            // Условие сделки
            .singleSelectParameters([.dealStatusCommercialRent]),
            .multiParameters([
                .taxationForm
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

    func testRentCommercialOthers() {
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
                .legalAddress,
                .business,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perCommercialOfferPerMonth),
                Factory.makePrice(.perCommercialOfferPerYear),
                Factory.makePrice(.perMeterPerMonth),
                Factory.makePrice(.perMeterPerYear),
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
            .multipleSelectParameters([.officeClass]),
            .boolParameters([
                .hasTwentyFourSeven,
                .hasParking
            ]),

            // Условие сделки
            .singleSelectParameters([.dealStatusCommercialRent]),
            .boolParameters([
                .hasUtilitiesIncluded,
                .hasElectricityIncluded,
                .hasCleaningIncluded
            ]),
            .multiParameters([
                .taxationForm
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

    func testRentCommercialOffice() {
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
            .category(.commercial),
            .commercialType(.others([
                .office,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perCommercialOfferPerMonth),
                Factory.makePrice(.perCommercialOfferPerYear),
                Factory.makePrice(.perMeterPerMonth),
                Factory.makePrice(.perMeterPerYear),
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
            .multipleSelectParameters([.officeClass]),
            .boolParameters([
                .hasTwentyFourSeven,
                .hasParking
            ]),

            // Условие сделки
            .singleSelectParameters([.dealStatusCommercialRent]),
            .boolParameters([
                .hasUtilitiesIncluded,
                .hasElectricityIncluded,
                .hasCleaningIncluded
            ]),
            .multiParameters([
                .taxationForm
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

    func testRentCommercialRetail() {
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
            .category(.commercial),
            .commercialType(.others([
                .retail,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perCommercialOfferPerMonth),
                Factory.makePrice(.perCommercialOfferPerYear),
                Factory.makePrice(.perMeterPerMonth),
                Factory.makePrice(.perMeterPerYear),
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
            .multipleSelectParameters([.officeClass]),
            .boolParameters([
                .hasTwentyFourSeven,
                .hasParking
            ]),

            // Условие сделки
            .singleSelectParameters([.dealStatusCommercialRent]),
            .boolParameters([
                .hasUtilitiesIncluded,
                .hasElectricityIncluded,
                .hasCleaningIncluded
            ]),
            .multiParameters([
                .taxationForm
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

    func testRentCommercialFreePurpose() {
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
            .category(.commercial),
            .commercialType(.others([
                .freePurpose,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perCommercialOfferPerMonth),
                Factory.makePrice(.perCommercialOfferPerYear),
                Factory.makePrice(.perMeterPerMonth),
                Factory.makePrice(.perMeterPerYear),
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
            .multipleSelectParameters([.officeClass]),
            .boolParameters([
                .hasTwentyFourSeven,
                .hasParking
            ]),

            // Условие сделки
            .singleSelectParameters([.dealStatusCommercialRent]),
            .boolParameters([
                .hasUtilitiesIncluded,
                .hasElectricityIncluded,
                .hasCleaningIncluded
            ]),
            .multiParameters([
                .taxationForm
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

    func testRentCommercialWarehouse() {
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
            .category(.commercial),
            .commercialType(.others([
                .warehouse,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perCommercialOfferPerMonth),
                Factory.makePrice(.perCommercialOfferPerYear),
                Factory.makePrice(.perMeterPerMonth),
                Factory.makePrice(.perMeterPerYear),
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),
            // TODO: @d0br0 subtest 'Время до метро на транспорте' is missed

            // Объект
            .singleSelectParameters([.entranceType]),
            .boolParameters([
                .hasVentilation,
                .aircondition
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
            .multipleSelectParameters([.officeClass]),
            .boolParameters([
                .hasTwentyFourSeven,
                .hasParking
            ]),

            // Условие сделки
            .singleSelectParameters([.dealStatusCommercialRent]),
            .boolParameters([
                .hasUtilitiesIncluded,
                .hasElectricityIncluded,
                .hasCleaningIncluded
            ]),
            .multiParameters([
                .taxationForm
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

    func testRentCommercialManufacturing() {
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
            .category(.commercial),
            .commercialType(.others([
                .manufacturing,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perCommercialOfferPerMonth),
                Factory.makePrice(.perCommercialOfferPerYear),
                Factory.makePrice(.perMeterPerMonth),
                Factory.makePrice(.perMeterPerYear),
            ])),

            // Расположение
            .geoIntent(options: .resetGeoIntentAlertShouldBeShown(for: [])),
            .metroDistance(Factory.metroDistanceOptions),
            // TODO: @d0br0 subtest 'Время до метро на транспорте' is missed

            // Объект
            .singleSelectParameters([.entranceType]),
            .boolParameters([
                .hasVentilation,
                .aircondition
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
            .multipleSelectParameters([.officeClass]),
            .boolParameters([
                .hasTwentyFourSeven,
                .hasParking
            ]),

            // Условие сделки
            .singleSelectParameters([.dealStatusCommercialRent]),
            .boolParameters([
                .hasUtilitiesIncluded,
                .hasElectricityIncluded,
                .hasCleaningIncluded
            ]),
            .multiParameters([
                .taxationForm
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

    func testRentCommercialPublicCatering() {
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
            .category(.commercial),
            .commercialType(.others([
                .publicCatering,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perCommercialOfferPerMonth),
                Factory.makePrice(.perCommercialOfferPerYear),
                Factory.makePrice(.perMeterPerMonth),
                Factory.makePrice(.perMeterPerYear),
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
            .multipleSelectParameters([.officeClass]),

            // Условие сделки
            .singleSelectParameters([.dealStatusCommercialRent]),
            .boolParameters([
                .hasUtilitiesIncluded,
                .hasElectricityIncluded,
                .hasCleaningIncluded
            ]),
            .multiParameters([
                .taxationForm
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

    func testRentCommercialAutoRepair() {
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
            .category(.commercial),
            .commercialType(.others([
                .autoRepair,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perCommercialOfferPerMonth),
                Factory.makePrice(.perCommercialOfferPerYear),
                Factory.makePrice(.perMeterPerMonth),
                Factory.makePrice(.perMeterPerYear),
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
            .multipleSelectParameters([.officeClass]),

            // Условие сделки
            .singleSelectParameters([.dealStatusCommercialRent]),
            .boolParameters([
                .hasUtilitiesIncluded,
                .hasElectricityIncluded,
                .hasCleaningIncluded
            ]),
            .multiParameters([
                .taxationForm
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

    func testRentCommercialHotel() {
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
            .category(.commercial),
            .commercialType(.others([
                .hotel,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perCommercialOfferPerMonth),
                Factory.makePrice(.perCommercialOfferPerYear),
                Factory.makePrice(.perMeterPerMonth),
                Factory.makePrice(.perMeterPerYear),
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
            .multipleSelectParameters([.officeClass]),

            // Условие сделки
            .singleSelectParameters([.dealStatusCommercialRent]),
            .boolParameters([
                .hasUtilitiesIncluded,
                .hasElectricityIncluded,
                .hasCleaningIncluded
            ]),
            .multiParameters([
                .taxationForm
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

    func testRentCommercialBusiness() {
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
            .category(.commercial),
            .commercialType(.others([
                .business,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perCommercialOfferPerMonth),
                Factory.makePrice(.perCommercialOfferPerYear),
                Factory.makePrice(.perMeterPerMonth),
                Factory.makePrice(.perMeterPerYear),
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
            .multipleSelectParameters([.officeClass]),

            // Условие сделки
            .singleSelectParameters([.dealStatusCommercialRent]),
            .boolParameters([
                .hasUtilitiesIncluded,
                .hasElectricityIncluded,
                .hasCleaningIncluded
            ]),
            .multiParameters([
                .taxationForm
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

    func testRentCommercialLegalAddress() {
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
            .category(.commercial),
            .commercialType(.others([
                .legalAddress,
            ])),
            .numberRange([
                Factory.makeNumberRange(.commercialArea)
            ]),
            .price(.multiple([
                Factory.makePrice(.perCommercialOfferPerMonth),
                Factory.makePrice(.perCommercialOfferPerYear),
                Factory.makePrice(.perMeterPerMonth),
                Factory.makePrice(.perMeterPerYear),
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
            .multipleSelectParameters([.officeClass]),

            // Условие сделки
            .singleSelectParameters([.dealStatusCommercialRent]),
            .multiParameters([
                .taxationForm
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
