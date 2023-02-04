//
//  SearchResultsReporterTests.swift
//  YREServiceLayer-Unit-Tests
//
//  Created by Fedor Solovev on 13.10.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREModel
@testable import YREServiceLayer

final class SearchResultsReporterTests: XCTestCase {
    func testWhenShouldBeCorrectAndReport() {
        // given
        let subscriptionListingAnalytics = AnalyticsMock()
        let subscriptionListingCorrector = ResponseInfoCorrectorMock()
        let subscriptionListingReporter = SubscriptionListingSearchResultsReporter(analyticsReporter: subscriptionListingAnalytics,
                                                                                   responseInfoCorrector: subscriptionListingCorrector)
        let newbuildingOfferListingPlanAnalytics = AnalyticsMock()
        let newbuildingOfferListingPlanCorrector = ResponseInfoCorrectorMock()
        let newbuildingOfferListingPlanReporter = NewbuildingOfferListingPlanSearchResultsReporter(
            analyticsReporter: newbuildingOfferListingPlanAnalytics,
            responseInfoCorrector: newbuildingOfferListingPlanCorrector
        )
        let siteResellerOffersListAnalytics = AnalyticsMock()
        let siteResellerOffersListCorrector = ResponseInfoCorrectorMock()
        let siteResellerOffersListReporter = SiteResellerOffersListSearchResultsReporter(
            analyticsReporter: siteResellerOffersListAnalytics,
            responseInfoCorrector: siteResellerOffersListCorrector
        )
        let multihousePlanSearchAnalytics = AnalyticsMock()
        let multihousePlanSearchCorrector = ResponseInfoCorrectorMock()
        let multihousePlanSearchReporter = MultihousePlanSearchResultsReporter(analyticsReporter: multihousePlanSearchAnalytics,
                                                                               responseInfoCorrector: multihousePlanSearchCorrector)
        let villageCardListingAnalytics = AnalyticsMock()
        let villageCardListingCorrector = ResponseInfoCorrectorMock()
        let villageCardListingReporter = VillageCardListingSearchResultsReporter(analyticsReporter: villageCardListingAnalytics,
                                                                                 responseInfoCorrector: villageCardListingCorrector)
        let listingSearchAnalytics = AnalyticsMock()
        let listingSearchCorrector = ResponseInfoCorrectorMock()
        let listingSearchReporter = ListingSearchResultsReporter(analyticsReporter: listingSearchAnalytics,
                                                                 responseInfoCorrector: listingSearchCorrector)
        let newbuildingListingAnalytics = AnalyticsMock()
        let newbuildingListingCorrector = ResponseInfoCorrectorMock()
        let newbuildingListingReporter = NewbuildingListingSearchResultsReporter(analyticsReporter: newbuildingListingAnalytics,
                                                                                 responseInfoCorrector: newbuildingListingCorrector)
        let villageListingAnalytics = AnalyticsMock()
        let villageListingCorrector = ResponseInfoCorrectorMock()
        let villageListingReporter = VillageListingSearchResultsReporter(analyticsReporter: villageListingAnalytics,
                                                                         responseInfoCorrector: villageListingCorrector)

        // when
        let responseInfo: ResponseInfo = .init(
            searchQuery: .init(logQueryId: "", logQueryText: "", sort: nil),
            pager: .init(page: 0, pageSize: 0, totalItems: 0, totalPages: 0)
        )
        let reporters: [SearchResultsReporterProtocol] = [
            subscriptionListingReporter,
            newbuildingOfferListingPlanReporter,
            siteResellerOffersListReporter,
            multihousePlanSearchReporter,
            villageCardListingReporter,
            listingSearchReporter,
            newbuildingListingReporter,
            villageListingReporter
        ]
        reporters.forEach { $0.report(responseInfo) }

        // then
        let expectedReportHandlers: [XCTestExpectation] = [
            subscriptionListingAnalytics.report,
            newbuildingOfferListingPlanAnalytics.report,
            siteResellerOffersListAnalytics.report,
            multihousePlanSearchAnalytics.report,
            villageCardListingAnalytics.report,
            listingSearchAnalytics.report,
            newbuildingListingAnalytics.report,
            villageListingAnalytics.report
        ]

        let expectedCorrectHandlers: [XCTestExpectation] = [
            subscriptionListingCorrector.correct,
            newbuildingOfferListingPlanCorrector.correct,
            siteResellerOffersListCorrector.correct,
            multihousePlanSearchCorrector.correct,
            villageCardListingCorrector.correct,
            listingSearchCorrector.correct,
            newbuildingListingCorrector.correct,
            villageListingCorrector.correct
        ]
        self.wait(for: expectedReportHandlers + expectedCorrectHandlers, timeout: Self.timeout)
    }

    func testWhenShouldBeOnlyReport() {
        // given
        let mapSearchAnalytics = AnalyticsMock()
        let mapSearchReporter = MapSearchResultsReporter(analyticsReporter: mapSearchAnalytics)

        let newbuildingMapSearchAnalytics = AnalyticsMock()
        let newbuildingMapSearchReporter = NewbuildingMapSearchResultsReporter(analyticsReporter: newbuildingMapSearchAnalytics)

        let villageMapSearchAnalytics = AnalyticsMock()
        let villageMapSearchReporter = VillageMapSearchResultsReporter(analyticsReporter: villageMapSearchAnalytics)

        // when
        let responseInfo: ResponseInfo = .init(
            searchQuery: .init(logQueryId: "", logQueryText: "", sort: nil),
            pager: .init(page: 0, pageSize: 0, totalItems: 0, totalPages: 0)
        )
        let reporters: [SearchResultsReporterProtocol] = [
            mapSearchReporter,
            newbuildingMapSearchReporter,
            villageMapSearchReporter,
        ]
        reporters.forEach { $0.report(responseInfo) }

        // then
        let expectedReportHandlers: [XCTestExpectation] = [
            mapSearchAnalytics.report,
            newbuildingMapSearchAnalytics.report,
            villageMapSearchAnalytics.report,
        ]

        self.wait(for: expectedReportHandlers, timeout: Self.timeout)
    }

    private static let timeout: TimeInterval = 1
}
