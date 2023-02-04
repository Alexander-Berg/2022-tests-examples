//
//  Created by Alexey Aleshkov on 25.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

// swiftlint:disable large_tuple
// swiftlint:disable number_separator

import XCTest
@testable import ColorDifferenceKit
import simd

final class LABFloatColorConverterTests: XCTestCase {
    typealias XYZColorRep = (x: Float, y: Float, z: Float)
    typealias LABColorRep = (l: Float, a: Float, b: Float)

    let scalarConverter = LABColorConverter<Float>(tristimulus: CIEIlluminant.d65_CIE1931().scaled.tuple, pow: pow)

    func round(_ value: Float) -> Float {
        return ScalarMath.rounded(value, upTo: 4, pow: powf)
    }

    func round(_ value: (Float, Float, Float)) -> (Float, Float, Float) {
        return (
            round(value.0),
            round(value.1),
            round(value.2)
        )
    }

    func scalarConvert(_ source: XYZColorRep) -> LABColorRep {
        return self.scalarConverter.XYZToLAB(source)
    }

    func scalarConvert(_ source: LABColorRep) -> XYZColorRep {
        return self.scalarConverter.LABToXYZ(source)
    }

    func testColors1() {
        let source = XYZColorRep(x: 41.241085, y: 21.264935, z: 1.9331759)
        let target = LABColorRep(l: 53.238235, a: 80.08957, b: 67.20071)

        let value00 = scalarConvert(source)
        XCTAssertEqual(value00, target)

        let value10 = scalarConvert(value00)
        XCTAssertEqual(round(value10), round(source))
    }
}
