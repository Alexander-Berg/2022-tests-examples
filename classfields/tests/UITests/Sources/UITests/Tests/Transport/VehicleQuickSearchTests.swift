import AutoRuProtoModels
import SwiftProtobuf
import XCTest
import Snapshots

/// @depends_on AutoRu AutoRuServices AutoRuVehicleTextSearch AutoRuCellHelpers
final class VehicleQuickSearchTests: BaseTest {
    private lazy var mainSteps = MainSteps(context: self)

    // MARK: -

    override func setUp() {
        super.setUp()
        setupServer()
        launch()
    }

    // MARK: - Tests

    func test_quickSearch() {
        server.addHandler("GET /searchline/history") { (_, _) -> Response? in
            Response.okResponse(fileName: "vehicle_quick_search_various_types")
        }

        let screen = openQuickSearch().onScreen()
        Snapshot.compareWithSnapshot(
            image: screen.scrollableElement.waitAndScreenshot().image,
            ignoreEdges: UIEdgeInsets(top: 64, left: 0, bottom: 800, right: 0)
        )
    }

    // MARK: - Setup

    private func setupServer() {

        server.addHandler("POST /device/hello") { (_, _) -> Response? in
            Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }

        server.forceLoginMode = .forceLoggedIn
        try! server.start()
    }

    // MARK: - Private

    @discardableResult
    private func openQuickSearch() -> VehicleQuickSearchSteps {
        return mainSteps.openQuickSearch()
    }
}
