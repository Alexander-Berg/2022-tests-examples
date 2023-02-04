//
//  DealerCabinetPanoramaFromAppTests.swift
//  UITests
//
//  Created by Dmitry Sinev on 11/17/20.
//

import XCTest
import Snapshots
import AutoRuProtoModels
import SwiftProtobuf

/// @depends_on AutoRuDealerCabinet
final class DealerCabinetPanoramaFromAppTests: DealerBaseTest {

    override func setUp() {
        app.launchArguments.append("dealerCabinetPanoramasNotEmpty")
        super.setUp()
    }

    // MARK: - Tests

    func test_PanoramasNormalUser() {
        launch()
        self.openListingAndWaitForLoading()
            .tapOnPanoramaNavBarButton()
            .wait(for: 1)
            .panoramaOptionsTap()
            .wait(for: 2)

        let snapshotId = SnapshotIdentifier(suite: SnapshotIdentifier.suiteName(from: #file), identifier: #function)
        let fullScreenshot = XCUIScreen.main.screenshot().image

        Snapshot.compareWithSnapshot(image: fullScreenshot, identifier: snapshotId)
    }

    @discardableResult
    private func openListingAndWaitForLoading() -> DealerCabinetSteps {
        return self.mainSteps
            .openDealerCabinetTab()
            .waitForLoading()
    }

    // MARK: - Setup

    override func setupServer() {
        super.setupServer()

        self.server.addHandler("GET /user?with_auth_types=true") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_profile_all_grants_including_panoramas", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_non_empty_default", userAuthorized: true)
        }
    }
}
