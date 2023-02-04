//
//  FeedListingOnMainTests.swift
//  UITests
//
//  Created by Vitalii Stikhurov on 13.05.2022.
//

import XCTest
class FeedListingOnMainTests: BaseTest {
    let oppenedOfferId = "1115533173-16984e9b"
    let lastPage1OfferId = "1115048793-abc7bc99"
    lazy var mainSteps = MainSteps(context: self)
    var settings: [String: Any] = [:]
    
    override var appSettings: [String: Any] {
        return settings
    }
    
    override func setUp() {
        super.setUp()
        settings = super.appSettings
        setupServer()
    }

    // MARK: - Interface

    func setupServer() {
        api.personalization.getPersonalizedOffersFeed
            .get(parameters: .wildcard)
            .ok(mock: .file("getPersonalizedOffersFeed"))
        
        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }
        server.forceLoginMode = .forceLoggedIn
        
        api.offer.category(.cars)
            .offerID(oppenedOfferId)
            .get
            .ok(mock: .file("offer_CARS_1092222570-3203c7f6_ok"))
        
        try! server.start()
    }
    
    func test_feedOnMainExistAndCanBeOppened() {
        let secondPageNotExpectation = api.personalization.getPersonalizedOffersFeed
            .get(parameters: .parameters([.page(2), .pageSize(20)]))
            .notExpect()
        let uidExpectation = expectationForRequest { request in
            return request.uri.starts(with: "/personalization/get-personalized-offers-feed") && request.headers["x-mm-device-id"] != nil
        }

        launch()

        mainSteps
            .scrollTo("feed_section")
            .exist(selector: "feed_section")
            .scrollTo("HistoryOffer_\(oppenedOfferId)")
            .tap("HistoryOffer_\(oppenedOfferId)")
            .exist(selector: "sale_card")
        
        wait(for: [secondPageNotExpectation, uidExpectation], timeout: 1)
    }
    
    func test_loadMore() {
        let secondPageExpectation = api.personalization.getPersonalizedOffersFeed
            .get(parameters: .parameters([.page(2), .pageSize(20)]))
            .expect()
        
        launch()
        mainSteps
            .scrollTo("HistoryOffer_\(lastPage1OfferId)")
            .exist(selector: "HistoryOffer_\(lastPage1OfferId)")
        
        wait(for: [secondPageExpectation], timeout: 10)
    }
}
