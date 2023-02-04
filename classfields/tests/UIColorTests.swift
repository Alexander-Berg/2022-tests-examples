import UIKit
import XCTest
@testable import YRECoreUI

final class UIColorTests: XCTestCase {
    func testOverlappingOpaqueColorWithOpaqueColor() {
        let bottom = UIColor.yre_displayP3Color(red: 1, green: 0, blue: 0, alpha: 1)
        let top = UIColor.yre_displayP3Color(red: 0, green: 0, blue: 1, alpha: 1)

        let actual = bottom.overlaid(with: top).resolvedColor(with: .current)
        let expected = top

        XCTAssertEqual(actual, expected)
    }

    func testOverlappingOpaqueColorWithTransparentColor() {
        let bottom = UIColor.red
        let top = UIColor.blue.withAlphaComponent(0.5)

        let actual = bottom.overlaid(with: top).resolvedColor(with: .current)
        let expected = UIColor.yre_displayP3Color(red: 0.5, green: 0, blue: 0.5, alpha: 1)

        XCTAssertEqual(actual, expected)
    }

    func testOverlappingTransparentColorWithTransparentColor() {
        let bottom = UIColor.red.withAlphaComponent(0.5)
        let top = UIColor.blue.withAlphaComponent(0.5)

        let actual = bottom.overlaid(with: top).resolvedColor(with: .current)
        let expected = UIColor.yre_displayP3Color(red: 1.0 / 3, green: 0, blue: 2.0 / 3, alpha: 0.75)

        XCTAssertEqual(actual, expected)
    }

    func testHexConversion() {
        let hexString = "35117C"
        let color1 = UIColor.yre_color(hexString: hexString)
        let color2 = UIColor(rgb: 0x35117C)
        let hex1 = color1?.hexString
        let hex2 = color2.hexString

        XCTAssertEqual(hexString, hex1)
        XCTAssertEqual(hexString, hex2)
        XCTAssertEqual(color1, color2)
    }
}
