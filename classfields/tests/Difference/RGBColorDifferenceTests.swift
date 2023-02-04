//
//  Created by Alexey Aleshkov on 19.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

// swiftlint:disable number_separator

import XCTest
@testable import ColorDifferenceKit

final class RGBColorDifferenceTests: XCTestCase {
    typealias RGBColorRep = (red: UInt8, green: UInt8, blue: UInt8)

    lazy var weightedDiff = WeightedEuclideanSRGBColorDifference<CGFloat, CGFloat>(div: /, pow: pow, sqrt: sqrt)

    func weightedDifference(_ source: RGBColorRep, _ target: RGBColorRep) -> CGFloat {
        return self.weightedDiff.difference(
            r1: CGFloat(source.red),
            g1: CGFloat(source.green),
            b1: CGFloat(source.blue),
            r2: CGFloat(target.red),
            g2: CGFloat(target.green),
            b2: CGFloat(target.blue)
        )
    }

    func testSameColors() {
        let source = RGBColorRep(red: 127, green: 127, blue: 127)
        let target = RGBColorRep(red: 127, green: 127, blue: 127)

        let diff: CGFloat = 0
        let value = weightedDifference(source, target)
        XCTAssertEqual(value, diff)
    }

    func testAlmostSameColors1() {
        let source = RGBColorRep(red: 0, green: 0, blue: 0)
        let target = RGBColorRep(red: 1, green: 0, blue: 0)

        let diff: CGFloat = 1.4142135623730951
        let value = weightedDifference(source, target)
        XCTAssertEqual(value, diff)
    }

    func testAlmostSameColors2() {
        let source = RGBColorRep(red: 0, green: 0, blue: 0)
        let target = RGBColorRep(red: 0, green: 1, blue: 0)

        let diff: CGFloat = 2.0
        let value = weightedDifference(source, target)
        XCTAssertEqual(value, diff)
    }

    func testAlmostSameColors3() {
        let source = RGBColorRep(red: 0, green: 0, blue: 0)
        let target = RGBColorRep(red: 0, green: 0, blue: 1)

        let diff: CGFloat = 1.7320508075688772
        let value = weightedDifference(source, target)
        XCTAssertEqual(value, diff)
    }

    func testAlmostSameGrayColors() {
        let source = RGBColorRep(red: 127, green: 127, blue: 127)
        let target = RGBColorRep(red: 127, green: 128, blue: 127)

        let diff: CGFloat = 2
        let value = weightedDifference(source, target)
        XCTAssertEqual(value, diff)
    }

    func testColors1() {
        // these diff values are maximum

        let source = RGBColorRep(red: 0, green: 0, blue: 0)
        let target = RGBColorRep(red: 255, green: 255, blue: 255)

        let diff: CGFloat = 765.0
        let value = weightedDifference(source, target)
        XCTAssertEqual(value, diff)
    }

    func testColors2() {
        let source = RGBColorRep(red: 0, green: 0, blue: 255)
        let target = RGBColorRep(red: 255, green: 0, blue: 0)

        let diff: CGFloat = 570.1973342624464
        let value = weightedDifference(source, target)
        XCTAssertEqual(value, diff)
    }

    func testColors3() {
        let source = RGBColorRep(red: 200, green: 105, blue: 200)
        let target = RGBColorRep(red: 225, green: 105, blue: 200)

        let diff: CGFloat = 43.30127018922193
        let value = weightedDifference(source, target)
        XCTAssertEqual(value, diff)
    }

    func testColors4() {
        let source = RGBColorRep(red: 175, green: 200, blue: 100)
        let target = RGBColorRep(red: 200, green: 200, blue: 100)

        let diff: CGFloat = 43.30127018922193
        let value = weightedDifference(source, target)
        XCTAssertEqual(value, diff)
    }

    //

    // swiftlint:disable:next line_length
    lazy var weightedIntDiff = WeightedEuclideanSRGBColorDifference<Int, UInt>(div: /, pow: IntegerMath.pow, sqrt: IntegerMath.sqrtIterative)

    func weightedIntDifference(_ source: RGBColorRep, _ target: RGBColorRep) -> Int {
        return self.weightedIntDiff.difference(
            r1: Int(source.red),
            g1: Int(source.green),
            b1: Int(source.blue),
            r2: Int(target.red),
            g2: Int(target.green),
            b2: Int(target.blue)
        )
    }

    func testSameIntColors() {
        let source = RGBColorRep(red: 127, green: 127, blue: 127)
        let target = RGBColorRep(red: 127, green: 127, blue: 127)

        let diff: Int = 0
        let value = weightedIntDifference(source, target)
        XCTAssertEqual(value, diff)
    }

    func testIntColors1() {
        // these diff values are maximum

        let source = RGBColorRep(red: 0, green: 0, blue: 0)
        let target = RGBColorRep(red: 255, green: 255, blue: 255)

        let diff: Int = 765
        let value = weightedIntDifference(source, target)
        XCTAssertEqual(value, diff)
    }

    func testIntColors4() {
        let source = RGBColorRep(red: 175, green: 200, blue: 100)
        let target = RGBColorRep(red: 200, green: 200, blue: 100)

        let diff: Int = 43
        let value = weightedIntDifference(source, target)
        XCTAssertEqual(value, diff)
    }
}
