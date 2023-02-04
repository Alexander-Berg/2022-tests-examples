//
//  Created by Alexey Aleshkov on 24.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

// swiftlint:disable number_separator

import XCTest
@testable import ColorDifferenceKit
import simd

final class XYZFloatColorConverterTests: XCTestCase {
    typealias XYZColorRep = (x: Float, y: Float, z: Float)
    typealias RGBColorRep = (r: Float, g: Float, b: Float)

    let scalarConverter = LinearSRGBColorConverter<Float, Float>(pow: pow)

    func scalarLinearConvert(_ source: RGBColorRep) -> RGBColorRep {
        return self.scalarConverter.SRGBToLinearSRGB(source)
    }

    func scalarConvert(_ source: RGBColorRep) -> RGBColorRep {
        return self.scalarConverter.LinearSRGBToSRGB(source)
    }

    func convert(_ source: RGBColorRep) -> XYZColorRep {
        let linear = self.scalarLinearConvert(source)
        let result = XYZFloatColorConverter.linearSRGBToXYZ(linear)
        return result
    }

    func convert(_ source: XYZColorRep) -> RGBColorRep {
        let linear = XYZFloatColorConverter.XYZToLinearSRGB(source)
        let result = self.scalarConvert(linear)
        return result
    }

    func testColors1() {
        let source = RGBColorRep(r: 1.0, g: 0, b: 0)
        let target = XYZColorRep(x: 41.241085, y: 21.264935, z: 1.9331759)

        let value0 = convert(source)
        XCTAssertEqual(value0, target)

        let round: (Float) -> Float = { value in
            return ScalarMath.rounded(value, upTo: 6, pow: powf)
        }
        let value1 = convert(value0)
        let roundedValue1 = RGBColorRep(r: round(value1.r), g: round(value1.g), b: round(value1.b))
        XCTAssertEqual(roundedValue1, source)
    }
}
