//
//  InfinityListingTests.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 25.02.2021.
//

import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuServices AutoRuSaleList
class InfinityListingTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)
    var settings: [String: Any] = [:]

    override var appSettings: [String: Any] {
        return settings
    }

    override func setUp() {
        super.setUp()
        settings = super.appSettings
        settings["currentHosts"] = [
            "PublicAPI": "http://127.0.0.1:\(port)/"
        ]

        mockListingBase()
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .startMock()
    }

    func test_onlyExtendedResultUsed() {
        mocker
            .mock_searchHistory(state: .used)
            .mock_searchCars(newCount: 0, usedCount: 0)
            .mock_searchOfferLocatorCounters(type: .all, distances: [.init(radius: 400, count: 1)])
        launch()

        mainSteps
            .openSearchHistory("LADA (ВАЗ) 1111 Ока")
            .validateSnapshot(of: "startInfinityListingTitle", snapshotId: "startInfinityListingTitle_1_white")
            .validateSnapshot(of: "empty_result", snapshotId: "empty_result")
    }

    func test_onlyExtendedResultNew() {
        mocker
            .mock_searchHistory(state: .new)
            .mock_searchCars(newCount: 0, usedCount: 0)
            .mock_searchOfferLocatorCounters(type: .all, distances: [.init(radius: 400, count: 1)])

        launch()

        mainSteps
            .openSearchHistory("LADA (ВАЗ) 1111 Ока")
            .validateSnapshot(of: "startInfinityListingTitle", snapshotId: "startInfinityListingTitle_1_white")
            .validateSnapshot(of: "empty_result", snapshotId: "empty_result")
    }

    func test_onlyExtendedResultAll() {
        mocker
            .mock_searchHistory(state: .all)
            .mock_searchCars(newCount: 0, usedCount: 0)
            .mock_searchOfferLocatorCounters(type: .all, distances: [.init(radius: 400, count: 1)])

        launch()

        mainSteps
            .openSearchHistory("LADA (ВАЗ) 1111 Ока")
            .validateSnapshot(of: "startInfinityListingTitle", snapshotId: "startInfinityListingTitle_1_white")
            .validateSnapshot(of: "empty_result", snapshotId: "empty_result")
    }

    func test_allResultsUsed() {
        mocker
            .mock_searchHistory(state: .used)
            .mock_searchCars(newCount: 0, usedCount: 1)
            .mock_searchOfferLocatorCounters(type: .all, distances: [.init(radius: 400, count: 1)])

        launch()

        mainSteps
            .openSearchHistory("LADA (ВАЗ) 1111 Ока")
            .scrollTo("startInfinityListingTitle")
            .validateSnapshot(of: "startInfinityListingTitle", snapshotId: "startInfinityListingTitle_1_gray")
    }

    func test_allResultsAll() {
        mocker
            .mock_searchHistory(state: .all)
            .mock_searchCars(newCount: 1, usedCount: 1)
            .mock_searchOfferLocatorCounters(type: .all, distances: [.init(radius: 400, count: 1)])

        launch()

        mainSteps
            .openSearchHistory("LADA (ВАЗ) 1111 Ока")
            .scrollTo("startInfinityListingTitle")
            .validateSnapshot(of: "startInfinityListingTitle", snapshotId: "startInfinityListingTitle_1_gray")
    }

    func test_allResultsNew_notShowedIL() {
        mocker
            .mock_searchHistory(state: .new)
            .mock_searchCars(newCount: 2, usedCount: 0)
            .mock_searchOfferLocatorCounters(type: .all, distances: [.init(radius: 400, count: 1)])

        launch()

        mainSteps
            .openSearchHistory("LADA (ВАЗ) 1111 Ока")
            .scrollToOffer(with: "1", position: .footer)
            .notExist(selector: "startInfinityListingTitle")
    }

    func test_infinityListingSnippetHasFullDistanceInfo() {
        mocker
            .mock_searchHistory(state: .all)
            .mock_searchCars(newCount: 0, usedCount: 0)
            .mock_searchOfferLocatorCounters(type: .all)

        launch()

        mainSteps
            .openSearchHistory("LADA (ВАЗ) 1111 Ока")
            .scrollToOffer(with: "100_0", position: .footer)
            .validateSnapshot(of: "offer_snippet_address_100_0", snapshotId: "infinity_listing_offer_snippet_address")
    }

    func test_checkInfinityListingExistAfterLongListing() {
        mocker
            .mock_searchHistory(state: .all)
            .mock_searchCars(newCount: 0, usedCount: 22)
            .mock_searchOfferLocatorCounters(type: .all)

        launch()

        mainSteps
            .openSearchHistory("LADA (ВАЗ) 1111 Ока")
            .scrollTo("startInfinityListingTitle", maxSwipes: 40)
            .exist(selector: "startInfinityListingTitle")
    }

    func test_subscriptionButtonAfter11Offer() {
        mocker
            .mock_searchHistory(state: .all)
            .mock_searchCars(newCount: 0, usedCount: 12)
            .mock_searchOfferLocatorCounters(type: .all, distances: [.init(radius: 400, count: 4)])

        launch()

        mainSteps
            .openSearchHistory("LADA (ВАЗ) 1111 Ока")
            .scrollToOffer(with: "10", position: .footer)
            .exist(selector: "search_subscription")
            .scrollTo("search_subscription", maxSwipes: 1)
            .validateSnapshot(of: "search_subscription", snapshotId: "search_subscription")
    }

    func test_subscriptionButtonAfterListing() {
        mocker
            .mock_searchHistory(state: .all)
            .mock_searchCars(newCount: 0, usedCount: 1)
            .mock_searchOfferLocatorCounters(type: .all, distances: [.init(radius: 400, count: 1)])

        launch()

        mainSteps
            .openSearchHistory("LADA (ВАЗ) 1111 Ока")
            .scrollToOffer(with: "100_0", position: .footer)
            .exist(selector: "search_subscription")
            .scrollTo("search_subscription")
            .validateSnapshot(of: "search_subscription", snapshotId: "search_subscription")
    }

    func test_stockCardInfinityListingExistRelatedBlockAfter7() {
        mocker
            .mock_searchHistory(state: .new)
            .mock_searchCars(newCount: 2, usedCount: 0)
            .mock_searchOfferLocatorCounters(type: .all)

        launch()

        mainSteps
            .openSearchHistory("LADA (ВАЗ) 1111 Ока")
            .openStockCardOffer(offersTitle: "12 предложений")
            .checkInfinityListingExist()
            .scrollToOffer(with: "500_0")
            .exist(selector: "related")
    }

    func test_stockCardRelatedBlockAfterIL() {
        mocker
            .mock_searchHistory(state: .new)
            .mock_searchCars(newCount: 2, usedCount: 0, distances: [.init(radius: 400, count: 1)])
            .mock_searchOfferLocatorCounters(type: .all, distances: [.init(radius: 400, count: 1)])

        launch()

        mainSteps
            .openSearchHistory("LADA (ВАЗ) 1111 Ока")
            .openStockCardOffer(offersTitle: "12 предложений")
            .scroll(to: "related")
            .exist(selector: "offer_400_0")
    }

    func test_stockCardStartILLabel() {
        mocker
            .mock_searchHistory(state: .new)
            .mock_searchCars(newCount: 2, usedCount: 0, distances: [.init(radius: 400, count: 1)])
            .mock_searchOfferLocatorCounters(type: .all, distances: [.init(radius: 400, count: 1)])

        launch()

        mainSteps
            .openSearchHistory("LADA (ВАЗ) 1111 Ока")
            .openStockCardOffer(offersTitle: "12 предложений")
            .scrollStartIL()
            .validateSnapshot(of: "startInfinityListingTitle", snapshotId: "startInfinityListingTitle_1_gray")
    }

    func test_hasEnterOnDealerCardFromNormalOffer() {
        mocker
            .mock_searchHistory(state: .all)
            .mock_searchCars(newCount: 0, usedCount: 1, isSalon: true, distances: [.init(radius: 400, count: 1)])
            .mock_searchOfferLocatorCounters(type: .all, distances: [.init(radius: 400, count: 1)])
            .mock_salon()
            .mock_offerCars(id: "0", isSalon: true)

        launch()

        mainSteps
            .openSearchHistory("LADA (ВАЗ) 1111 Ока")
            .tap("gallery_contact_cell")
            .exist(selector: "dealerOffersCount")
    }

    func test_noEnterOnDealerCardFromILOffer() {
        mocker
            .mock_searchHistory(state: .all)
            .mock_searchCars(newCount: 0, usedCount: 1, isSalon: true, distances: [.init(radius: 400, count: 1)])
            .mock_searchOfferLocatorCounters(type: .all, distances: [.init(radius: 400, count: 1)])
            .mock_salon()
            .mock_offerCars(id: "400_0", isSalon: true)

        launch()

        mainSteps
            .openSearchHistory("LADA (ВАЗ) 1111 Ока")
            .scrollTo("gallery_contact_cell")
            .tap("gallery_contact_cell")
            .notExist(selector: "dealerOffersCount")
    }

    func test_noILInDealerListing() {
        enum Const {
            static var offerId = "1099385564-a470b464"
        }

        mocker
            .mock_searchHistory(state: .all)
            .mock_searchCars(newCount: 0, usedCount: 1)
            .mock_searchOfferLocatorCounters(type: .all, distances: [.init(radius: 400, count: 1)])
            .mock_salon()
            .mock_salonListing()

        let requestExpectationLoadIL: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/search/cars/offer-locator-counters?page_size=1" {
                return true
            }
            return false
        }
        requestExpectationLoadIL.isInverted = true

        launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/\(Const.offerId)")))
            .scrollTo("DealerInfoLayout")
            .dealerNameTap()
            .dealerOfferCountTap()

        self.wait(for: [requestExpectationLoadIL], timeout: 5)
    }

    func test_notILIntComListing() {
        mocker
            .mock_searchHistory(isCar: false)
            .mock_searchCars(newCount: 0, usedCount: 1)
            .mock_searchOfferLocatorCounters(type: .all, distances: [.init(radius: 400, count: 1)])

        launch()

        let requestExpectationLoadIL: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/search/cars/offer-locator-counters?page_size=1" {
                return true
            }
            return false
        }
        requestExpectationLoadIL.isInverted = true

        mainSteps
            .openSearchHistory("Chevrolet 3000-Series")
            .notExist(selector: "search_subscription")

        self.wait(for: [requestExpectationLoadIL], timeout: 5)
    }

    func test_notILIntMotoListing() {
        mocker
            .mock_searchHistory(isCar: false)
            .mock_searchCars(newCount: 0, usedCount: 1)
            .mock_searchOfferLocatorCounters(type: .all, distances: [.init(radius: 400, count: 1)])

        launch()

        let requestExpectationLoadIL: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/search/cars/offer-locator-counters?page_size=1" {
                return true
            }
            return false
        }
        requestExpectationLoadIL.isInverted = true

        mainSteps
            .openSearchHistory("ABM Pegas 200")
            .notExist(selector: "search_subscription")

        self.wait(for: [requestExpectationLoadIL], timeout: 5)
    }

    func test_checkCorrectRequest_0() {
        mocker
            .mock_searchHistory(state: .all)
            .mock_searchCars(newCount: 0, usedCount: 0)
            .mock_searchOfferLocatorCounters(type: .all, distances: [
                .init(radius: 100, count: 0),
                .init(radius: 200, count: 0),
                .init(radius: 300, count: 3),
                .init(radius: 400, count: 4),
                .init(radius: 500, count: 5),
                .init(radius: 600, count: 5),
                .init(radius: 700, count: 7),
                .init(radius: 800, count: 8),
                .init(radius: 900, count: 9),
                .init(radius: 1000, count: 10),
                .init(radius: 1100, count: 11)
            ])

        launch()

        let requestExpectationLoadIL: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/search/cars/offer-locator-counters?page_size=1" {
                return true
            }
            return false
        }
        let requestFirstRadius: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc" {
                let req = try! Auto_Api_Search_SearchRequestParameters(jsonUTF8Data: request.messageBody!)
                if req.excludeRid == [10702], req.geoRadius == 300 {
                    return true
                }
            }
            return false
        }
        let requestSecondRadius: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc" {
                let req = try! Auto_Api_Search_SearchRequestParameters(jsonUTF8Data: request.messageBody!)
                if req.excludeGeoRadius == 300, req.geoRadius == 400 {
                    return true
                }
            }
            return false
        }
        let notRequestIfSameCount: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc" {
                let req = try! Auto_Api_Search_SearchRequestParameters(jsonUTF8Data: request.messageBody!)
                if req.geoRadius == 600 {
                    return true
                }
            }
            return false
        }
        notRequestIfSameCount.isInverted = true
        mainSteps
            .openSearchHistory("LADA (ВАЗ) 1111 Ока")

        self.wait(for: [
            requestExpectationLoadIL,
            requestFirstRadius,
            requestSecondRadius,
            notRequestIfSameCount
        ],
        timeout: 5)
    }

    func test_checkCorrectRequest_200() {
        mocker
            .mock_searchHistory(geoRadius: 200)
            .mock_searchCars(newCount: 0, usedCount: 0)
            .mock_searchOfferLocatorCounters(type: .all, distances: [
                .init(radius: 100, count: 0),
                .init(radius: 200, count: 0),
                .init(radius: 300, count: 3),
                .init(radius: 400, count: 4),
                .init(radius: 500, count: 5),
                .init(radius: 600, count: 5),
                .init(radius: 700, count: 7),
                .init(radius: 800, count: 8),
                .init(radius: 900, count: 9),
                .init(radius: 1000, count: 10),
                .init(radius: 1100, count: 10)
            ])

        launch()

        let requestExpectationLoadIL: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/search/cars/offer-locator-counters?page_size=1" {
                return true
            }
            return false
        }
        let requestFirstRadius: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc" {
                let req = try! Auto_Api_Search_SearchRequestParameters(jsonUTF8Data: request.messageBody!)
                if req.excludeRid.isEmpty, req.excludeGeoRadius == 200, req.geoRadius == 300 {
                    return true
                }
            }
            return false
        }
        let requestSecondRadius: XCTestExpectation = expectationForRequest { request -> Bool in
            if request.method == "POST", request.uri == "/search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc" {
                let req = try! Auto_Api_Search_SearchRequestParameters(jsonUTF8Data: request.messageBody!)
                if req.excludeGeoRadius == 300, req.geoRadius == 400 {
                    return true
                }
            }
            return false
        }

        mainSteps
            .openSearchHistory("LADA (ВАЗ) 1111 Ока")

        self.wait(for: [
            requestExpectationLoadIL,
            requestFirstRadius,
            requestSecondRadius
        ],
        timeout: 5)
    }

    private func mockListingBase() {
        mocker
            .mock_base()
            .mock_user()
            .mock_searchCarsSpecials()
            .mock_searchCarsContextRecommendNewInStock()
            .mock_searchMoto()
            .mock_searchTrucks()
            .mock_searchCarsRelated()
            .mock_referenceCatalogCarConfigurationsSubtree()
            .mock_referenceCatalogCarsAllOptions()
            .mock_reviewsAutoListing()
            .mock_videoSearchCars()
    }
}
