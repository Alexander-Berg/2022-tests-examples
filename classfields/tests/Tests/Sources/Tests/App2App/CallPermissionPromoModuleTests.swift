import XCTest
import AutoRuCallsUI
import Snapshots
import CoreGraphics

class CallPermissionPromoModuleTests: XCTestCase {
    var module: CallPermissionPromoModule!

    override func setUp() {
        super.setUp()

        module = CallPermissionPromoModule()
    }

    func test_snapshot() {
        // на скрине фон прозрачный – ок, в приложении фон есть у контейнера.
        Snapshot.compareWithSnapshot(viewController: module.viewController, size: CGSize(width: 414, height: 500))
    }
}
