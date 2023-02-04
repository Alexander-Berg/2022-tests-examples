//
//  SaleCardFavoritesTest.swift
//  UITests
//
//  Created by Pavel Savchenkov on 10.05.2021.
//

import Foundation
import XCTest
import Snapshots

/// @depends_on AutoRuSaleCard
class SaleCardFavoritesTest: BaseTest {
    lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        setupServer()
    }

    // MARK: - Helpers
    private func setupServer() {
        mocker
            .mock_base()
            .setForceLoginMode(.forceLoggedIn)
            .mock_offerFromHistoryLastAll()
            .mock_addOfferToFavorite("1098252972-99d8c274")
            .mock_deleteOfferFromFavorite("1098252972-99d8c274")
            .startMock()
    }

    // MARK: - Tests

    func testShoudAddToFavoritesFromCard() {
        let requestAddFavoriteWasCalled = expectationForRequest(method: "POST", uri: "/user/favorites/cars/1098252972-99d8c274")
        let requestDeleteFavoriteWasCalled = expectationForRequest(method: "DELETE", uri: "/user/favorites/cars/1098252972-99d8c274")

        let offerCardSteps = launch(on: .saleCardScreen, options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098252972-99d8c274")))
            .likeTap()
            .checkIsFavoriteButtonSelected()

        self.wait(for: [requestAddFavoriteWasCalled], timeout: 5)

        offerCardSteps.likeTap()
            .checkIsFavoriteButtonNotSelected()

        self.wait(for: [requestDeleteFavoriteWasCalled], timeout: 5)
    }

    func test_shouldFavoriteButtonForFirstPosition() {
        var experiments = BackendState.Experiments()
        experiments.add(exp: BackendState.Experiments.FavoriteButtonFirst())
        api.device.hello.post.ok(mock: experiments.toMockSource())

        launch(
            on: .saleCardScreen,
            options: .init(launchType: .deeplink("https://auto.ru/cars/used/sale/1098252972-99d8c274"))
        )
        .focus(on: .actionButtons) { $0
            .validateSnapshot(snapshotId: "favorite_button_first_position")
        }
    }
}
