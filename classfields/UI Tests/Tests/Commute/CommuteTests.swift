//
//  CommuteTests.swift
//  UITests
//
//  Created by Leontyev Saveliy on 10/03/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class CommuteTests: BaseTestCase {
    override func setUp() {
        super.setUp()

        GeoAPIStubConfigurator.setupAddressGeocoder(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupGeoSuggestList(using: self.dynamicStubs)
    }

    func testCommuteScreen() {
        self.relaunchApp(with: .commonUITests)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let commuteSteps = CommuteSteps()
        let addressSearchSteps = CommuteAddressSearchSteps()
        let locationDialog = SystemDialogs.makeLocationActivity(self)

        searchResultsSteps.openFilters()
        filtersSteps
            .tapOnCommute()
            .tapOnAcceptAlertButton()

        locationDialog
            .activate()
            .tapOnButton(.disallow)
            .deactivate()

        commuteSteps
            .isMapPresented()
            .isCommutePanelVisible()
            .isCommutePanelInSelectionAddressState()
            .pressAddressField()

        addressSearchSteps
            .isCommuteAddressSearchViewControllerExists()
            .selectFirstSuggest()

        commuteSteps
            .isCommutePanelVisible()
            .isCommutePanelInSelectionAddressState()
            .isAddressFieldContains(text: "Садовническая улица, 82с2", withRetryAttempts: 1)
            .pressSubmitButton()
            .isCommutePanelInCommuteConfigurationState()

        for index in 0..<3 {
            commuteSteps.selectTransportType(index: index)
        }

        for index in 0..<5 {
            commuteSteps.selectTime(index: index)
        }
    }

    func testCommuteCellTitle() {
        self.relaunchApp(with: .commonUITests)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let commuteSteps = CommuteSteps()
        let locationDialog = SystemDialogs.makeLocationActivity(self)

        searchResultsSteps.openFilters()

        filtersSteps
            .tapOnCommute()
            .tapOnAcceptAlertButton()

        locationDialog
            .activate()
            .tapOnButton(.disallow)
            .deactivate()

        commuteSteps
            .isCommutePanelVisible()
            .pressSubmitButton()
            .pressBackButton()

        filtersSteps.isCommuteCellTitleEqual(to: "10 минут пешком")

        self.reselectCommuteConfiguration(transportIndex: 1, timeIndex: 1)
        filtersSteps.isCommuteCellTitleEqual(to: "15 минут на машине")

        self.reselectCommuteConfiguration(transportIndex: 2, timeIndex: 2)
        filtersSteps.isCommuteCellTitleEqual(to: "20 минут на транспортe")

        self.reselectCommuteConfiguration(transportIndex: 0, timeIndex: 3)
        filtersSteps.isCommuteCellTitleEqual(to: "30 минут пешком")

        self.reselectCommuteConfiguration(transportIndex: 1, timeIndex: 4)
        filtersSteps.isCommuteCellTitleEqual(to: "45 минут на машине")
    }

    func testResetGeoAlert() {
        self.relaunchApp(with: .commonUITests)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()
        let commuteSteps = CommuteSteps()
        let locationDialog = SystemDialogs.makeLocationActivity(self)

        searchResultsSteps.openFilters()

        filtersSteps
            .tapOnCommute()
            .tapOnAcceptAlertButton()

        locationDialog
            .activate()
            .tapOnButton(.disallow)
            .deactivate()

        commuteSteps
            .isCommutePanelVisible()
            .pressSubmitButton()
            .pressBackButton()

        filtersSteps
            .tapOnPlainGeoIntentField()
            .tapOnCancelAlertButton()
            .tapOnDrawGeoIntentButton()
            .tapOnCancelAlertButton()
    }
    
    func testCommuteDeeplinkWithAddress() {
        APIStubConfigurator.setupCommuteWithAddressDeeplink(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupRegionInfo_Moscow(using: self.dynamicStubs)
        APIStubConfigurator.setupCommuteMapSearch(using: self.dynamicStubs)
        APIStubConfigurator.setupCommutePolygon(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupGeoSuggestCommuteRed(using: self.dynamicStubs)
        
        self.relaunchApp(with: .commonUITests)

        let commuteSteps = CommuteSteps()
        let mapScreenSteps = MapScreenSteps()
        let addressSearchSteps = CommuteAddressSearchSteps()

        self.communicator.sendDeeplink("https://realty.yandex.ru/moskva/kupit/kvartira/karta?commuteTransport=PUBLIC&commuteTime=20&zoom=14&commutePointLatitude=55.789238&commutePointLongitude=37.819073")
        
        mapScreenSteps
            .pressCommuteButton()
        commuteSteps
            .isCommutePanelVisible()
            .isAddressFieldContains(text: "Красное")
            .pressAddressField()
        addressSearchSteps
            .isCommuteAddressSearchViewControllerExists()
            .isCommuteSuggestNotEmpty()
    }

    func testCommuteDeeplinkWithoutAddress() {
        APIStubConfigurator.setupCommuteWithoutAddressDeeplink(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupRegionInfo_Moscow(using: self.dynamicStubs)
        APIStubConfigurator.setupCommuteMapSearch(using: self.dynamicStubs)
        APIStubConfigurator.setupCommutePolygon(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupGeoSuggestCommuteEmpty(using: self.dynamicStubs)

        self.relaunchApp(with: .commonUITests)

        let commuteSteps = CommuteSteps()
        let mapScreenSteps = MapScreenSteps()
        let addressSearchSteps = CommuteAddressSearchSteps()

        self.communicator
            .sendDeeplink("https://realty.yandex.ru/moskva/kupit/kvartira/1,2-komnatnie/karta/?commuteTransport=PUBLIC&commuteTime=20&leftLongitude=37.75279&bottomLatitude=55.767593&rightLongitude=37.888746&topLatitude=55.798597&zoom=14&commutePointLatitude=55.789238&commutePointLongitude=37.819073")

        mapScreenSteps
            .pressCommuteButton()
        commuteSteps
            .isCommutePanelVisible()
            .isAddressFieldContains(text: "55.789238, 37.819073")
            .pressAddressField()
        addressSearchSteps
            .isCommuteAddressSearchViewControllerExists()
            .isCommuteSuggestEmpty()
    }

    private func reselectCommuteConfiguration(transportIndex: Int, timeIndex: Int) {
        let filtersSteps = FiltersSteps()
        let commuteSteps = CommuteSteps()

        filtersSteps
            .tapOnCommute()

        commuteSteps
            .isCommutePanelInCommuteConfigurationState()
            .selectTransportType(index: transportIndex)
            .selectTime(index: timeIndex)
            .pressBackButton()
    }
}
