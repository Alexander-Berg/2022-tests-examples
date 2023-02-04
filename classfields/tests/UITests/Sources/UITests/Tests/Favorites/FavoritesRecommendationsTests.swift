//
//  FavoritesRecommendationsTests.swift
//  UITests
//
//  Created by Dmitry Sinev on 10/14/20.
//

import XCTest
import Snapshots
import AutoRuProtoModels

final class FavoritesRecommendationsTests: FavoritesTests {
    let suiteName = SnapshotIdentifier.suiteName(from: #file)

    override var launchEnvironment: [String: String] {
        var value = super.launchEnvironment
        let userDefaults: [String: Any] = [
            "hideRecommendationsInFavoritesDate": 0.0
        ]
        let userDefaultsJsonData = try! JSONSerialization.data(withJSONObject: userDefaults, options: [])
        value["STANDARD_USER_DEFAULTS"] = userDefaultsJsonData.base64EncodedString()
        return value
    }

    override var appSettings: [String: Any] {
        var updated = super.appSettings
        updated["enableFavoritesRecommendations"] = true
        return updated
    }

    // MARK: - Setup

    func test_hasOfferRecommendations() {
        currentFavoritesStub = "favs_offers_with_recommendations"
        launch()

        openFavorites()
            .scrollToSpecials()
            .wait(for: 2)

        validateSnapshots(suiteName: suiteName, accessibilityId: "SpecialsCellView", snapshotId: #function)
    }

    func test_hasOfferRecommendationsOne() {
        currentFavoritesStub = "favs_one_offer_with_recommendations"
        launch()

        openFavorites()
            .scrollToSpecials()
            .wait(for: 2)

        validateSnapshots(suiteName: suiteName, accessibilityId: "favoritesTableList", snapshotId: #function)
    }

    func test_hasOfferRecommendationsScroll() {
        currentFavoritesStub = "favs_offers_with_recommendations"
        launch()

        openFavorites()
            .scrollToSpecials()
            .scrollRecommendations()
            .wait(for: 2)

        validateSnapshots(suiteName: suiteName, accessibilityId: "SpecialsCellView", snapshotId: #function)
    }

    func test_hasOfferRecommendationsTap() {
        currentFavoritesStub = "favs_offers_with_recommendations"
        launch()

        openFavorites()
            .scrollToSpecials()
            .tapRecommendations()
            .wait(for: 1)
            .scrollDown()
            .wait(for: 1)
            .checkTitleLabelHasText("1 750 000")
    }

    func test_hasOfferRecommendationsVeil() {
        currentFavoritesStub = ""

        let favoritesWithoutRecomendedRequest = expectationForRequest { (request) -> Bool in
            if request.uri.starts(with: "/user/favorites/all"), !request.uri.contains("with_recommended=true") {
                return true
            }
            return false
        }
        server.addHandler("GET /user/favorites/all *") { (_, _) -> Response? in
            let stub = "favs_offers_with_recommendations"
            return Response.okResponse(fileName: stub)
        }
        launch()

        let steps = openFavorites()
            .scrollToSpecials()
            .tapHideRecommendations()
            .wait(for: 1)

        server.addHandler("GET /user/favorites/all *") { (_, _) -> Response? in
            let stub = "favs_offers_with_updates"
            return Response.okResponse(fileName: stub)
        }
        steps
            .tapHideRecommendationsInternal()
            .wait(for: 1)
            .checkHasNotOfferRecommendations()
        wait(for: [favoritesWithoutRecomendedRequest], timeout: 10)
    }
}
