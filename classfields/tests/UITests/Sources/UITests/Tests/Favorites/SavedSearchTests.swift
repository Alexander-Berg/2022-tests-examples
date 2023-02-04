//
//  SavedSearchTests.swift
//  UITests
//
//  Created by Aleksey Gotyanov on 10/15/20.
//

import XCTest
import Snapshots

class SavedSearchTests: BaseTest {

    private lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_verifiedDealerSubtitle() throws {
        api.salon.byDealerId.dealerId("20136708").get.ok(mock: .model() {
            $0.salon.loyaltyProgram = true
            $0.salon.isOficial = true
            $0.salon.dealerID = "20136708"
        })
        server.addHandler("GET /user/favorites/all/subscriptions") { request, _ in
            Response.okResponse(fileName: "dealer_subscriptions_no_parameters")
        }

        launch()
        openSearches()
            .wait(for: 1)
            .checkVerifiedDealerSubtitle()
    }

    func test_thatTapOnDealerSubscriptionOpensDealerCard() {
        advancedMockReproducer.setup(server: self.server, mockFolderName: "Favorites/Searches")

        launch()
        openSearches()
            .tapSavedSearch(id: "96107edc87f843388eecf43695324747bea0350f", index: 0)
            .wait(for: 2)
            .exist(selector: "DealerCardViewController")
            .validateSnapshot(of: "DealerCardViewController")
    }

    private func setupServer() {
        server.forceLoginMode = .forceLoggedIn
        try! server.start()
    }

    @discardableResult
    private func openSearches() -> FavoritesSteps {
        return mainSteps
            .openFavoritesTab()
            .waitForLoading()
            .tapSegment(at: .searches)
    }

}
