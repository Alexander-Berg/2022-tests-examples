import Foundation
import XCTest
import YxSwissKnife

final class BundleExtensionTests: XCTestCase {

    func testDisplayNameOfYxSwissKnife() {
        let expectedBundleDisplayName = "YxSwissKnife"

        let bundle = Bundle.main
        XCTAssert(bundle.displayName == expectedBundleDisplayName)
    }
}
