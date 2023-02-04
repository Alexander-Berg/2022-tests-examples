//
//  FilterPresetsTests.swift
//  UI Tests
//
//  Created by Alexey Salangin on 3/16/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig

final class FilterPresetsTests: BaseTestCase {
    func testRentTwoRoomApartmentPreset() {
        let configuration = ExternalAppConfiguration.filterUITests
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()
            .isPresetsCellPresented()
            .tapOnPresetCell(type: .rentTwoRoomApartment, takeScreenshot: true)

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()

        XCTAssertTrue(filtersSteps.action(equalTo: .rent))
        XCTAssertTrue(filtersSteps.category(equalTo: .apartment))

        filtersSteps
            .isRentTimeEqual(to: .long)
            .areRoomsTotalEqual(to: .rooms2)
    }

    func testBuyApartmentInSitePreset() {
        let configuration = ExternalAppConfiguration.filterUITests
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()
            .isPresetsCellPresented()
            .tapOnPresetCell(type: .buyApartmentInSite)

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()

        XCTAssertTrue(filtersSteps.action(equalTo: .buy))
        XCTAssertTrue(filtersSteps.category(equalTo: .apartment))

        filtersSteps
            .isApartmentTypeEqual(to: .newbuilding)
            .priceParameter(hasValue: "от 1,4 млн ₽")
    }

    func testBuySingleRoomApartment() {
        let configuration = ExternalAppConfiguration.filterUITests
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()
            .isPresetsCellPresented()
            .tapOnPresetCell(type: .buySingleRoomApartment)

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()

        XCTAssertTrue(filtersSteps.action(equalTo: .buy))
        XCTAssertTrue(filtersSteps.category(equalTo: .apartment))

        filtersSteps
            .isApartmentTypeEqual(to: .all)
            .areRoomsTotalEqual(to: .rooms1)
            .priceParameter(hasValue: "за всё")
    }

    func testBuyVillagePresetInSPb() {
        let configuration = ExternalAppConfiguration.filterUITests
        configuration.geoData.regionData = .custom(regionType: .stPeterburgAndRegion)
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()
            .isPresetsCellPresented()
            .tapOnPresetCell(type: .buyVillage)

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()

        XCTAssertTrue(filtersSteps.action(equalTo: .buy))
        XCTAssertTrue(filtersSteps.category(equalTo: .house))

        filtersSteps
            .isObjectTypeEqual(to: .village)
            .priceParameter(hasValue: "от 5,2 млн ₽")
    }

    func testBuyVillagePresetInSamara() {
        let configuration = ExternalAppConfiguration.filterUITests
        configuration.geoData.regionData = .custom(regionType: .samara)
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()
            .isPresetsCellPresented()
            .isPresetNotExists(type: .buyVillage)
    }

    func testDeveloperLegendaPromoPresetInMoscow() {
        let configuration = ExternalAppConfiguration.filterUITests
        configuration.geoData.regionData = .custom(regionType: .moscowAndRegion)
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()
            .isPresetsCellPresented()
            .isPresetNotExists(type: .developerLegendaPromo)
    }

    func testDeveloperLegendaPromoPresetInSpb() {
        let configuration = ExternalAppConfiguration.filterUITests
        configuration.geoData.regionData = .custom(regionType: .stPeterburgAndRegion)
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()
            .isPresetsCellPresented()
            .tapOnPresetCell(type: .developerLegendaPromo, takeScreenshot: true)

        let wrappedBrowserSteps = WrappedBrowserSteps()
        wrappedBrowserSteps
            .isScreenPresented()
            .tapOnCloseButton()
            .isScreenNotPresented()
    }

    func testConciergePresetInMoscow() {
        ConciergeAPIStubConfigurator(dynamicStubs: self.dynamicStubs).setupConcierge()

        let configuration = ExternalAppConfiguration.filterUITests
        configuration.geoData.regionData = .custom(regionType: .moscowAndRegion)
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()
            .switchToAction(.rent)
            .isPresetsCellPresented()
            .isPresetNotExists(type: .concierge)

        filtersSteps
            .switchToAction(.buy)
            .switchToCategory(.house)
            .isPresetsCellPresented()
            .isPresetNotExists(type: .concierge)

        filtersSteps
            .switchToCategory(.room)
            .isPresetsCellPresented()
            .tapOnPresetCell(type: .concierge, takeScreenshot: false)

        let conciergeSteps = ConciergeSteps()

        conciergeSteps
            .isScreenPresented()
    }

    func testConciergePresetInEkaterinburg() {
        let configuration = ExternalAppConfiguration.filterUITests
        configuration.geoData.regionData = .custom(regionType: .ekaterinburg)
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()
            .isPresetsCellPresented()
            .isPresetNotExists(type: .concierge)
    }

    func testYandexRentPresetInMoscow() {
        let configuration = ExternalAppConfiguration.filterUITests
        configuration.geoData.regionData = .custom(regionType: .moscowAndRegion)
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()
            .isPresetsCellPresented()
            .tapOnPresetCell(type: .yandexRent)

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()

        XCTAssertTrue(filtersSteps.action(equalTo: .rent))
        XCTAssertTrue(filtersSteps.category(equalTo: .apartment))

        filtersSteps
            .isRentTimeEqual(to: .long)
            .isBoolParameterEnabled(cellAccessibilityIdentifier: FiltersSteps.Identifiers.yandexRentCell)
    }

    func testYandexRentPresetInEkaterinburg() {
        let configuration = ExternalAppConfiguration.filterUITests
        configuration.geoData.regionData = .custom(regionType: .ekaterinburg)
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()
            .isPresetsCellPresented()
            .isPresetNotExists(type: .yandexRent)
    }
}
