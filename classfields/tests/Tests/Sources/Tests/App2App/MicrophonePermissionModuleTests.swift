import XCTest
import AutoRuCallsUI
import Snapshots
import CoreGraphics

class MicrophonePermissionModuleTests: XCTestCase {
    var module: MicrophonePermissionModule!

    override func setUp() {
        super.setUp()

        module = MicrophonePermissionModule(
            message: "Так вы сможете общаться с теми, кто предпочитает звонить через Авто.ру"
        )
    }

    func test_snapshot() {
        // на скрине фон прозрачный – ок, в приложении фон есть у контейнера.
        Snapshot.compareWithSnapshot(viewController: module.viewController, size: CGSize(width: 414, height: 500))
    }
}
