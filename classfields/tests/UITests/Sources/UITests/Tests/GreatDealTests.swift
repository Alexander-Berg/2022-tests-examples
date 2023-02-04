//
//  GreatDealTests.swift
//  UITests
//
//  Created by Alexander Malnev on 4/27/20.
//

import XCTest
import AutoRuProtoModels
import SwiftProtobuf
import Snapshots

/// @depends_on AutoRuViews AutoRuCellHelpers
class GreatDealBadgesTests: BaseTest {
    let suiteName = SnapshotIdentifier.suiteName(from: #file)

    lazy var mainSteps = MainSteps(context: self)

    override var appSettings: [String: Any] {
        var value = super.appSettings
        value["enableGreatDeal"] = true
        return value
    }

    override func setUp() {
        super.setUp()
        setupServer()
        launch()
    }

    // MARK: - Helpers

    private func setupServer() {

        server.addHandler("POST /device/hello") { (request, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        try! server.start()
    }

    private func setupListingResponse(stubName: String) {
         server.addHandler("POST /search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc") { (request, _) -> Response? in
             return Response.okResponse(fileName: stubName)
         }
    }

    private func setupOfferResponse(stubName: String) {
        server.addHandler("GET /offer/CARS/1") { (request, _) -> Response? in
            return Response.okResponse(fileName: stubName)
        }
    }

    @discardableResult
    private func routeToListing() -> SaleCardListSteps {
        return mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .wait(for: 1)
            .tapStatusBar() // this is neccessary to dismiss notification
            .scrollToOffer(with: "1")
    }

    @discardableResult
    private func routeToOfferCard() -> SaleCardSteps {
        return routeToListing()
            .openCarOffer(with: "1")
    }

    private func validateGreatDealBadgeNotExist(priceBadge: String) {
        let priceBadge = app.staticTexts[priceBadge].firstMatch
        XCTAssertFalse(priceBadge.waitForExistence(timeout: 1))
    }

    private func validateGreatDealBadgeExists(priceBadge: String) {
        let priceBadge = app.staticTexts[priceBadge].firstMatch
        XCTAssertTrue(priceBadge.waitForExistence(timeout: 1))
    }

    private func screenshotForBadge(acessibilityId: String) -> UIImage {
        return app.descendants(matching: .any).matching(identifier: acessibilityId).firstMatch.screenshot().image
    }

    private func validateBadgeSnapshots(accessibilityId: String, snapshotId: String) {
        let snapshotId = SnapshotIdentifier(suite: suiteName, identifier: snapshotId)
        let screenshot = screenshotForBadge(acessibilityId: accessibilityId)

        Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotId, perPixelTolerance: 0.01)
    }

    // MARK: - Tests

    func test_listingGoodDealBadgeExists() {
        setupListingResponse(stubName: "great_deal_good_deal_response")
        routeToListing()

        validateGreatDealBadgeExists(priceBadge: "Хорошая цена")
    }

    func test_listingExcellentDealBadgeExists() {
        setupListingResponse(stubName: "great_deal_excellent_deal_response")
        routeToListing()

        validateGreatDealBadgeExists(priceBadge: "Отличная цена")
    }

    func test_listingGoodDealHighlightedBadgeExists() {
        setupListingResponse(stubName: "great_deal_good_deal_highlighted_response")
        routeToListing()

        validateGreatDealBadgeExists(priceBadge: "Хорошая цена")
    }

    func test_listingExcellentDealHighlightedBadgeExists() {
        setupListingResponse(stubName: "great_deal_excellent_deal_highlighted_response")
        routeToListing()

        validateGreatDealBadgeExists(priceBadge: "Отличная цена")
    }

    func test_listingNoGoodDealBadgesExist() {
        setupListingResponse(stubName: "great_deal_no_deals_response")
        routeToListing()

        validateGreatDealBadgeNotExist(priceBadge: "Хорошая цена")
        validateGreatDealBadgeNotExist(priceBadge: "Отличная цена")
    }

    func test_offerGoodDealBadgeExists() {
        setupListingResponse(stubName: "great_deal_good_deal_response")
        setupOfferResponse(stubName: "great_deal_good_deal_offer_response")

        routeToOfferCard()
        validateGreatDealBadgeExists(priceBadge: "Хорошая цена")
    }

    func test_offerExcellentDealBadgeExists() {
        setupListingResponse(stubName: "great_deal_excellent_deal_response")
        setupOfferResponse(stubName: "great_deal_excellent_deal_offer_response")

        routeToOfferCard()
        validateGreatDealBadgeExists(priceBadge: "Отличная цена")
    }

    func test_offerNoExcellentDealBadgeExists() {
        setupListingResponse(stubName: "great_deal_no_deals_response")
        setupOfferResponse(stubName: "great_deal_no_deals_offer_response")

        routeToOfferCard()

        validateGreatDealBadgeNotExist(priceBadge: "Отличная цена")
        validateGreatDealBadgeNotExist(priceBadge: "Хорошая цена")
    }
}
