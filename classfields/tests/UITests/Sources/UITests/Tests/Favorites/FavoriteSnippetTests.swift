//
//  FavoritesSnippetTests.swift
//  UITests
//
//  Created by Sergey An. Sergeev on 14.10.2020.
//

import Foundation
import Snapshots

/// @depends_on AutoRuFavoriteSaleList AutoRuCellHelpers
final class FavoriteSnippetTests: FavoritesTests {
    let suiteName = SnapshotIdentifier.suiteName(from: #file)

    func test_hasOfferCallButton() {
        currentFavoritesStub = "favs_offers_with_updates"

        launch()
        openFavorites()
            .scrollDown()
            .wait(for: 2)
            .checkHasOfferCallButton()

        validateSnapshots(suiteName: suiteName, accessibilityId: "call_button", snapshotId: #function)
    }

    func test_soldOffersDontHaveCallButton() {
        currentFavoritesStub = "favs_offers_no_updates_sold"

        launch()
        openFavorites()
            .wait(for: 2)
            .checkHasNotOfferCallButton()
    }

    func test_hasOfferShowReportButton() {
        currentFavoritesStub = "favs_offers_with_updates"

        launch()
        openFavorites()
            .wait(for: 2)
            .scrollDown()
            .checkHasShowReportButton()

        validateSnapshots(suiteName: suiteName, accessibilityId: "show_report_button", snapshotId: #function)
    }

    func test_showReport() {
        currentFavoritesStub = "favs_offers_with_updates"

        mocker.mock_reportLayoutForOffer(bought: false)

        launch()
        openFavorites()
            .showReport()
    }
}
