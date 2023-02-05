import Foundation
import XCTest

@testable import YxSwissKnife

public final class YxGradientViewTests: XCTest {
    func testGradientView() {
        let configuration = YxScreenshotTestConfiguration(
            recordMode: false,
            useDrawHierarchyInRect: true,
            referenceDir: referenceDirPath
        )
        let view = YxGradientView(
            frame: CGRect(x: 0, y: 0, width: 200, height: 48),
            gradient: YxGradient(items: [
                (0, UIColor(hex: "0xFF6F33")!),
                (1, UIColor(hex: "0xED515F")!)
                ])
        )
        do {
            try view.check(
                conf: configuration,
                identifier: configuration.makeDefaultIdentifier(pack: "\(type(of: self))"),
                tolerance: 0
            )
        } catch {
            XCTFail("Comparison failed, but shouldn't")
        }
    }
}

private let referenceDirKey = "SNAPSHOT_TEST_REF_DIR"
private let referenceDirPath = ProcessInfo.processInfo.environment[referenceDirKey]!
