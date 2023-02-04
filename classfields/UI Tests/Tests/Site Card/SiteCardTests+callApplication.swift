//
//  SiteCardTests+callApplication.swift
//  UI Tests
//
//  Created by Alexey Salangin on 11.11.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import Swifter

extension SiteCardTests {
    func testCallApplication() {
        let rawPhone = "8005553535"
        let sanitizedPhone = "+7\(rawPhone)"

        let expectedBody = ConciergeTicketBody(
            phone: sanitizedPhone,
            url: "https://realty.yandex.ru/newbuilding/521570/",
            rgid: "587795",
            sitePayload: .init(siteId: "521570")
        )
        let predicate = Predicate<HttpRequest>.body(expectedBody)

        SiteCardAPIStubConfiguration.setupSiteSearchResultsListMoscow(using: self.dynamicStubs)
        SiteCardAPIStubConfiguration.setupSiteCard(using: self.dynamicStubs, stubKind: .onSale)
        SiteCardAPIStubConfiguration.setupOfferStatNoPrimaryOffers(using: self.dynamicStubs, site: .levelAmurskaya)

        let expectation = XCTestExpectation()
        ConciergeAPIStubConfigurator(dynamicStubs: self.dynamicStubs).setupConcierge(
            predicate: predicate,
            handler: { expectation.fulfill() }
        )

        self.relaunchApp(with: .cardTests)

        SearchResultsListSteps()
            .isScreenPresented()
            .withSiteList()
            .isListNonEmpty()
            .cell(withIndex: 0)
            .isPresented()
            .tap()

        let cardSteps = SiteCardSteps()
        cardSteps
            .isScreenPresented()
            .isLoadingIndicatorHidden()
            .scrollToSubmitApplicationView()
            .isSubmitApplicationAvailable()
            .scrollSubmitApplicationViewToCenter()
            .isSubmitApplicationButtonTappable()

        let submitApplicationSteps = cardSteps.tapOnSubmitApplicationButton()
        submitApplicationSteps
            .isScreenPresented()
            .typePhone(rawPhone)
            .tapSubmit()
            .isSuccessAlertPresented()
            .tapCloseAlert()
            .isScreenNotPresented()

        let result = XCTWaiter.yreWait(for: [expectation], timeout: 30)
        XCTAssertTrue(result)
    }

    private struct ConciergeTicketBody: Codable & Equatable {
        struct SitePayload: Codable & Equatable {
            let siteId: String
        }

        let phone: String
        let url: String
        let rgid: String
        let sitePayload: SitePayload
    }
}
