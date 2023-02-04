//
//  AppStartupTests.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 08.04.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig

final class AppStartupTests: BaseTestCase { }

// MARK: - Geo on First Launch

extension AppStartupTests {
    func testFirstAppLaunchWithoutPredefinedRegion() {
        GeoAPIStubConfigurator.setupRegionAutodetectWithRegion_SpbLO(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupRegionInfo_SpbLO(using: self.dynamicStubs)

        self.relaunchApp(with: Self.appConfigurationWithoutRegionStub)

        let map = SearchResultsMapSteps()
        map
            .isScreenPresented()
            .hasGeoIntentWithTitle("Санкт-Петербург и ЛО")
    }
    
    func testFirstAppLaunchWithoutPredefinedRegionShowsPaidSites() {
        GeoAPIStubConfigurator.setupRegionAutodetectWithRegion_SpbLO(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupRegionInfo_SpbLO_hasPaidSites(using: self.dynamicStubs)

        // Value of 'shouldDisplayEmbeddedMainFilters' parameter must not impact the scenario and its test.
        // Here we set it to true to prove that fact."
        let configuration = Self.appConfigurationWithoutRegionStub
        configuration.shouldDisplayEmbeddedMainFilters = true
        self.relaunchApp(with: configuration)

        FiltersSteps()
            .isScreenNotPresented()

        SearchResultsMapSteps()
            .isScreenPresented()
            .hasGeoIntentWithTitle("Санкт-Петербург и ЛО")

        CommonSearchResultsSteps()
            .openFilters()
            .isApartmentTypeEqual(to: .newbuilding)
    }

    func testFirstAppLaunchWithRegionFallback() {
        // No stubs for a region - waiting for a fallback

        self.relaunchApp(with: Self.appConfigurationWithoutRegionStub)

        let map = SearchResultsMapSteps()
        map
            .isScreenPresented()
            .hasGeoIntentWithTitle("Москва")
    }

    private static var appConfigurationWithoutRegionStub: ExternalAppConfiguration {
        return ExternalAppConfiguration(
            launchCount: 1,
            pushNotificationIntroWasShown: true,
            startupPromoWasShown: true,
            shouldDisplayEmbeddedMainFilters: false,
            mainListOpened: false,
            selectedTabItem: .search,
            isAuthorized: true,
            geoData: .emptyRegion(geoIntent: .fromRegion) // Do not stub region here, we want to check its fetching
        )
    }
}

// MARK: - Geo on Subsequent Launches

extension AppStartupTests {
    func testGeoIntentExistenceAfterAppLaunchWithOutdatedRegionConfiguration() {
        // There's a stub for RegionConfiguration only -
        // - we start with an outdated Configuration and expect the app will fetch it
        GeoAPIStubConfigurator.setupRegionInfo_Moscow(using: self.dynamicStubs)

        // Region has its own GeoIntent, so we don't provide any additional geo data
        let configuration = ExternalAppConfiguration.commonUITests
        configuration.launchCount = 2
        configuration.geoData = .fallbackWithOutdatedRegionConfiguration(geoIntent: .fromRegion)
        self.relaunchApp(with: configuration)

        let map = SearchResultsMapSteps()
        map
            .isScreenPresented()
            .hasGeoIntentWithTitle("Москва")
    }

    func testGeoIntentEmptinessAfterAppLaunchWithOutdatedRegionConfiguration() {
        // There's a stub for RegionConfiguration only -
        // - we start with an outdated Configuration and expect the app will fetch it
        GeoAPIStubConfigurator.setupRegionInfo_Moscow(using: self.dynamicStubs)

        let configuration = ExternalAppConfiguration.commonUITests
        configuration.launchCount = 2
        configuration.geoData = .fallbackWithOutdatedRegionConfiguration(geoIntent: .empty)
        self.relaunchApp(with: configuration)

        let map = SearchResultsMapSteps()
        map
            .isScreenPresented()
            .hasGeoIntentWithTitle(nil)
    }

    func testGeoIntentPreservationAfterAppLaunchWithOutdatedRegionConfiguration() {
        let geoIntentTitle = "Meatless"

        // There's a stub for RegionConfiguration only -
        // - we start with an outdated Configuration and expect the app will fetch it
        GeoAPIStubConfigurator.setupRegionInfo_Moscow(using: self.dynamicStubs)

        // Region has its own GeoIntent, and we also provide an additional geo data
        let configuration = ExternalAppConfiguration.commonUITests
        configuration.launchCount = 2
        configuration.geoData = .fallbackWithOutdatedRegionConfiguration(geoIntent: .customTitle(geoIntentTitle))
        self.relaunchApp(with: configuration)

        let map = SearchResultsMapSteps()
        map
            .isScreenPresented()
            .hasGeoIntentWithTitle(geoIntentTitle)
    }
}

// MARK: - Deeplinks on First Launch

extension AppStartupTests {
    // case: https://st.yandex-team.ru/VSAPPS-10430
    func testOpenSearchDeeplinkDuringFirstSessionAfterPaidSitesDisplay() {
        GeoAPIStubConfigurator.setupRegionAutodetectWithRegion_SpbLO(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupRegionInfo_SpbLO_hasPaidSites(using: self.dynamicStubs)
        
        let config = Self.appConfigurationWithoutRegionStub
        config.launchCount = 1 // Emphasize the first session
        self.relaunchApp(with: config)

        let searchResultsSteps = CommonSearchResultsSteps()
        let filtersSteps = FiltersSteps()

        searchResultsSteps
            .openFilters()
        filtersSteps
            .isScreenPresented()
            .switchToAction(.buy)
            .switchToCategory(.apartment)
            .submitFilters()

        APIStubConfigurator.setupOfferListDeeplink_RentRoom(using: self.dynamicStubs)
        self.communicator
            .sendDeeplink("https://realty.yandex.ru/sankt-peterburg/kupit/kvartira/dvuhkomnatnaya/")

        searchResultsSteps
            .openFilters()

        filtersSteps
            .isScreenPresented()
            .ensureAction(equalTo: .rent)
            .ensureCategory(equalTo: .room)
    }
}

// MARK: - Deferred Deeplinks

extension AppStartupTests {
    func disabled_testCommonDeferredDeeplinkIncoming() {
        self.applyAPIStubsForDeferredDeeplinking()

        let config = ExternalAppConfiguration.standardFirstLaunchUITests
        config.geoData = Self.deferredDeeplinkGeoData
        config.deferredDeeplink = Self.deferredDeeplinkData
        self.relaunchApp(with: config, startWithPromo: true)

        PushNotificationsIntroSteps()
            .isScreenPresented()
            .close()

        SearchResultsListSteps()
            .isScreenPresented()
            .isScreenTitle(equals: Self.geoIntentWithAppliedDeferredDeeplink)

        // TODO: check AnalyticsAppOpenedWithDeepLinkSourceEvent reported
        // TODO: check AnalyticsDeferredDeepLinkCameTooLateEvent not reported
    }

    /// Yeah, this is kinda the first launch and we should not skip this promo programatically,
    /// but here's the problem:
    /// - we want to check the app behavior on deferred deeplink which came too late
    /// - if the deeplink has coming when Push Promo is displayed, it's not "too late", even if the timer has expired
    /// - in this fucking XCTests it could take forever to wait when Push Promo is found on the screen
    /// So we cannot check this case with enabled Push Promo,
    /// and we cannot check the case when Push Promo is displayed too long to make our "too late" deeplink not "too late"
    func testCommonDeferredDeeplinkIncomingWithoutPushPromo() {
        self.applyAPIStubsForDeferredDeeplinking()

        let config = ExternalAppConfiguration.standardFirstLaunchUITests
        config.geoData = Self.deferredDeeplinkGeoData
        config.deferredDeeplink = Self.deferredDeeplinkData
        config.pushNotificationIntroWasShown = true
        self.relaunchApp(with: config)

        SearchResultsListSteps()
            .isScreenPresented()
            .isScreenTitle(equals: Self.geoIntentWithAppliedDeferredDeeplink)

        // TODO: check AnalyticsAppOpenedWithDeepLinkSourceEvent reported
        // TODO: check AnalyticsDeferredDeepLinkCameTooLateEvent not reported
    }

    func testLateDeferredDeeplinkIncoming() {
        self.applyAPIStubsForDeferredDeeplinking()

        let config = ExternalAppConfiguration.standardFirstLaunchUITests
        config.geoData = Self.deferredDeeplinkGeoData
        config.deferredDeeplink = Self.lateDeferredDeeplinkData
        // See description above the `testCommonDeferredDeeplinkIncomingWithoutPushPromo` method
        config.pushNotificationIntroWasShown = true
        self.relaunchApp(with: config)
        
        SearchResultsListSteps()
            .isScreenNotPresented()

        SearchResultsMapSteps()
            .isScreenPresented()
            .hasGeoIntentWithTitle(Self.geoIntentWithNonAppliedDeferredDeeplink)

        // TODO: check AnalyticsDeferredDeepLinkCameTooLateEvent reported
        // TODO: check AnalyticsAppOpenedWithDeepLinkSourceEvent not reported
    }

    func testNoDeferredDeeplinkProcessingOnSubsequentLaunch() {
        self.applyAPIStubsForDeferredDeeplinking()

        let config = ExternalAppConfiguration.standardSubsequentLaunchUITests
        config.geoData = Self.deferredDeeplinkGeoData
        config.startupPromoWasShown = true // to eliminate its 2 on the test
        config.deferredDeeplink = Self.deferredDeeplinkData
        self.relaunchApp(with: config)

        SearchResultsListSteps()
            .isScreenNotPresented()

        SearchResultsMapSteps()
            .isScreenPresented()
            .hasGeoIntentWithTitle(Self.geoIntentWithNonAppliedDeferredDeeplink)

        // TODO: check AnalyticsDeferredDeepLinkCameTooLateEvent not reported
        // TODO: check AnalyticsAppOpenedWithDeepLinkSourceEvent not reported
    }

    // MARK: Private

    private static var deferredDeeplinkData: ExternalAppConfiguration.DeferredDeeplink = {
        return .init(
            triggerAfter: 10,
            waitingTimeout: 20,
            url: URL(string: "https://realty.yandex.ru/orenburgskaya_oblast/kupit/kvartira/dvuhkomnatnaya/").yreForced()
        )
    }()

    private static var lateDeferredDeeplinkData: ExternalAppConfiguration.DeferredDeeplink = {
        return .init(
            triggerAfter: 10,
            waitingTimeout: 5,
            url: URL(string: "https://realty.yandex.ru/orenburgskaya_oblast/kupit/kvartira/dvuhkomnatnaya/").yreForced()
        )
    }()

    private static var deferredDeeplinkGeoData: ExternalAppConfiguration.GeoData = .fallback()

    private static var geoIntentWithAppliedDeferredDeeplink: String = "Оренбургская область"
    private static var geoIntentWithNonAppliedDeferredDeeplink: String = "Москва"

    private func applyAPIStubsForDeferredDeeplinking() {
        APIStubConfigurator.setupOfferListDeeplink_OrenburgRegion(using: self.dynamicStubs)
        GeoAPIStubConfigurator.setupRegionInfo_OrenburgRegion(using: self.dynamicStubs)
    }
}
