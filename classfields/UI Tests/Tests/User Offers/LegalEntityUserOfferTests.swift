//
//  LegalEntityUserOfferTests.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 04.06.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

//
// We don't check Published Unpaid cases here, because that's a wrong state.
// We don't check Banned cases here, because there should be no difference with a common user tests.
final class LegalEntityUserOfferPlacementStatusTests: BaseTestCase {
    override func setUp() {
        super.setUp()

        InAppServicesStubConfigurator.setupServiceInfoWithOfferListOnly(using: self.dynamicStubs)
        UserOffersAPIStubConfigurator.setupAllSupportedRequiredFeaturesForUserOffers(using: self.dynamicStubs)
        APIStubConfigurator.setupLegalEntityUser(using: self.dynamicStubs)
    }

    func testUnpublishedWithInProgressStatus() {
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersList(using: self.dynamicStubs,
                                                                     stubKind: .legalEntityInProgress)
        self.performCommonSteps(snippetSteps: { cellSteps in
            cellSteps
                .compareWithScreenshot(identifier: "userOffers.list.cell.legalEntity.unpublished.placementInProgress")
                .isEditOfferButtonTappable()
        })
    }

    func testUnpublishedWithNotEnoughFundsError() {
        UserOffersAPIStubConfigurator.setupUnpublishedUserOffersList(using: self.dynamicStubs,
                                                                     stubKind: .legalEntityNotEnoughFunds)
        self.performCommonSteps(snippetSteps: { cellSteps in
            cellSteps
                .compareWithScreenshot(identifier: "userOffers.list.cell.legalEntity.unpublished.placementNotEnoughFunds")
                .isEditOfferButtonTappable()
        })
    }

    func testOnModerationWithInProgressStatus() {
        UserOffersAPIStubConfigurator.setupOnModerationUserOffersList(using: self.dynamicStubs,
                                                                      stubKind: .legalEntityInProgress)
        self.performCommonSteps(snippetSteps: { cellSteps in
            cellSteps
                .compareWithScreenshot(identifier: "userOffers.list.cell.legalEntity.moderation.placementInProgress")
        })
    }

    func testOnModerationWithNotEnoughFundsError() {
        UserOffersAPIStubConfigurator.setupOnModerationUserOffersList(using: self.dynamicStubs,
                                                                      stubKind: .legalEntityNotEnoughFunds)
        self.performCommonSteps(snippetSteps: { cellSteps in
            cellSteps
                .compareWithScreenshot(identifier: "userOffers.list.cell.legalEntity.moderation.placementNotEnoughFunds")
        })
    }

    // MARK: Private

    private func performCommonSteps(snippetSteps: (UserOfferSnippetSteps) -> Void) {
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
        snippetSteps(cellSteps)
    }
}
