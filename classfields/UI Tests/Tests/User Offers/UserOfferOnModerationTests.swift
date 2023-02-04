//
//  UserOfferOnModerationTests.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 19.03.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class UserOfferOnModerationTests: BaseTestCase {
    override func setUp() {
        super.setUp()

        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        APIStubConfigurator.setupUser(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOfferPreview(using: self.dynamicStubs)
    }
    
    func testFreeUserOfferOnModeration() {
        UserOffersAPIStubConfigurator.setupOnModerationUserOffersList(using: self.dynamicStubs, stubKind: .free)
        UserOffersAPIStubConfigurator.setupOnModerationUserOffersCard(using: self.dynamicStubs, stubKind: .free)
        
        self.performCommonTests(card: { cardSteps in
            cardSteps
                .isActivePlacementView(presented: false)
                .isInactivePlacementView(presented: false)
                .isActivateButton(tappable: false)
        })
    }

    func testUnpaidUserOfferOnModeration() {
        UserOffersAPIStubConfigurator.setupOnModerationUserOffersList(using: self.dynamicStubs, stubKind: .unpaid)
        UserOffersAPIStubConfigurator.setupOnModerationUserOffersCard(using: self.dynamicStubs, stubKind: .unpaid)

        self.performCommonTests(card: { cardSteps in
            cardSteps
                .isActivePlacementView(presented: false)
                .isInactivePlacementView(presented: true)
                .isActivateButton(tappable: true)
        })
    }

    func testPaidUserOfferOnModeration() {
        UserOffersAPIStubConfigurator.setupOnModerationUserOffersList(using: self.dynamicStubs, stubKind: .paid)
        UserOffersAPIStubConfigurator.setupOnModerationUserOffersCard(using: self.dynamicStubs, stubKind: .paid)

        self.performCommonTests(card: { cardSteps in
            cardSteps
                .isActivePlacementView(presented: true)
                .isInactivePlacementView(presented: false)
        })
    }

    /// Offers from Feed always have `free placement`, so we have only one test
    func testUserOfferOnModerationFromFeed() {
        UserOffersAPIStubConfigurator.setupOnModerationUserOffersList(using: self.dynamicStubs, stubKind: .fromFeed)
        UserOffersAPIStubConfigurator.setupOnModerationUserOffersCard(using: self.dynamicStubs, stubKind: .fromFeed)

        self.performCommonTests(card: { cardSteps in
            cardSteps
                .isActivePlacementView(presented: false)
                .isInactivePlacementView(presented: false)
                .isActivateButton(tappable: false)
        })
    }

    private func performCommonTests(card specificCardSteps: (UserOffersCardSteps) -> Void) {
        self.relaunchApp(with: .userOfferTests)

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
        userOfferCard
            .isPriceEditIconNotExists()
            .isPriceEditButtonDisabled()
            .isAddPhotosButtonNotExists()
            .isUnpublishButtonNotExists()
            .isProlongationButtonNotExists()
            .isPublishButton(tappable: false)
            .isRemoveButtonNotExists()
            .scrollToPreviewButton()
            .isOfferPreviewButtonTappable()
            .isEditButtonNotExists()
        userOfferCard.openOfferPreview()

        let previewOfferCard = OfferCardSteps()
        previewOfferCard
            .isOfferCardPresented()
            .isCallButtonNotExists()
    }
}
