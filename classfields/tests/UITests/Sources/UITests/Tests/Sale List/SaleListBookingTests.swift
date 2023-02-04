import AutoRuProtoModels
import SwiftProtobuf
import XCTest
import Snapshots

/// @depends_on AutoRuSaleList AutoRuCellHelpers
final class SaleListBookingTests: BaseTest {
    let suiteName = SnapshotIdentifier.suiteName(from: #file)

    lazy var mainSteps = MainSteps(context: self)

    override func setUp() {
        super.setUp()
        setupServer()
        launch()
    }

    // MARK: - Helpers

    private func setupServer() {
        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        server.addHandler("POST /search/cars?context=listing&page=1&page_size=20&sort=fresh_relevance_1-desc") { (_, _) -> Response? in
            Response.okResponse(fileName: "sale_list_booking")
        }

        try! server.start()
    }

    @discardableResult
    private func routeToListing(offerId: String, position: SaleCardListSteps.OfferPosition = .body) -> SaleCardListSteps {
        return mainSteps
            .wait(for: 1)
            .openFilters()
            .resetFilters()
            .showResultsTap()
            .wait(for: 1)
            .scrollToOffer(with: offerId, position: position, maxSwipes: 15)
    }

    private func screenshotForElement(acessibilityId: String) -> UIImage {
        return app.descendants(matching: .any).matching(identifier: acessibilityId).firstMatch.screenshot().image
    }

    private func validateElementSnapshots(accessibilityId: String, snapshotId: String) {
        let snapshotId = SnapshotIdentifier(suite: suiteName, identifier: snapshotId)
        let screenshot = screenshotForElement(acessibilityId: accessibilityId)

        Snapshot.compareWithSnapshot(image: screenshot, identifier: snapshotId, perPixelTolerance: 0.01)
    }
}
