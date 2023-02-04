//
//  PublishedUserOfferTests.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 19.03.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

final class PublishedUserOfferTests: BaseTestCase {
    override func setUp() {
        super.setUp()

        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        APIStubConfigurator.setupUser(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOfferPreview(using: self.dynamicStubs)
    }

    func testPaidPublishedUserOffer() {
        UserOffersAPIStubConfigurator.setupPublishedUserOffersList(using: self.dynamicStubs, stubKind: .paid)
        UserOffersAPIStubConfigurator.setupPublishedUserOffersCard(using: self.dynamicStubs, stubKind: .paid)

        self.performCommonTests(specificSnippetTests: { snippetSteps in
            snippetSteps
                .compareWithScreenshot(identifier: "userOffers.list.cell.publishedPaid")
                .isEditPriceButtonTappable()
                .isPriceEditControlTappable()
        },
                                specificCardTests: { cardSteps in
            cardSteps
                .isActivePlacementView(presented: true)
                .isInactivePlacementView(presented: false)
                .isPriceEditIconTappable()
                .isPriceEditButtonTappable()
                .isAddPhotosButtonTappable()
                .isProlongationButtonNotExists()
                .isPublishButton(tappable: false)
                .isRemoveButtonNotExists()
                .scrollToUnpublishButton()
                .isUnpublishButtonTappable()
                .scrollToPreviewButton()
                .isOfferPreviewButtonTappable()
                .isEditButtonTappable()
        })
    }

    func testUnpaidPublishedUserOffer() {
        UserOffersAPIStubConfigurator.setupPublishedUserOffersList(using: self.dynamicStubs, stubKind: .unpaid)
        UserOffersAPIStubConfigurator.setupPublishedUserOffersCard(using: self.dynamicStubs, stubKind: .unpaid)

        self.performCommonTests(specificSnippetTests: { snippetSteps in
            snippetSteps
                .compareWithScreenshot(identifier: "userOffers.list.cell.publishedUnpaid")
                .isPublishButtonTappable()
        },
                                specificCardTests: { cardSteps in
            cardSteps
                .isInactivePlacementView(presented: true)
                .isActivateButton(tappable: true)
                .isPriceEditIconTappable()
                .isPriceEditButtonTappable()
                .isAddPhotosButtonTappable()
                .isUnpublishButtonNotExists()
                .isProlongationButtonNotExists()
                .scrollToPublishButton()
                .isPublishButton(tappable: false)
                .scrollToRemoveButton()
                .isRemoveButtonTappable()
                .scrollToPreviewButton()
                .isOfferPreviewButtonTappable()
                .isEditButtonTappable()
        })
    }

    func testFreePublishedUserOffer() {
        UserOffersAPIStubConfigurator.setupPublishedUserOffersList(using: self.dynamicStubs, stubKind: .free)
        UserOffersAPIStubConfigurator.setupPublishedUserOffersCard(using: self.dynamicStubs, stubKind: .free)

        self.performCommonTests(specificSnippetTests: { snippetSteps in
            snippetSteps
                .compareWithScreenshot(identifier: "userOffers.list.cell.publishedFree")
                .isEditPriceButtonTappable()
                .isPriceEditControlTappable()
        },
                                specificCardTests: { cardSteps in
            cardSteps
                .isActivePlacementView(presented: false)
                .isInactivePlacementView(presented: false)
                .isActivateButton(tappable: false)
                .isPriceEditIconTappable()
                .isPriceEditButtonTappable()
                .isAddPhotosButtonTappable()
                .isProlongationButtonNotExists()
                .isPublishButton(tappable: false)
                .isRemoveButtonNotExists()
                .scrollToUnpublishButton()
                .isUnpublishButtonTappable()
                .scrollToPreviewButton()
                .isOfferPreviewButtonTappable()
                .isEditButtonTappable()
        })
    }

    /// Offers from Feed always have `free placement`, so we have only one test
    func testPublishedUserOfferFromFeed() {
        UserOffersAPIStubConfigurator.setupPublishedUserOffersList(using: self.dynamicStubs, stubKind: .fromFeed)
        UserOffersAPIStubConfigurator.setupPublishedUserOffersCard(using: self.dynamicStubs, stubKind: .fromFeed)

        self.performCommonTests(specificSnippetTests: { snippetSteps in
            snippetSteps
                .compareWithScreenshot(identifier: "userOffers.list.cell.publishedFromFeed")
        },
                                specificCardTests: { cardSteps in
            cardSteps
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
                .isEditButtonNotExists()
        })
    }
    
    private func performCommonTests(specificSnippetTests: (UserOfferSnippetSteps) -> Void,
                                    specificCardTests: (UserOffersCardSteps) -> Void) {
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
        specificSnippetTests(cellSteps)
        cellSteps.openCard()

        let userOfferCard = UserOffersCardSteps()
        userOfferCard.isScreenPresented()
        specificCardTests(userOfferCard)
        userOfferCard.openOfferPreview()

        let previewOfferCard = OfferCardSteps()
        previewOfferCard
            .isOfferCardPresented()
            .isCallButtonNotExists()
    }
}
