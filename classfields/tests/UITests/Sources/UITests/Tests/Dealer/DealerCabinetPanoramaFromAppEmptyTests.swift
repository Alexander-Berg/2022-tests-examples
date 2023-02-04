//
//  File.swift
//  UITests
//
//  Created by Alexander Malnev on 2/3/21.
//

import XCTest
import Snapshots

/// @depends_on AutoRuDealerCabinet
final class DealerCabinetPanoramaFromAppEmptyTests: DealerBaseTest {

    // MARK: - Tests

    func test_PanoramasPhotographFromEmptyState() {
        launch()
        self.openListingAndWaitForLoading()
            .tapOnPanoramaPhotographButton()
            .wait(for: 1)
            .addPanoramaCellTap()
            .wait(for: 1)
            .addPanoramaVinTap()
            .addPanoramaVinType()
            .wait(for: 1)
            .addPanoramaVinButtonExist()
    }

    @discardableResult
    private func openListingAndWaitForLoading() -> DealerCabinetSteps {
        return self.mainSteps
            .openDealerCabinetTab(isAttentions: false)
            .waitForLoading()
    }

    // MARK: - Setup

    override func setupServer() {
        super.setupServer()

        self.server.addHandler("GET /user?with_auth_types=true") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_profile_photograph_grants", userAuthorized: true)
        }

        self.server.addHandler("GET /user/offers/all *") { (_, _) -> Response? in
            Response.okResponse(fileName: "dealer_offers_empty", userAuthorized: true)
        }
    }
}
