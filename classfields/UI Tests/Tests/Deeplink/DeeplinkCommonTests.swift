//
//  DeeplinkCommonTests.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 08.04.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class DeeplinkCommonTests: BaseTestCase {
    func testOpensVillageCardWithExplicitRegionInDeeplink() {
        GeoAPIStubConfigurator.setupRegionInfo_Moscow(using: self.dynamicStubs)
        APIStubConfigurator.setupVillageCardDeeplinkWithMoscowRegion(using: self.dynamicStubs)
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
    
    func testOpensFavoritesDeeplink() {
        APIStubConfigurator.setupFavoritesDeeplink(using: self.dynamicStubs)
        self.relaunchApp(with: .commonUITests)
        
        self.communicator
            .sendDeeplink("https://realty.yandex.ru/favorites/")
        FavoritesListSteps()
            .screenIsPresented()
    }
    
    func testOpensSavedSearchesDeeplink() {
        APIStubConfigurator.setupSavedSearchesDeeplink(using: self.dynamicStubs)
        self.relaunchApp(with: .commonUITests)
        
        self.communicator
            .sendDeeplink("https://realty.yandex.ru/subscriptions/")
        SavedSearchesListSteps()
            .screenIsPresented()
    }

    func testOpensTechSupportChatDeeplink() {
        APIStubConfigurator.setupSavedSearchesDeeplink(using: self.dynamicStubs)
        self.relaunchApp(with: .commonUITests)

        self.communicator
            .sendDeeplink("https://realty.yandex.ru/chat/techsupport")
        ChatSteps()
            .isScreenPresented()
    }

    func testShouldOpenJournalDeeplink() {
        APIStubConfigurator.setupJournalDeeplink(using: self.dynamicStubs)
        self.relaunchApp(with: .commonUITests)

        self.communicator
            .sendDeeplink("https://m.realty.yandex.ru/journal/?only-content=true")
        WebPageSteps()
            .screenIsPresented()
    }

    func testShouldOpenInAppServicesDeeplink() {
        APIStubConfigurator.setupInAppServicesDeeplink(using: self.dynamicStubs)
        self.relaunchApp(with: .commonUITests)

        self.communicator
            .sendDeeplink("https://realty.yandex.ru/services")
        InAppServicesSteps()
            .isScreenPresented()
    }

    func testShouldOpenOwnerLandingDeeplink() {
        APIStubConfigurator.setupInAppServicesDeeplink(using: self.dynamicStubs)
        self.relaunchApp(with: .commonUITests)
        
        self.communicator
            .sendDeeplink("https://realty.yandex.ru/arenda/owner-landing")
        WebPageSteps()
            .screenIsPresented()
            .tapOnCloseButton()
            .screenIsDismissed()
        InAppServicesSteps()
            .isScreenPresented()
    }

    func testShouldOpenOwnerApplication() {
        APIStubConfigurator.setupInAppServicesDeeplink(using: self.dynamicStubs)
        self.relaunchApp(with: .commonUITests)

        self.communicator
            .sendDeeplink("https://arenda.yandex.ru/lk/sdat-kvartiry")
        YaRentOwnerApplicationSteps()
            .isScreenPresented()
            .tapOnCloseButton()
            .isScreenNotPresented()
        InAppServicesSteps()
            .isScreenPresented()
    }

    func testShouldOpenSimilarOffersDeeplink() {
        let offerID = "8626959308009774024"
        OfferCardAPIStubConfiguration.setupSimilarOffer(using: self.dynamicStubs, for: offerID)
        self.relaunchApp(with: .commonUITests)

        self.communicator
            .sendDeeplink("https://realty.yandex.ru/offer/\(offerID)/similar/")
        PredefinedOffersListSteps()
            .isScreenPresented()
            .isListNonEmpty()
            .tapOnCloseButton()
            .isScreenNotPresented()
    }

    func testDeeplinkWithUnknownAction() {
        APIStubConfigurator.setupUnknownActionDeeplink(using: self.dynamicStubs)
        self.relaunchApp(with: .commonUITests)

        self.communicator
            .sendDeeplink("https://realty.yandex.ru/journal/?only-content=true")
        DeepLinkProcessingSteps()
            .isScreenPresented()
            .compareSnapshot(snapshotID: "deeplinkProcessing.unknownActionError")
    }
}
