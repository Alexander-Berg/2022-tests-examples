//
//  UserOffersListTests.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 22.02.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import class YREAppConfig.ExternalAppConfiguration

final class UserOffersListTests: BaseTestCase {
    func testOpensUserOffersListWithBannedUser() {
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        APIStubConfigurator.setupBannedUser(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOffersList(using: self.dynamicStubs, stubKind: .common)

        let userOffersList = self.performCommonTest(launchConfig: .userOfferTests)

        userOffersList
            .isBannedUserViewPresented()
            .isAddOfferNavbarButtonNotExists()
            .isBannedUserHelpButtonTappable()
    }

    func testOpensUserOffersListWithUnsupportedFeatures() {
        UserOffersAPIStubConfigurator.setupUnsupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        APIStubConfigurator.setupUser(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOffersList(using: self.dynamicStubs, stubKind: .common)

        let userOffersList = self.performCommonTest(launchConfig: .userOfferTests)

        userOffersList
            .isListNotPresented()
            .isAddOfferNavbarButtonNotExists()

        AppUpdateStubSteps()
            .isScreenPresented()
            .isUpdateButtonTappable()
    }

    func testOpensUserOffersListWithNothingFoundState() {
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        APIStubConfigurator.setupAgentUser(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOffersList(using: self.dynamicStubs, stubKind: .common)

        let userOffersList = self.performCommonTest(launchConfig: .userOfferTests)

        userOffersList
            .isListPresented()
            .isAddOfferNavbarButtonTappable()
            .isFiltersButtonTappable()
            .tapFiltersButton()

        UserOffersAPIStubConfigurator.setupUserOffersList(using: self.dynamicStubs, stubKind: .empty)
        let filterSteps = UserOffersFilterSteps()
        filterSteps
            .isScreenPresented()
            .switchToAction(.sell)
            .submitFilters()

        userOffersList
            .isNothingFoundViewPresented()
    }

    func testChatsPromoBanner() {
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        APIStubConfigurator.setupUser(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOffersList(using: self.dynamicStubs, stubKind: .common)

        let userOffersList = self.performCommonTest(launchConfig: .userOfferWithPromoTests)

        userOffersList
            .isListPresented()

        let promoBanner = userOffersList.chatsPromo()

        promoBanner
            .isPresented()
            .isHittable()
            .toggle()
            .tapOnHide()
            .isDismissed()
    }

    func testChatsPromoBannerForAgent() {
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        APIStubConfigurator.setupLegalEntityUser(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOffersList(using: self.dynamicStubs, stubKind: .common)

        let userOffersList = self.performCommonTest(launchConfig: .userOfferWithPromoTests)

        userOffersList
            .isListPresented()

        let promoBanner = userOffersList.chatsPromo()

        promoBanner
            .isDismissed()
    }

    private func performCommonTest(launchConfig: ExternalAppConfiguration) -> UserOffersListSteps {
        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)

        self.relaunchApp(with: launchConfig)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()

        let userOffersListSteps = UserOffersListSteps()
            .isScreenPresented()

        return userOffersListSteps
    }
}
