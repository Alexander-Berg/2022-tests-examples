//
//  DeeplinkOfferListTests.swift
//  UITests
//
//  Created by Evgeny Y. Petrov on 09/01/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import YREAppConfig
import XCTest

final class DeeplinkListSearchTests: BaseTestCase {
    func testOpensOfferList() {
        APIStubConfigurator.setupOfferListDeeplink_OrenburgRegion(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupRegionInfo_OrenburgRegion(using: self.dynamicStubs)
        APIStubConfigurator.setupOfferSearchResultsList_OrenburgRegion(using: self.dynamicStubs)

        self.relaunchApp(with: .commonUITests)

        let expectedPageTitle = "Оренбургская область"

        self.communicator
            .sendDeeplink("https://realty.yandex.ru/orenburgskaya_oblast/kupit/kvartira/dvuhkomnatnaya/")
        SearchResultsListSteps()
            .isScreenPresented()
            .isScreenTitle(equals: expectedPageTitle)
            .isSwitchToMapButtonTappable()
            .isSwitchToFiltersButtonTappable()
            .withOfferList()
            .isListNonEmpty()
    }

    func testOpensAMPOfferList() {
        APIStubConfigurator.setupOfferListDeeplink_OrenburgRegion(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupRegionInfo_OrenburgRegion(using: self.dynamicStubs)
        APIStubConfigurator.setupOfferSearchResultsList_OrenburgRegion(using: self.dynamicStubs)

        self.relaunchApp(with: .commonUITests)

        let expectedPageTitle = "Оренбургская область"

        self.communicator
            .sendDeeplink("https://realty.yandex.ru/amp/orenburgskaya_oblast/kupit/kvartira/dvuhkomnatnaya/")
        SearchResultsListSteps()
            .isScreenPresented()
            .isScreenTitle(equals: expectedPageTitle)
            .isSwitchToMapButtonTappable()
            .isSwitchToFiltersButtonTappable()
            .withOfferList()
            .isListNonEmpty()
    }

    func testOpensSiteList() {
        APIStubConfigurator.setupSiteListDeeplink(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupRegionInfo_MoscowAndMO(using: self.dynamicStubs)
        APIStubConfigurator.setupSiteSearchResultsList_MoscowAndMO(using: self.dynamicStubs)

        self.relaunchApp(with: .commonUITests)

        let expectedPageTitle = "Москва и МО"

        self.communicator
            .sendDeeplink("https://realty.yandex.ru/moskva_i_moskovskaya_oblast/kupit/novostrojka/?from=main_menu")
        SearchResultsListSteps()
            .isScreenPresented()
            .isScreenTitle(equals: expectedPageTitle)
            .isSwitchToMapButtonTappable()
            .isSwitchToFiltersButtonTappable()
            .withSiteList()
            .isListNonEmpty()
    }

    func testOpensVillageList() {
        APIStubConfigurator.setupVillageListDeeplink(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupRegionInfo_MoscowAndMO(using: self.dynamicStubs)
        APIStubConfigurator.setupVillageSearchResultsList_MoscowAndMO(using: self.dynamicStubs)

        self.relaunchApp(with: .commonUITests)

        let expectedPageTitle = "Москва и МО"

        self.communicator
            .sendDeeplink("https://realty.yandex.ru/moskva_i_moskovskaya_oblast/kupit/kottedzhnye-poselki/?from=main_menu")
        SearchResultsListSteps()
            .isScreenPresented()
            .isScreenTitle(equals: expectedPageTitle)
            .isSwitchToMapButtonTappable()
            .isSwitchToFiltersButtonTappable()
            .withVillageList()
            .isListNonEmpty()
    }

    // case: https://st.yandex-team.ru/VSAPPS-6776
    func testOpenOfferListAfterRelaunch() {
        APIStubConfigurator.setupOfferListDeeplink_Spb(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupRegionInfo_Spb(using: self.dynamicStubs)
        APIStubConfigurator.setupOfferSearchResultsList_Spb(using: self.dynamicStubs)

        self.relaunchApp(with: .savedSearchTests)
        
        let expectedPageTitle = "Санкт-Петербург"

        SavedSearchesListSteps()
            .screenIsPresented()

        self.communicator
            .sendDeeplink("https://realty.yandex.ru/sankt-peterburg/kupit/kvartira/dvuhkomnatnaya/")
        SearchResultsListSteps()
            .isScreenPresented()
            .isScreenTitle(equals: expectedPageTitle)

        CommonSearchResultsSteps()
            .filterButtonHasBadge()
    }

    // Case for bug  https://st.yandex-team.ru/VSAPPS-6802
    func testChangeRegionAndGeointentAfterDeeplink() {
        let list = SearchResultsListSteps()
        let commonSearchResult = CommonSearchResultsSteps()
        let filter = FiltersSteps()
        let geoIntentPicker = GeoIntentSteps()

        APIStubConfigurator.setupOfferListDeeplink_Spb(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupRegionListMoscowAndMO(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupGeoSuggestMoscowMetro(using: self.dynamicStubs)

        self.relaunchApp(with: .commonUITests)

        // Run deeplink for SPb search
        GeoAPIStubConfigurator.setupRegionInfo_SpbLO(using: self.dynamicStubs)
        
        self.communicator
            .sendDeeplink("https://realty.yandex.ru/sankt-peterburg/kupit/kvartira/dvuhkomnatnaya/")
        list
            .isScreenPresented()

        // Change filter to MSK region and add plain geointent
        GeoAPIStubConfigurator.setupRegionInfo_Moscow(using: self.dynamicStubs)

        commonSearchResult
            .openFiltersIfNeeded()
        filter
            .tapOnPlainGeoIntentField()

        geoIntentPicker
            .tapOnRegionButton()

        RegionSearchSteps()
            .changeRegion(rgid: "741964") // MSK

        geoIntentPicker
            .typeTextIntoGeoIntentSearchField(text: "метро Арбатская")
            .tapToFirstSuggestedGeoIntent()
            .tapSubmitButton()
        
        list
            .isScreenPresented()
            .isScreenTitle(equals: "метро Арбатская")
    }

    // case: https://st.yandex-team.ru/VSAPPS-8217
    func testOpenOfferListWithoutRegionChange() {
        let expectedPageTitle = "Санкт-Петербург и ЛО"

        // Initial state - Spb region

        GeoAPIStubConfigurator.setupRegionAutodetectWithRegion_SpbLO(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupRegionInfo_SpbLO(using: self.dynamicStubs)

        let configuration = ExternalAppConfiguration.commonUITests
        configuration.mainListOpened = true
        configuration.geoData = .emptyRegion(geoIntent: .fromRegion)
        self.relaunchApp(with: configuration)

        let list = SearchResultsListSteps()
        list
            .isScreenPresented()
            .isScreenTitle(equals: expectedPageTitle)

        // State after no-region deeplink open - still Spb region

        APIStubConfigurator.setupOfferListDeeplink_EmptyGeo(using: self.dynamicStubs)

        self.communicator
            .sendDeeplink("https://realty.yandex.ru/any-region/kupit/kvartira/dvuhkomnatnaya/")
        list
            .isScreenPresented()
            .isScreenTitle(equals: expectedPageTitle)
    }

    // case: https://st.yandex-team.ru/VSAPPS-8217
    func testOpenOfferListWithoutGeoIntentChange() {
        let expectedPageTitle = "метро Арбатская"

        // Initial state

        // There's a stub for RegionConfiguration only -
        // - we start with an outdated Configuration and expect the app will fetch it
        GeoAPIStubConfigurator.setupRegionInfo_Moscow(using: self.dynamicStubs)

        // Region has its own GeoIntent, and we also provide an additional geo data
        let configuration = ExternalAppConfiguration.commonUITests
        configuration.geoData = .fallbackWithOutdatedRegionConfiguration(geoIntent: .customTitle(expectedPageTitle))
        configuration.mainListOpened = true
        self.relaunchApp(with: configuration)

        let list = SearchResultsListSteps()
        list
            .isScreenPresented()
            .isScreenTitle(equals: expectedPageTitle)

        // State after no-region deeplink open

        APIStubConfigurator.setupOfferListDeeplink_EmptyGeo(using: self.dynamicStubs)

        self.communicator
            .sendDeeplink("https://realty.yandex.ru/any-region/kupit/kvartira/dvuhkomnatnaya/")
        list
            .isScreenPresented()
            .isScreenTitle(equals: expectedPageTitle)
    }
}
