//
//  BannedUserOfferTests.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 19.03.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class BannedUserOfferTests: BaseTestCase {
    override func setUp() {
        super.setUp()

        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        APIStubConfigurator.setupUser(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOfferPreview(using: self.dynamicStubs)
    }

    /// Banned offers shouldn't be affected by `placement`, so we have only one test
    func testCommonOffer() {
        UserOffersAPIStubConfigurator.setupBannedUserOffersList(using: self.dynamicStubs, stubKind: .common)
        UserOffersAPIStubConfigurator.setupBannedUserOffersCard(using: self.dynamicStubs, stubKind: .common)
        self.performCommonListTests()

        let userOfferCard = UserOffersCardSteps()
        let previewOfferCard = OfferCardSteps()

        userOfferCard
            .isScreenPresented()
            .isActivePlacementView(presented: false)
            .isInactivePlacementView(presented: false)
            .isActivateButton(tappable: false)
            .isPriceEditIconTappable()
            .isPriceEditButtonTappable()
            .isAddPhotosButtonTappable()
            .isUnpublishButtonNotExists()
            .isProlongationButtonNotExists()
            .isPublishButton(tappable: false)
            .scrollToRemoveButton()
            .isRemoveButtonTappable()
            .scrollToPreviewButton()
            .isOfferPreviewButtonTappable()
            .isOpenSupportButtonTappable()
            .isEditButtonTappable()
            .openOfferPreview()

        previewOfferCard
            .isOfferCardPresented()
            .isCallButtonNotExists()
    }

    func testOfferFromFeed() {
        UserOffersAPIStubConfigurator.setupBannedUserOffersList(using: self.dynamicStubs, stubKind: .fromFeed)
        UserOffersAPIStubConfigurator.setupBannedUserOffersCard(using: self.dynamicStubs, stubKind: .fromFeed)
        self.performCommonListTests()

        let userOfferCard = UserOffersCardSteps()
        let previewOfferCard = OfferCardSteps()

        userOfferCard
            .isScreenPresented()
            .isActivePlacementView(presented: false)
            .isInactivePlacementView(presented: false)
            .isActivateButton(tappable: false)
            .isPriceEditIconNotExists()
            .isPriceEditButtonDisabled()
            .isAddPhotosButtonNotExists()
            .isProlongationButtonNotExists()
            .isPublishButton(tappable: false)
            .isUnpublishButtonNotExists()
            .isRemoveButtonNotExists()
            .scrollToPreviewButton()
            .isOfferPreviewButtonTappable()
            .isOpenSupportButtonTappable()
            .isEditButtonNotExists()
            .openOfferPreview()

        previewOfferCard
            .isOfferCardPresented()
            .isCallButtonNotExists()
    }

    func testOpenSupportChatFromCard() {
        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupBannedUserOffersList(using: self.dynamicStubs, stubKind: .fromFeed)
        UserOffersAPIStubConfigurator.setupBannedUserOffersCard(using: self.dynamicStubs, stubKind: .fromFeed)

        self.relaunchApp(with: .userOfferTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()

        let userOffersList = UserOffersListSteps()
        let userOfferCard = UserOffersCardSteps()
        let chat = ChatSteps()

        userOffersList
            .isScreenPresented()
            .cell(withIndex: 0)
            .openCard()
        userOfferCard
            .isScreenPresented()
            .scrollToOpenSupportButton()
            .openSupport()
        chat
            .isScreenPresented()
    }

    private func performCommonListTests() {
        let userOffersList = UserOffersListSteps()

        self.relaunchApp(with: .userOfferTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnUserOffersListSection()

        userOffersList
            .isScreenPresented()
            .isListNonEmpty()
            .cell(withIndex: 0)
            .compareWithScreenshot(identifier: "userOffers.list.cell.banned")
            .openCard()
    }
}
