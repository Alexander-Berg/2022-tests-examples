//
//  POITests.swift
//  UITests
//
//  Created by Dmitry Sinev on 6/8/21.
//

import AutoRuProtoModels
import SwiftProtobuf
import XCTest
import Snapshots

final class POITests: BaseTest {
    let suiteName = SnapshotIdentifier.suiteName(from: #file)

    lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .startMock()
    }

    func test_POIBigBanner() {
        Step("Большое промо о POI в списке объявлений") {}
        mocker
            .mock_base()
            .mock_user()
            .mock_userOffersAllExternalPanoramaNoPOI()
            .mock_userOfferFromAllExternalPanoramaNoPOI()
            .mock_fullPanoramaFailToLoad()
            .mock_userOfferFromAllExternalPanoramaNoPOIStats()
            .mock_userOfferFromAllExternalPanoramaNoPOIDraft()
            .mock_referenceCatalogCarsAllOptions()
            .mock_userOfferFromAllExternalPanoramaNoPOIDraftSuggest()

        launch()

        openUserSaleList()
            .wait(for: 5)
            .tapTryBigPOIPromo()
            .checkPanoramaLoadError()
            .closeGallery()
            .editOffer()
            .tapPOILink()
            .checkPanoramaPlayerCloseButton()
    }

    private func openUserSaleList() -> OffersSteps {
        let steps = self.mainSteps
            .openTab(.offers)
            .as(OffersSteps.self)

        return steps
    }
}
