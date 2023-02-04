//
//  ConciergeTests.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 6/11/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig

final class ConciergeTests: BaseTestCase {
    func testConciergeBannerOnFilters() {
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
            .isConciergeBannerExists()
            .switchToAction(.rent)
            .isConciergeBannerHidden()

        filtersSteps
            .switchToAction(.buy)
            .switchToCategory(.house)
            .isConciergeBannerHidden()

        filtersSteps
            .switchToCategory(.room)
            .isConciergeBannerExists()
            .tapOnConciergeBanner()

        let conciergeSteps = ConciergeSteps()

        conciergeSteps
            .isScreenPresented()
            .typePhone()
            .tapSubmit()
            .isSuccessAlertPresented()
            .tapCloseAlert()
            .isScreenNotPresented()
    }

    func testConciergeBannerOnMainListing() {
        ConciergeAPIStubConfigurator(dynamicStubs: self.dynamicStubs).setupConciergeError()
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscowBilling(using: self.dynamicStubs)

        let configuration = ExternalAppConfiguration.snippetsTests
        configuration.geoData.regionData = .custom(regionType: .moscowAndRegion)
        self.relaunchApp(with: configuration)

        let searchResultsSteps = SearchResultsListSteps()

        searchResultsSteps
            .isScreenPresented()
            .scrollToConcierge()
            .tapOnConcierge()

        let conciergeSteps = ConciergeSteps()

        conciergeSteps
            .isScreenPresented()
            .typePhone()
            .tapSubmit()
            .isErrorAlertPresented()
            .tapCloseAlert()
            .isScreenPresented()
    }

    func testConciergeBannerOnMainListingWithOpenPhoneScreen() {
        ConciergeAPIStubConfigurator(dynamicStubs: self.dynamicStubs).setupConciergeError()
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscowBilling(using: self.dynamicStubs)

        let configuration = ExternalAppConfiguration.snippetsTests
        configuration.geoData.regionData = .custom(regionType: .moscowAndRegion)
        self.relaunchApp(with: configuration)

        let searchResultsSteps = SearchResultsListSteps()

        searchResultsSteps
            .isScreenPresented()
            .scrollToConcierge()
            .tapOnConcierge()

        let conciergeSteps = ConciergeSteps()

        conciergeSteps
            .isScreenPresented()
            .tapCallMe()

        let conciergePhoneSteps = CallApplicationSteps()

        conciergePhoneSteps
            .isScreenPresented()
            .typePhone()
            .tapSubmit()
            .isErrorAlertPresented()
            .tapCloseAlert()
            .isScreenPresented()
    }
}
