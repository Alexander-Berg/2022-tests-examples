//
//  UserOfferCardMosRuConnectionTests.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 16.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class UserOfferCardMosRuConnectionTests: BaseTestCase {
    override func setUp() {
        super.setUp()

        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOffersList(using: self.dynamicStubs, stubKind: .common)
    }

    func testPanelWithDisabledMosRu() {
        APIStubConfigurator.setupUserWithDisabledMosRu(using: self.dynamicStubs)
        self.checkConnectionPanelNonExistenceWithGivenState(stub: .notLinked)
    }

    func testNotLinkedState() {
        self.checkConnectionPanelExistenceWithGivenState(stub: .notLinked)
    }

    func testWaitForCheckNoFlatState() {
        self.checkConnectionPanelExistenceWithGivenState(stub: .waitForCheckNoFlat)
    }

    func testWaitForCheckFlatExistsState() {
        self.checkConnectionPanelExistenceWithGivenState(stub: .waitForCheckFlatExists)
    }

    func testTrustedNoFlatState() {
        self.checkConnectionPanelExistenceWithGivenState(stub: .trustedNoFlat)
    }

    func testTrustedNoRentFlatState() {
        self.checkConnectionPanelExistenceWithGivenState(stub: .trustedNoRentFlat)
    }

    func testTrustedMatchedFlatState() {
        self.checkConnectionPanelExistenceWithGivenState(stub: .trustedMatchedFlat)
    }

    func testTrustedMatchedRentFlatState() {
        self.checkConnectionPanelExistenceWithGivenState(stub: .trustedMatchedRentFlat)
    }

    func testReportNotReceivedState() {
        self.checkConnectionPanelExistenceWithGivenState(stub: .reportNotReceived)
    }

    func testNotTrustedState() {
        self.checkConnectionPanelExistenceWithGivenState(stub: .notTrusted)
    }

    func testNotOwnerState() {
        self.checkConnectionPanelExistenceWithGivenState(stub: .notOwner)
    }

    func testInternalErrorState() {
        self.checkConnectionPanelExistenceWithGivenState(stub: .internalError)
    }

    func testNotLinkedStateForLongRentOffer() {
        self.checkConnectionPanelExistenceWithGivenState(stub: .longRentOffer)
    }

    func testShortRentOffer() {
        APIStubConfigurator.setupUserWithEnabledMosRu(using: self.dynamicStubs)
        self.checkConnectionPanelNonExistenceWithGivenState(stub: .shortRentOffer)
    }

    func testOfferBanned() {
        APIStubConfigurator.setupUserWithEnabledMosRu(using: self.dynamicStubs)
        self.checkConnectionPanelNonExistenceWithGivenState(stub: .offerBanned)
    }

    func testOfferOnModeration() {
        APIStubConfigurator.setupUserWithEnabledMosRu(using: self.dynamicStubs)
        self.checkConnectionPanelNonExistenceWithGivenState(stub: .offerOnModeration)
    }

    func testNotLinkedStateForOfferRequiresPayment() {
        self.checkConnectionPanelExistenceWithGivenState(stub: .offerRequiresPayment)
    }

    // MARK: Private

    private func performCommonSteps(andThen specificCardSteps: (UserOffersCardSteps) -> Void) {
        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()

        let userOffersList = UserOffersListSteps()
        userOffersList
            .isScreenPresented()
            .isListNonEmpty()

        let cellSteps = userOffersList.cell(withIndex: 0)
        cellSteps.openCard()

        let userOfferCard = UserOffersCardSteps()
        userOfferCard.isScreenPresented()
        specificCardSteps(userOfferCard)
    }

    private func checkConnectionPanelExistenceWithGivenState(
        stub: UserOffersAPIStubConfigurator.StubKind.Card.MosRu
    ) {
        APIStubConfigurator.setupUserWithEnabledMosRu(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOffersCardWithMosRuInfo(using: self.dynamicStubs, stubKind: stub)
        
        self.relaunchApp(with: .userOfferTests)
        
        self.performCommonSteps(andThen: { card in
            card
                .isMosRuConnectionPanelPresented()
        })
    }

    private func checkConnectionPanelNonExistenceWithGivenState(
        stub: UserOffersAPIStubConfigurator.StubKind.Card.MosRu
    ) {
        UserOffersAPIStubConfigurator.setupUserOffersCardWithMosRuInfo(using: self.dynamicStubs, stubKind: stub)

        self.relaunchApp(with: .userOfferTests)

        self.performCommonSteps(andThen: { card in
            card.isMosRuConnectionPanelNotPresented()
        })
    }
}
