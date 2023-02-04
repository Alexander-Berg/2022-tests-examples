//
//  UserOffersOverQuotaAlertTests.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 20.08.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig
// TODO: Eliminate this dependency in https://st.yandex-team.ru/VSAPPS-7215
import YRESettings

final class UserOffersOverQuotaAlertTests: BaseTestCase {
    override func setUp() {
        super.setUp()
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
    }

    func testFirstAlertAppearance() {
        APIStubConfigurator.setupUserWithOverQuota(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOffersList(using: self.dynamicStubs, stubKind: .common)
        self.launchUserOffersList(with: .userOfferOverQuotaTests)

        let rootNavigation = RootNavigationSteps()
        let userOffersList = UserOffersListSteps()
        let overQuotaAlert = userOffersList.overQuotaAlert()
        let browser = overQuotaAlert.browser()

        userOffersList
            .isScreenPresented()
            .isListPresented()
        overQuotaAlert
            .isScreenPresented()
            .tapOnReadRulesButton()
            .isScreenNotPresented()
        browser
            .isScreenPresented()
            .tapOnCloseButton()
            .isScreenNotPresented()
        overQuotaAlert
            .tapOnAcceptButton()
            .isScreenNotPresented()
        rootNavigation
            .tabBarTapOnProfileItem()
            .tabBarTapOnHomeItem()
        overQuotaAlert.isScreenNotPresented()
    }

    func testSubsequentAlertAppearance() {
        APIStubConfigurator.setupUserWithOverQuota(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOffersList(using: self.dynamicStubs, stubKind: .common)

        let configuration = ExternalAppConfiguration.userOfferOverQuotaTests
        configuration.userOffersOverQuotaAlertShownAt = Date().addingTimeInterval(-self.appearanceDelay)
        self.launchUserOffersList(with: configuration)

        let userOffersList = UserOffersListSteps()
        let overQuotaAlert = userOffersList.overQuotaAlert()
        overQuotaAlert
            .isScreenPresented()
    }

    func testSubsequentAlertAppearanceAfterTinyDelay() {
        APIStubConfigurator.setupUserWithOverQuota(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOffersList(using: self.dynamicStubs, stubKind: .common)

        let configuration = ExternalAppConfiguration.userOfferOverQuotaTests
        // This delay must not be enough to display the Alert
        let delay = self.appearanceDelay * 0.5
        configuration.userOffersOverQuotaAlertShownAt = Date().addingTimeInterval(-delay)
        self.launchUserOffersList(with: configuration)

        let userOffersList = UserOffersListSteps()
        let overQuotaAlert = userOffersList.overQuotaAlert()

        userOffersList
            .isScreenPresented()
            .isListPresented()
            .isAuthViewNotPresented()
        overQuotaAlert
            .isScreenNotPresented()
    }

    func testCompletelyClosedAlert() {
        APIStubConfigurator.setupUserWithOverQuota(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOffersList(using: self.dynamicStubs, stubKind: .common)
        self.relaunchApp(with: .userOfferOverQuotaTests)

        let configuration = ExternalAppConfiguration.userOfferOverQuotaTests
        configuration.userOffersOverQuotaAlertMayBeShown = false
        self.launchUserOffersList(with: configuration)

        let userOffersList = UserOffersListSteps()
        let overQuotaAlert = userOffersList.overQuotaAlert()

        userOffersList
            .isScreenPresented()
            .isListPresented()
            .isAuthViewNotPresented()
        overQuotaAlert
            .isScreenNotPresented()
    }

    // Uses common User model without `over quota` property. No over quota - no alert.
    func testNoOverQuota() {
        APIStubConfigurator.setupUser(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOffersList(using: self.dynamicStubs, stubKind: .common)
        self.launchUserOffersList(with: .userOfferOverQuotaTests)

        let userOffersList = UserOffersListSteps()
        let overQuotaAlert = userOffersList.overQuotaAlert()

        userOffersList
            .isScreenPresented()
            .isListPresented()
            .isAuthViewNotPresented()
        overQuotaAlert
            .isScreenNotPresented()
    }

    func testNoLoadedUserOffersList() {
        APIStubConfigurator.setupUserWithOverQuota(using: self.dynamicStubs)
        // No stub for a list of UserOffers

        self.launchUserOffersList(with: .userOfferOverQuotaTests)

        let userOffersList = UserOffersListSteps()
        let overQuotaAlert = userOffersList.overQuotaAlert()

        userOffersList
            .isScreenPresented()
        overQuotaAlert
            .isScreenNotPresented()
    }

    // Not authenticated User - no alert.
    func testNoAuth() {
        APIStubConfigurator.setupUserWithOverQuota(using: self.dynamicStubs)

        let configuration = ExternalAppConfiguration.userOfferOverQuotaTests
        configuration.isAuthorized = false
        self.launchUserOffersList(with: configuration)

        let userOffersList = UserOffersListSteps()
        let overQuotaAlert = userOffersList.overQuotaAlert()

        userOffersList
            .isScreenPresented()
            .isAuthViewPresented()
        overQuotaAlert
            .isScreenNotPresented()
    }

    func testAlertLayout() {
        APIStubConfigurator.setupUserWithOverQuota(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOffersList(using: self.dynamicStubs, stubKind: .common)
        self.launchUserOffersList(with: .userOfferOverQuotaTests)

        let userOffersList = UserOffersListSteps()
        let overQuotaAlert = userOffersList.overQuotaAlert()

        userOffersList
            .isScreenPresented()
        overQuotaAlert
            .isScreenPresented()
    }

    // MARK: Private

    private let appearanceDelay: TimeInterval = YRESettings().business.userOffersOverQuotaAlertDisplayingThreshold

    private func launchUserOffersList(with config: ExternalAppConfiguration) {
        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)

        self.relaunchApp(with: config)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()
    }
}

extension ExternalAppConfiguration {
    fileprivate static var userOfferOverQuotaTests: ExternalAppConfiguration {
        let configuration = Self.commonUITests
        configuration.isAuthorized = true
        configuration.selectedTabItem = .home
        configuration.userOffersOverQuotaAlertMayBeShown = true
        configuration.userOffersOverQuotaAlertShownAt = nil
        return configuration
    }
}
