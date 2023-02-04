//
//  FiltersRegionTests.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 03.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig

final class FiltersRegionTests: BaseTestCase {
    func testOnlySamoletAppearance() {
        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let parameterName = "ЖК от Самолет"

        self.performTest(parameterName: parameterName, regions: [.moscowAndRegion]) {
            searchResultsSteps.openFilters()
            filtersSteps
                .isScreenPresented()
                .switchToCategory(.apartment)
                .switchToAction(.buy)
                .tapOnApartmentTypeButton(.newbuilding)
                .isCellPresented(containing: parameterName)
        }

        self.performTest(parameterName: parameterName, regions: [.novokuznetsk]) {
            searchResultsSteps.openFilters()
            filtersSteps
                .isScreenPresented()
                .tapOnApartmentTypeButton(.newbuilding)
                .isCellNotPresented(containing: parameterName)
        }
    }

    func testMetroStationBuildingAppearance() {
        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let parameterName = "Рядом построят станцию метро"

        self.performTest(
            parameterName: parameterName,
            regions: [.moscowAndRegion, .stPeterburgAndRegion]
        ) {
            searchResultsSteps.openFilters()
            filtersSteps
                .isScreenPresented()
                .switchToCategory(.apartment)
                .switchToAction(.buy)
                .tapOnApartmentTypeButton(.secondary)
                .isCellPresented(containing: parameterName)
        }


        self.performTest(parameterName: parameterName, regions: [.novokuznetsk]) {
            searchResultsSteps.openFilters()
            filtersSteps
                .isScreenPresented()
                .isCellNotPresented(containing: parameterName)
        }
    }

    func testRenovationProgrammAppearance() {
        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let parameterName = "Программа реновации"

        self.performTest(parameterName: parameterName, regions: [.moscowAndRegion]) {
            searchResultsSteps.openFilters()
            filtersSteps
                .isScreenPresented()
                .switchToCategory(.apartment)
                .switchToAction(.buy)
                .tapOnApartmentTypeButton(.secondary)
                .isCellPresented(containing: parameterName)
        }


        self.performTest(parameterName: parameterName, regions: [.novokuznetsk]) {
            searchResultsSteps.openFilters()
            filtersSteps
                .isScreenPresented()
                .isCellNotPresented(containing: parameterName)
        }
    }

    func testTimeToMetroAppearance() {
        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let parameterName = "Время до метро"

        self.performTest(
            parameterName: parameterName,
            regions: [
                .moscowAndRegion,
                .stPeterburgAndRegion,
                .novosibirsk,
                .ekaterinburg,
                .nizhniyNovgorod,
                .samara,
                .kazan
            ]
        ) {
            searchResultsSteps.openFilters()
            filtersSteps
                .isScreenPresented()
                .switchToCategory(.apartment)
                .switchToAction(.buy)
                .tapOnApartmentTypeButton(.secondary)
                .isCellPresented(containing: parameterName)
        }


        self.performTest(parameterName: parameterName, regions: [.novokuznetsk]) {
            searchResultsSteps.openFilters()
            filtersSteps
                .isScreenPresented()
                .isCellNotPresented(containing: parameterName)
        }
    }

    func testBuyVillageHouseAppearance() {
        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let parameterName = "Коттеджный поселок (Дом)"

        self.performTest(
            parameterName: parameterName,
            regions: [
                .moscowAndRegion,
                .stPeterburgAndRegion,
            ]
        ) {
            searchResultsSteps.openFilters()
            filtersSteps
                .isScreenPresented()
                .switchToAction(.buy)
                .switchToCategory(.house)
                .ensureObjectButtonExists(.village)
        }


        self.performTest(parameterName: parameterName, regions: [.novokuznetsk]) {
            searchResultsSteps.openFilters()
            filtersSteps
                .isScreenPresented()
                .switchToAction(.buy)
                .switchToCategory(.house)
                .ensureObjectButtonNotExists(.village)
        }
    }

    func testBuyVillageLotAppearance() {
        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let parameterName = "Коттеджный поселок (Участок)"

        self.performTest(
            parameterName: parameterName,
            regions: [
                .moscowAndRegion,
                .stPeterburgAndRegion,
            ]
        ) {
            searchResultsSteps.openFilters()
            filtersSteps
                .isScreenPresented()
                .switchToAction(.buy)
                .switchToCategory(.lot)
                .ensureObjectButtonExists(.village)
        }


        self.performTest(parameterName: parameterName, regions: [.novokuznetsk]) {
            searchResultsSteps.openFilters()
            filtersSteps
                .isScreenPresented()
                .switchToAction(.buy)
                .switchToCategory(.lot)
                .ensureObjectButtonNotExists(.village)
        }
    }

    func testOnlySamoletParameterDisappearanceOnRegionChange() {
        let queryItems: Set<URLQueryItem> = [
            URLQueryItem(name: "onlySamolet", value: "YES"),
            URLQueryItem(name: "developerId", value: "102320"),
        ]

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let api = FiltersAPIStubConfigurator(dynamicStubs: self.dynamicStubs, anyOfferKind: .site)
        let parameterName = "ЖК от Самолет"

        self.performTest(parameterName: parameterName, regions: [.moscowAndRegion]) {
            searchResultsSteps.openFilters()
            filtersSteps
                .isScreenPresented()
                .switchToCategory(.apartment)
                .switchToAction(.buy)
                .tapOnApartmentTypeButton(.newbuilding)
                .isCellPresented(containing: parameterName)

            self.performAPITest(using: api, contains: queryItems, notContain: [], closure: {
                filtersSteps
                    .tapOnBoolParameterCell(accessibilityIdentifier: FiltersSteps.Identifiers.onlySamoletCell)
            })

            filtersSteps
                .isCellPresented(accessibilityIdentifier: FiltersSteps.Identifiers.geoIntentCell)
                .tapOnPlainGeoIntentField()

            GeoAPIStubConfigurator.setupRegionList_SpbAndLO(using: self.dynamicStubs)
            GeoAPIStubConfigurator.setupRegionInfo_SpbLO(using: self.dynamicStubs)

            let geoIntentPicker = GeoIntentSteps()
            geoIntentPicker
                .tapOnRegionButton()

            RegionSearchSteps()
                .changeRegion(rgid: "741965") // Spb & LO

            geoIntentPicker
                .tapOnBackButton()

            filtersSteps
                .isCellNotPresented(containing: parameterName)

            self.performAPITest(using: api, contains: [], notContain: queryItems, closure: {
                // It's small hack to make an additional request
                // The additional request is needed to check that 'queryItems'
                // is not included in request parameters.
                filtersSteps
                    .isCellPresented(containing: "Студия")
                    .tapOnRoomsTotalButton(.studio)
            })
        }
    }

    // MARK: - Private

    private func performTest(parameterName: String, regions: [RegionType], testClosure: () -> Void) {
        let configuration = ExternalAppConfiguration.filterUITests
        configuration.geoData.geoIntent = .fromRegion

        for region in regions {
            let activityName = "Проверяем параметер \"\(parameterName)\" в регионе \(region.readableName)"
            XCTContext.runActivity(named: activityName) { _ in
                configuration.geoData.regionData = .custom(regionType: region)
                self.relaunchApp(with: configuration)
                testClosure()
            }
        }
    }

    private func performAPITest(
        using api: FiltersAPIStubConfigurator,
        contains: Set<URLQueryItem>,
        notContain: Set<URLQueryItem>,
        closure: () -> Void
    ) {
        let expectation = XCTestExpectation()
        let notContainKeys = Set(notContain.map { $0.name })

        api.setupSearchCounter(
            predicate: .queryItems(
                contain: contains,
                notContainKeys: notContainKeys,
                notContain: notContain
            ),
            handler: { expectation.fulfill() }
        )

        closure()

        let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
        XCTAssert(result)
    }

    private typealias RegionType = ExternalAppConfiguration.GeoData.RegionData.RegionType
}
