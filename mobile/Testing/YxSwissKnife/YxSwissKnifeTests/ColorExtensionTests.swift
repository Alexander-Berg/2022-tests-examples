import Foundation
import XCTest
@testable import YxSwissKnife

class ColorExtensionTests: XCTestCase {

    func testCreatingUIColorFromHexString() {
        guard let uiColor = UIColor(hex: "#FF9911") else {
            XCTFail("Can't parse proper color from # notation")
            return
        }

        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        var alpha: CGFloat = 0

        uiColor.getRed(&red, green: &green, blue: &blue, alpha: &alpha)

        XCTAssert(Int(red * 255) == 255)
        XCTAssert(Int(green * 255) == 153)
        XCTAssert(Int(blue * 255) == 17)
    }

    func testCreatingUiColorFromRgbComponents() {
        let uiColor = UIColor(red: 255, green: 153, blue: 11)

        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        var alpha: CGFloat = 0

        uiColor.getRed(&red, green: &green, blue: &blue, alpha: &alpha)

        XCTAssert(Int(red * 255) == 255)
        XCTAssert(Int(green * 255) == 153)
        XCTAssert(Int(blue * 255) == 11)
        XCTAssert(Int(alpha) == 1)
    }

    func testSettingAlphaComponentViaWithOpacity() {
        let uiColor = UIColor(red: 255, green: 153, blue: 11)
        let uiColorWithOpacity = uiColor.with(alpha: 13)

        var red: CGFloat = 0
        var green: CGFloat = 0
        var blue: CGFloat = 0
        var alpha: CGFloat = 0

        uiColorWithOpacity.getRed(&red, green: &green, blue: &blue, alpha: &alpha)

        XCTAssert(Int(red * 255) == 255)
        XCTAssert(Int(green * 255) == 153)
        XCTAssert(Int(blue * 255) == 11)
        XCTAssert(alpha.isEqual(to: 0.13))
    }
}
