//
//  Created by Alexey Aleshkov on 25.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

// swiftlint:disable large_tuple

import XCTest
@testable import ColorDifferenceKit

final class LABCGFloatColorConverterTests: XCTestCase {
    typealias RGBColorRep = (r: CGFloat, g: CGFloat, b: CGFloat)
    typealias LABColorRep = (l: CGFloat, a: CGFloat, b: CGFloat)

    // swiftlint:disable:next force_unwrapping
    let cgConverter = CGLABColorConverter(tristimulus: CIEIlluminant.d65_CIE1931().scaled.tuple)!

    func round(_ value: CGFloat) -> CGFloat {
        return ScalarMath.rounded(value, upTo: 4, pow: pow)
    }

    func round(_ value: (CGFloat, CGFloat, CGFloat)) -> (CGFloat, CGFloat, CGFloat) {
        return (
            round(value.0),
            round(value.1),
            round(value.2)
        )
    }

    func convert(_ source: RGBColorRep) -> LABColorRep {
        // swiftlint:disable:next force_unwrapping
        return self.cgConverter.SRGBToLAB(source)!
    }

    func convert(_ source: LABColorRep) -> RGBColorRep {
        // swiftlint:disable:next force_unwrapping
        return self.cgConverter.LABToSRGB(source)!
    }

    func testColors1() {
        let source = RGBColorRep(r: 0, g: 0, b: 0)
        let target = LABColorRep(l: 0, a: -0.5, b: -0.5)

        let value0 = convert(source)
        XCTAssertEqual(target, value0)

        let value1 = convert(value0)
        XCTAssertEqual(round(value1), round(source))
    }

    func testColors2() {
        let source = RGBColorRep(r: 0.5, g: 0.5, b: 0.5)
        let target = LABColorRep(l: 53.3882, a: -0.4888, b: -0.5103)

        let value0 = convert(source)
        XCTAssertEqual(round(target), round(value0))

        let value1 = convert(value0)
        XCTAssertEqual(round(value1), round(source))
    }

    func testColors3() {
        let source = RGBColorRep(r: 1, g: 0.5, b: 0.5)
        let target = LABColorRep(l: 68.6716, a: 48.9854, b: 23.4196)

        let value0 = convert(source)
        XCTAssertEqual(round(target), round(value0))

        let value1 = convert(value0)
        XCTAssertEqual(round(value1), round(source))
    }
}
