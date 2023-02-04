//
//  DeeplinkUserOfferTests.swift
//  UI Tests
//
//  Created by Ibrakhim Nikishin on 11/2/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import class YREAppConfig.ExternalAppConfiguration

final class DeeplinkUserOfferTests: BaseTestCase {
    func testOpensUserOffersList() {
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        APIStubConfigurator.setupUser(using: self.dynamicStubs)

        let config = ExternalAppConfiguration.commonUITests
        config.isAuthorized = true
        self.relaunchApp(with: config)

        self.communicator
            .sendDeeplink("https://realty.yandex.ru/management")
        UserOffersListSteps()
            .isScreenPresented()
    }
    
    func testOpensUserOffer() {
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        APIStubConfigurator.setupUser(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupPublishedUserOffersCard(using: self.dynamicStubs, stubKind: .free)

        self.relaunchApp(with: .userOfferTests)

        self.communicator.sendDeeplink(
            "https://realty.yandex.ru/management-new/offer/\(UserOffersAPIStubConfigurator.userOfferID)"
        )
        UserOffersCardSteps()
            .isScreenPresented()
    }
}
