//
//  DeeplinkUITests.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 22/11/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest

final class DeeplinkCardTests: BaseTestCase {
    func testOpensOfferCard() {
        APIStubConfigurator.setupOfferCardDeeplink(using: self.dynamicStubs)
        APIStubConfigurator.setupOfferCard(using: self.dynamicStubs)

        self.relaunchApp(with: .commonUITests)

        self.communicator
            .sendDeeplink("https://realty.yandex.ru/offer/5462851513175475378/")
        OfferCardSteps()
            .isOfferCardPresented()
            .isCallButtonTappable()
        RootNavigationSteps()
            .dismissModallyPresented()
    }
    
    func testOpensSiteCard() {
        APIStubConfigurator.setupSiteCardDeeplink(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCardLevelAmurskaya(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupOfferStat(using: self.dynamicStubs, site: .levelAmurskaya)

        self.relaunchApp(with: .commonUITests)

        self.communicator
            .sendDeeplink("https://realty.yandex.ru/moskva/kupit/novostrojka/level-amurskaya/?id=521570")
        SiteCardSteps()
            .isScreenPresented()
            .isCallButtonTappable()
        RootNavigationSteps()
            .dismissModallyPresented()
    }

    func testOpensVillageCard() {
        APIStubConfigurator.setupVillageCardDeeplink(using: self.dynamicStubs)
        APIStubConfigurator.setupVillageCard(using: self.dynamicStubs)

        self.relaunchApp(with: .commonUITests)

        self.communicator
            .sendDeeplink("https://realty.yandex.ru/moskva/kupit/kottedzhnye-poselki/marsel/?id=1831744")
        VillageCardSteps()
            .isScreenPresented()
            .isCallButtonTappable()
        RootNavigationSteps()
            .dismissModallyPresented()
    }
}
