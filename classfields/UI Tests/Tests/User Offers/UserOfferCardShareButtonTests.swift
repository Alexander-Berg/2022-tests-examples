//
//  UserOfferCardShareButtonTests.swift
//  UI Tests
//
//  Created by Evgenii Novikov on 30.11.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

final class UserOfferCardShareButtonTests: BaseTestCase {
    func testUserOfferCardWithShareLinkInResponse() {
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupPublishedUserOffersCard(using: self.dynamicStubs, stubKind: .free)
        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)
        
        self.relaunchApp(with: .inAppServicesTests)
        
        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
        
        InAppServicesUserOffersSteps()
            .isSectionPresentedAsList()
            .tapOnOffer(at: 3)
        
        UserOffersCardSteps()
            .isScreenPresented()
            .isShareButtonTappable()
    }
    
    func testUserOfferCardWithoutShareLinkInResponse() {
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupPublishedUserOffersCard(using: self.dynamicStubs, stubKind: .withoutShareLink)
        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)
        
        self.relaunchApp(with: .inAppServicesTests)
        
        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
        
        InAppServicesUserOffersSteps()
            .isSectionPresentedAsList()
            .tapOnOffer(at: 3)
        
        UserOffersCardSteps()
            .isScreenPresented()
            .isShareButtonNotExists()
    }
}
