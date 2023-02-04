//
//  SiteCardTests.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 15.06.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class SiteCardTests: BaseTestCase {}

// MARK: - Card

extension SiteCardTests {
    func testCallsFromCard() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .billing)

        self.performCommonTests { cardSteps in
            let callScenarios = SiteCallScenarios(
                dynamicStubs: self.dynamicStubs,
                callButtonHandler: cardSteps
            )
            callScenarios
                .runBillingOnCall()
                .runCallWithTwoPhoneNumbers()
                .runCallWithError()
        }
    }

    // Application form unavailable
    func testFlatStatusOnSaleWithUnavailableRegion() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListNovosibirsk(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCardNovaDom(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupOfferStatNoPrimaryOffers(using: self.dynamicStubs, site: .novaDom)

        self.performCommonTests { cardSteps in
            cardSteps
                .isSubmitApplicationUnavailable()
                .scrollToNoDevelopersCell()
                .isNoDevelopersCellAvailable()
                .scrollNoDevelopersCellToCenter()
                .compareCellWithScreenshot(identifier: "siteCard.flatStatusOnSale.unavailableRegion", cell: .noDeveloper)
        }
    }

    func testFlatStatusNotOnSale() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .notOnSale)
        SiteCardAPIStubConfiguration.setupOfferStatNoPrimaryOffers(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonTests { cardSteps in
            cardSteps
                .isSubmitApplicationUnavailable()
                .scrollToNoDevelopersCell()
                .isNoDevelopersCellAvailable()
                .scrollNoDevelopersCellToCenter()
                .compareCellWithScreenshot(identifier: "siteCard.flatStatusNotOnSale", cell: .noDeveloper)
        }
    }

    func testCeilingHeightCellSingleValue() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .ceilingHeight(.singleValue))
        SiteCardAPIStubConfiguration.setupOfferStatNoPrimaryOffers(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonTests { cardSteps in
            cardSteps
                .scrollToCardInfoCell(.ceilingHeight)
                .makeCardInfoCellScreenshot(.ceilingHeight, identifier: "ceilingHeight_singleValue")
        }
    }

    func testCeilingHeightCellRange() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .ceilingHeight(.range))
        SiteCardAPIStubConfiguration.setupOfferStatNoPrimaryOffers(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonTests { cardSteps in
            cardSteps
                .scrollToCardInfoCell(.ceilingHeight)
                .makeCardInfoCellScreenshot(.ceilingHeight, identifier: "ceilingHeight_range")
        }
    }

    func testFlatStatusSoonAvailable() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .soonAvailable)
        SiteCardAPIStubConfiguration.setupOfferStatNoPrimaryOffers(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonTests { cardSteps in
            cardSteps
                .isSubmitApplicationUnavailable()
                .scrollToNoDevelopersCell()
                .isNoDevelopersCellAvailable()
                .scrollNoDevelopersCellToCenter()
                .compareCellWithScreenshot(identifier: "siteCard.flatStatusSoonAvailable", cell: .noDeveloper)
        }
    }

    func testFlatStatusTemporaryUnavailable() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .temporaryUnavailable)
        SiteCardAPIStubConfiguration.setupOfferStatNoPrimaryOffers(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonTests { cardSteps in
            cardSteps
                .isSubmitApplicationUnavailable()
                .scrollToNoDevelopersCell()
                .isNoDevelopersCellAvailable()
                .scrollNoDevelopersCellToCenter()
                .compareCellWithScreenshot(identifier: "siteCard.flatStatusTemporaryUnavailable", cell: .noDeveloper)
        }
    }

    func testFlatStatusSoldFlatWithStatParams() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .sold(.flatWithStatParams))
        SiteCardAPIStubConfiguration.setupOfferStatNoPrimaryOffers(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonTests { cardSteps in
            cardSteps
                .isSubmitApplicationUnavailable()
                .scrollToNoDevelopersCell()
                .isNoDevelopersCellAvailable()
                .scrollNoDevelopersCellToCenter()
                .compareCellWithScreenshot(identifier: "siteCard.flatStatusSoldFlatWithStatParams", cell: .noDeveloper)
        }
    }

    func testFlatStatusSoldApartmentWithStatParams() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .sold(.apartmentWithStatParams))
        SiteCardAPIStubConfiguration.setupOfferStatNoPrimaryOffers(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonTests { cardSteps in
            cardSteps
                .isSubmitApplicationUnavailable()
                .scrollToNoDevelopersCell()
                .isNoDevelopersCellAvailable()
                .scrollNoDevelopersCellToCenter()
                .compareCellWithScreenshot(identifier: "siteCard.flatStatusSoldApartmentWithStatParams", cell: .noDeveloper)
        }
    }

    func testFlatStatusSoldFlatsAndApartmentsWithStatParams() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .sold(.flatsAndApartmentsWithStatParams))
        SiteCardAPIStubConfiguration.setupOfferStatNoPrimaryOffers(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonTests { cardSteps in
            cardSteps
                .isSubmitApplicationUnavailable()
                .scrollToNoDevelopersCell()
                .isNoDevelopersCellAvailable()
                .scrollNoDevelopersCellToCenter()
                .compareCellWithScreenshot(identifier: "siteCard.flatStatusSoldFlatsAndApartmentsWithStatParams", cell: .noDeveloper)
        }
    }

    // with resale offers
    func testFlatStatusSoldNoPrimaryOffers() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .sold(.flat))
        SiteCardAPIStubConfiguration.setupOfferStatNoPrimaryOffers(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonTests { cardSteps in
            cardSteps
                .isSubmitApplicationUnavailable()
                .scrollToNoDevelopersCell()
                .isNoDevelopersCellAvailable()
                .scrollNoDevelopersCellToCenter()
                .compareCellWithScreenshot(identifier: "siteCard.flatStatusSoldNoPrimaryOffers", cell: .noDeveloper)
        }
    }

    func testFlatStatusSoldNoAnyOffers() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .sold(.flat))
        SiteCardAPIStubConfiguration.setupOfferStatNoAnyOffers(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonTests { cardSteps in
            cardSteps
                .isSubmitApplicationUnavailable()
                .scrollToNoDevelopersCell()
                .isNoDevelopersCellAvailable()
                .scrollNoDevelopersCellToCenter()
                .compareCellWithScreenshot(identifier: "siteCard.flatStatusSoldNoAnyOffers", cell: .noDeveloper)
        }
    }

    func testFlatStatusInProject() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .inProject)
        SiteCardAPIStubConfiguration.setupOfferStatNoAnyOffers(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonTests { cardSteps in
            cardSteps
                .isSubmitApplicationUnavailable()
                .scrollToNoDevelopersCell()
                .isNoDevelopersCellAvailable()
                .scrollNoDevelopersCellToCenter()
                .compareCellWithScreenshot(identifier: "siteCard.flatStatusInProject", cell: .noDeveloper)
        }
    }

    func testFlatStatusUnknown() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .unknown)
        SiteCardAPIStubConfiguration.setupOfferStatNoPrimaryOffers(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonTests { cardSteps in
            cardSteps
                .isSubmitApplicationUnavailable()
                .scrollToNoDevelopersCell()
                .isNoDevelopersCellAvailable()
                .scrollNoDevelopersCellToCenter()
                .compareCellWithScreenshot(identifier: "siteCard.flatStatusUnknown", cell: .noDeveloper)
        }
    }

    func testFlatStatusUnknownNoAnyOffers() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .unknown)
        SiteCardAPIStubConfiguration.setupOfferStatNoAnyOffers(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonTests { cardSteps in
            cardSteps
                .isSubmitApplicationUnavailable()
                .scrollToNoDevelopersCell()
                .isNoDevelopersCellAvailable()
                .scrollNoDevelopersCellToCenter()
                .compareCellWithScreenshot(identifier: "siteCard.flatStatusUnknownNoAnyOffers", cell: .noDeveloper)
        }
    }

    // MARK: Photos

    func testNoPhoto() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .noPhoto)

        self.performCommonTests { cardSteps in
            cardSteps
                .compareCellWithScreenshot(identifier: "noPhoto", cell: .photos)
        }
    }

    func testSinglePhotoLayout() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .singlePhoto)

        self.performCommonTests { cardSteps in
            cardSteps
                .compareCellWithScreenshot(identifier: "singlePhoto", cell: .photos)
        }
    }

    func testSeveralPhotosLayout() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .severalPhotos)

        self.performCommonTests { cardSteps in
            cardSteps
                .compareCellWithScreenshot(identifier: "severalPhotos", cell: .photos)
        }
    }

    // https://st.yandex-team.ru/VSAPPS-8218#602d0fd4f38a0d7a84b68f79
    func testPhotoFallbackWhenNoLargeImages() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .noLargeImages)

        self.performCommonTests { cardSteps in
            cardSteps
                .compareCellWithScreenshot(identifier: "photoFallback", cell: .photos)
        }
    }

    func testOpensReviews() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCardLevelAmurskaya(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupOfferStat(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonTests { cardSteps in
            cardSteps
                .tapOnAnchor("Отзывы")

            let webPage = WebPageSteps()
            webPage.screenIsPresented()
        }
    }
    
    func testReviewsButtonExist() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .withReviews)
        SiteCardAPIStubConfiguration.setupOfferStatNoPrimaryOffers(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonTests { cardSteps in
            cardSteps.isAnchorExist("Отзывы")
        }
    }
    
    func testReviewsButtonNotExist() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .withoutReviews)
        SiteCardAPIStubConfiguration.setupOfferStatNoPrimaryOffers(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonTests { cardSteps in
            cardSteps.isAnchorNotExist("Отзывы")
        }
    }

    func testCallbackCell() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .callback)
        SiteCardAPIStubConfiguration.setupOfferStatNoPrimaryOffers(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonTests { cardSteps in
            cardSteps
                .scrollToCallbackCell()
                .tapOnCallbackCellPrivacyLink()
            
            ModalActionsAlertSteps()
                .isScreenPresented()
        }
    }

    func testToursCell() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .tours)
        SiteCardAPIStubConfiguration.setupOfferStatNoPrimaryOffers(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonTests { cardSteps in
            cardSteps
                .scrollToToursCell()
                .taOnTourCell(index: 0)

            WebPageSteps()
                .screenIsPresented()
                .tapOnCloseButton()
                .screenIsDismissed()
        }
    }

    func testOverviewsCell() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .overviews)
        SiteCardAPIStubConfiguration.setupOfferStatNoPrimaryOffers(using: self.dynamicStubs, site: .levelAmurskaya)

        self.performCommonTests { cardSteps in
            cardSteps
                .scrollToToursCell()
                .taOnTourCell(index: 0)

            WebPageSteps()
                .screenIsPresented()
                .tapOnCloseButton()
                .screenIsDismissed()
        }
    }

    func testBlocksOrder() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .allBlocks)
        SiteCardAPIStubConfiguration.setupOfferStatNoPrimaryOffers(using: self.dynamicStubs, site: .levelAmurskaya)

        // swiftlint:disable:next closure_body_length
        self.performCommonTests { cardSteps in
            cardSteps
                .isAnchorsCellAvailable()

            cardSteps
                .scrollToSubfilters()
                .isSiteSubfiltersPresented()

            cardSteps
                .scrollToRoomStatisticsCell(title: "1-комнатные")
                .isRoomStatisticsCellTappable(title: "1-комнатные")

            cardSteps
                .scrollToResaleOffers()
                .isResaleOffersCellTappable()

            cardSteps
                .scrollToPlanCell()
                .isPlanCellTappable()

            cardSteps
                .scrollToPriceHistoryCell()
                .isPriceHistoryCellAvailable()

            cardSteps
                .scrollToToursCell()
                .isToursCellAvailable()

            cardSteps
                .scrollToLocationCell()
                .isLocationCellAvailable()

            cardSteps
                .scrollToDecorationCell()
                .isDecorationCellAvailable()

            cardSteps
                .scrollToConstructionStateCell()
                .isConstructionStateCellAvailable()

            cardSteps
                .scrollToSpecialProposalsCell()
                .isSpecialProposalsCellAvailable()

            cardSteps
                .scrollToDeveloperCell()
                .isDeveloperCellAvailable()
    
            cardSteps
                .scrollToDocumentsCell()
                .isDocumentsCellAvailable()

            cardSteps
                .scrollToCallbackCell()
                .isCallbackCellAvailable()
        }
    }

    // MARK: - Private

    private func performCommonTests(specificCardTests: (SiteCardSteps) -> Void) {
        self.relaunchApp(with: .cardTests)

        SearchResultsListSteps()
            .isScreenPresented()
            .withSiteList()
            .isListNonEmpty()
            .cell(withIndex: 0)
            .isPresented()
            .isCallButtonTappable()
            .callButtonLabelStarts(with: "Позвонить")
            .tap()

        let siteCardSteps = SiteCardSteps()
        siteCardSteps
            .isScreenPresented()
            .isLoadingIndicatorHidden()
        specificCardTests(siteCardSteps)
    }
}

// MARK: - Site Offers

extension SiteCardTests {
    func testCallsFromOfferPlans() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscowBilling(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .billing)
        SiteCardAPIStubConfiguration.setupOfferStat(using: self.dynamicStubs, site: .severniyFontan)

        self.performCommonTests { cardSteps in
            // Replace the previous stub ASAP to ensure it will be accessible when we'll fetch offer list by plan
            SiteOfferListByPlanAPIStubConfigurator.setupSiteSearchResultsListByPlan(
                using: self.dynamicStubs
            )

            let siteOfferListByPlan = cardSteps
                .scrollToRoomStatisticsCell(title: "1-комнатные")
                .tapRoomStatisticsCell(title: "1-комнатные")

            let callScenarios = SiteCallScenarios(
                dynamicStubs: self.dynamicStubs,
                callButtonHandler: siteOfferListByPlan
            )
            callScenarios
                .runBillingOnCall()
                .runCallWithTwoPhoneNumbers()
                .runCallWithError()
        }
    }

    func testCallsFromSiteOfferList() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscowBilling(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .billing)

        SiteCardAPIStubConfiguration.setupOfferStat(using: self.dynamicStubs, site: .severniyFontan)
        OfferPlanListAPIStubConfigurator.setupOfferPlanList(
            using: self.dynamicStubs,
            siteID: SiteCardAPIStubConfiguration.Consts.Site.severniyFontan.siteId
        )
        OfferCardAPIStubConfiguration.setupOfferCard(using: self.dynamicStubs)

        self.performCommonTests { cardSteps in
            // Replace the previous stub ASAP to ensure it will be accessible when we'll fetch offer list by plan
            SiteOfferListByPlanAPIStubConfigurator.setupSiteSearchResultsListByPlan(
                using: self.dynamicStubs
            )

            cardSteps
                .scrollToRoomStatisticsCell(title: "1-комнатные")
                .tapRoomStatisticsCell(title: "1-комнатные")

            let siteOfferListByPlanSteps = SiteOfferListByPlanSteps()
            siteOfferListByPlanSteps
                .isScreenPresented()

            let callScenarios = SiteCallScenarios(
                dynamicStubs: self.dynamicStubs,
                callButtonHandler: siteOfferListByPlanSteps
            )
            callScenarios
                .runBillingOnCall()
                .runCallWithTwoPhoneNumbers()
                .runCallWithError()
        }
    }

    func testCallsFromOfferPlanList() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscowBilling(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .billing)

        SiteCardAPIStubConfiguration.setupOfferStat(using: self.dynamicStubs, site: .severniyFontan)
        OfferPlanListAPIStubConfigurator.setupOfferPlanList(
            using: self.dynamicStubs,
            siteID: SiteCardAPIStubConfiguration.Consts.Site.severniyFontan.siteId
        )
        OfferCardAPIStubConfiguration.setupOfferCard(using: self.dynamicStubs)

        self.performCommonTests { cardSteps in
            // Replace the previous stub ASAP to ensure it will be accessible when we'll fetch offer list by plan
            SiteOfferListByPlanAPIStubConfigurator.setupSiteSearchResultsListByPlan(
                using: self.dynamicStubs
            )

            cardSteps
                .scrollToPlanCell()
                .tapPlanCell()

            let siteOfferListByPlanSteps = OfferPlanListSteps()
            siteOfferListByPlanSteps
                .isScreenPresented()

            let callScenarios = SiteCallScenarios(
                dynamicStubs: self.dynamicStubs,
                callButtonHandler: siteOfferListByPlanSteps
            )
            callScenarios
                .runBillingOnCall()
                .runCallWithTwoPhoneNumbers()
                .runCallWithError()
        }
    }
}

// MARK: - Gallery

extension SiteCardTests {
    func testCallsFromGallery() {
        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .billing)

        self.performCommonTests { cardSteps in
            cardSteps
                .tapPhotosCell()

            let callScenarios = SiteCallScenarios(
                dynamicStubs: self.dynamicStubs,
                callButtonHandler: AnyOfferGallerySteps()
            )
            callScenarios
                .runBillingOnCall()
                .runCallWithTwoPhoneNumbers()
                .runCallWithError()
        }
    }
}
