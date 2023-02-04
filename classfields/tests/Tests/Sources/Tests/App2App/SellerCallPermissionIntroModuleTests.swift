import XCTest
import AutoRuCallsUI
import Snapshots
import CoreGraphics

class SellerCallPermissionIntroModuleTests: XCTestCase {
    var module: SellerCallPermissionIntroModule!

    override func setUp() {
        super.setUp()

        module = SellerCallPermissionIntroModule()
    }

    func test_snapshot() {
        // на скрине фон прозрачный – ок, в приложении фон есть у контейнера.
        Snapshot.compareWithSnapshot(viewController: module.viewController, size: CGSize(width: 414, height: 500))
    }
}
