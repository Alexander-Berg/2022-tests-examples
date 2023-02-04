//
//  UnpublishedUserOfferTests.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 19.03.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

final class UnpublishedUserOfferTests: BaseTestCase {
    override func setUp() {
        super.setUp()

        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        APIStubConfigurator.setupUser(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupUserOfferPreview(using: self.dynamicStubs)
    }

    func testPaidUnpublishedUserOffer() {
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersList(using: self.dynamicStubs, stubKind: .paid)
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersCard(using: self.dynamicStubs, stubKind: .paid)

        self.performCommonTests(specificSnippetTests: { snippetSteps in
            snippetSteps
                .isRemoveButtonTappable()
                .isPublishButtonTappable()
                .compareWithScreenshot(identifier: "userOffers.list.cell.unpublishedPaid")
        },
                                specificCardTests: { cardSteps in
            cardSteps
                .isActivePlacementView(presented: false)
                .isInactivePlacementView(presented: false)
                .isActivateButton(tappable: false)
                .isPriceEditIconTappable()
                .isPriceEditButtonTappable()
                .isAddPhotosButtonTappable()
                .isUnpublishButtonNotExists()
                .isProlongationButtonNotExists()
                .scrollToPublishButton()
                .isPublishButton(tappable: true)
                .scrollToRemoveButton()
                .isRemoveButtonTappable()
                .scrollToPreviewButton()
                .isOfferPreviewButtonTappable()
                .isEditButtonTappable()
        })
    }

    func testUnpaidUnpublishedUserOffer() {
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersList(using: self.dynamicStubs, stubKind: .unpaid)
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersCard(using: self.dynamicStubs, stubKind: .unpaid)

        self.performCommonTests(specificSnippetTests: { snippetSteps in
            snippetSteps
                .isEditOfferButtonTappable()
                .isPublishButtonTappable()
                .compareWithScreenshot(identifier: "userOffers.list.cell.unpublishedUnpaid")
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

    func testUnpaidUnpublishedUserOfferWithPendingPlacement() {
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersList(using: self.dynamicStubs, stubKind: .unpaidWithPendingPlacement)
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersCard(using: self.dynamicStubs, stubKind: .unpaidWithPendingPlacement)

        self.performCommonTests(specificSnippetTests: { snippetSteps in
            snippetSteps
                .isEditOfferButtonTappable()
                .compareWithScreenshot(identifier: "userOffers.list.cell.unpublishedPendingPlacement")
        },
                                specificCardTests: { cardSteps in
            cardSteps
                .isInactivePlacementView(presented: false)
                .isActivateButton(tappable: false)
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

    func testFreeUnpublishedUserOffer() {
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersList(using: self.dynamicStubs, stubKind: .free)
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersCard(using: self.dynamicStubs, stubKind: .free)

        self.performCommonTests(specificSnippetTests: { snippetSteps in
            snippetSteps
                .isRemoveButtonTappable()
                .isPublishButtonTappable()
                .compareWithScreenshot(identifier: "userOffers.list.cell.unpublishedFree")
        },
                                specificCardTests: { cardSteps in
            cardSteps
                .isActivePlacementView(presented: false)
                .isInactivePlacementView(presented: false)
                .isActivateButton(tappable: false)
                .isPriceEditIconTappable()
                .isPriceEditButtonTappable()
                .isAddPhotosButtonTappable()
                .isUnpublishButtonNotExists()
                .isProlongationButtonNotExists()
                .scrollToPublishButton()
                .isPublishButton(tappable: true)
                .scrollToRemoveButton()
                .isRemoveButtonTappable()
                .scrollToPreviewButton()
                .isOfferPreviewButtonTappable()
                .isEditButtonTappable()
        })
    }

    /// Offers from Feed always have `free placement`, so we have only one test
    func testUnpublishedUserOfferFromFeed() {
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersList(using: self.dynamicStubs, stubKind: .fromFeed)
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersCard(using: self.dynamicStubs, stubKind: .fromFeed)

        self.performCommonTests(specificSnippetTests: { snippetSteps in
            snippetSteps
                .compareWithScreenshot(identifier: "userOffers.list.cell.unpublishedFromFeed")
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
