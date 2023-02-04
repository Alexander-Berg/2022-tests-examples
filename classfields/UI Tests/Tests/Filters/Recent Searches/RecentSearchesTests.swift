//
//  RecentSearchesTests.swift
//  UI Tests
//
//  Created by Alexey Salangin on 3/22/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig

final class RecentSearchesTests: BaseTestCase {
    func testThereAreNoRecentSearchesOnFirstLaunch() {
        let configuration = ExternalAppConfiguration.filterUITests
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()
            .isRecentSearchesCellNotPresented()
    }

    func test2IdenticalSearches() {
        let configuration = ExternalAppConfiguration.filterUITests
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()
            .submitFilters()

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()
            .submitFilters()

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()
            .isRecentSearchesCellPresented()
            .isCellsCountEqual(to: 1)
    }

    func test6DifferentSearches() {
        let configuration = ExternalAppConfiguration.filterUITests
        configuration.geoData.regionData = .custom(regionType: .moscowAndRegion)
        self.relaunchApp(with: configuration)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        searchResultsSteps
            .openFilters()

        self.makeRentSearch()
        self.makeBuySearch()
        self.makeRentSearch()
        self.makeBuySearch()
        self.makeBigSearch()
        self.makeBuySearch()

        filtersSteps
            .isScreenPresented()
            .isRecentSearchesCellPresented()
            .makeScreenshot()
            .isCellsCountEqual(to: 5)
            .tapOnSearchCell(index: 1)

        searchResultsSteps
            .openFilters()

        filtersSteps
            .ensureAction(equalTo: .rent)
            .areRoomsTotalEqual(to: .studio, .rooms2)
            .isBoolParameterEnabled(cellAccessibilityIdentifier: FiltersSteps.Identifiers.yandexRentCell)
            .singleSelectParameter(with: FiltersSteps.Identifiers.metroDistanceCell, hasValue: "10 минут пешком")
    }

    func testBottomSheet() {
        let configuration = ExternalAppConfiguration.filterUITests
        configuration.geoData.regionData = .custom(regionType: .moscowAndRegion)
        self.relaunchApp(with: configuration)

        APIStubConfigurator.setupOfferSearchResultsCount(using: self.dynamicStubs)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        searchResultsSteps
            .openFilters()

        self.makeBigSearch()
        self.makeBuySearch()

        filtersSteps
            .isScreenPresented()
            .isRecentSearchesCellPresented()
            .isCellsCountEqual(to: 2)
            .tapOnSearchMoreButton(index: 1)
            .makeBottomSheetScreenshot()
            .tapOnBottomSheetApplyButton()

        searchResultsSteps
            .openFilters()

        filtersSteps
            .ensureAction(equalTo: .rent)
    }

    private func makeRentSearch() {
        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        filtersSteps
            .isScreenPresented()
            .switchToAction(.rent)
            .submitFilters()

        searchResultsSteps
            .openFilters()
    }

    private func makeBuySearch() {
        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        filtersSteps
            .isScreenPresented()
            .switchToAction(.buy)
            .submitFilters()

        searchResultsSteps
            .openFilters()
    }

    private func makeBigSearch() {
        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        filtersSteps
            .isScreenPresented()
            .switchToAction(.rent)
            .switchToCategory(.apartment)
            .tapOnRoomsTotalButtons([.studio, .rooms2])
            .tapOnBoolParameterCell(accessibilityIdentifier: FiltersSteps.Identifiers.yandexRentCell)
            .tapOnMetroDistanceCell()
            .tapOnRow("10 минут")
            .tapOnApplyButton()
        filtersSteps
            .submitFilters()

        searchResultsSteps
            .openFilters()
    }
}
