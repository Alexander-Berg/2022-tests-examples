//
//  Created by Alexey Aleshkov on 19.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

// swiftlint:disable number_separator

import XCTest
@testable import ColorDifferenceKit

final class LabColorDifferenceTests: XCTestCase {
    typealias CIELabColorRep = (l: CGFloat, a: CGFloat, b: CGFloat)

    lazy var cie1976Diff = CIE1976LabColorDifference<CGFloat, CGFloat>(pow: pow, sqrt: sqrt)

    func cie1976Difference(_ source: CIELabColorRep, _ target: CIELabColorRep) -> CGFloat {
        return self.cie1976Diff.difference(
            L1: source.l,
            a1: source.a,
            b1: source.b,
            L2: target.l,
            a2: target.a,
            b2: target.b
        )
    }

    lazy var cie1994Diff = CIE1994LabColorDifference<CGFloat, CGFloat>(kL: 1, kC: 1, kH: 1, K1: 0.045, K2: 0.015, pow: pow)

    func cie1994Difference(_ source: CIELabColorRep, _ target: CIELabColorRep) -> CGFloat {
        return self.cie1994Diff.difference(
            L1: source.l,
            a1: source.a,
            b1: source.b,
            L2: target.l,
            a2: target.a,
            b2: target.b
        )
    }

    // swiftlint:disable:next line_length
    lazy var cie2000Diff = CIE2000LabColorDifference<CGFloat, CGFloat>(kL: 1, kC: 1, kH: 1, pow: pow, atan2: atan2, sin: sin, cos: cos, exp: exp, fmod: fmod)

    func cie2000Difference(_ source: CIELabColorRep, _ target: CIELabColorRep) -> CGFloat {
        return self.cie2000Diff.difference(
            L1: source.l,
            a1: source.a,
            b1: source.b,
            L2: target.l,
            a2: target.a,
            b2: target.b
        )
    }

    lazy var cieCMCDiff_1_1 = CMCLabColorDifference<CGFloat, CGFloat>(l: 1, c: 1, pow: pow, atan2: atan2, cos: cos)

    func cieCMCDifference_1_1(_ source: CIELabColorRep, _ target: CIELabColorRep) -> CGFloat {
        return self.cieCMCDiff_1_1.difference(
            L1: source.l,
            a1: source.a,
            b1: source.b,
            L2: target.l,
            a2: target.a,
            b2: target.b
        )
    }

    lazy var cieCMCDiff_2_1 = CMCLabColorDifference<CGFloat, CGFloat>(l: 2, c: 1, pow: pow, atan2: atan2, cos: cos)

    func cieCMCDifference_2_1(_ source: CIELabColorRep, _ target: CIELabColorRep) -> CGFloat {
        return self.cieCMCDiff_2_1.difference(
            L1: source.l,
            a1: source.a,
            b1: source.b,
            L2: target.l,
            a2: target.a,
            b2: target.b
        )
    }

    func testSameColors() {
        let source = CIELabColorRep(l: 50, a: 0, b: 0)
        let target = CIELabColorRep(l: 50, a: 0, b: 0)

        let diff76: CGFloat = 0
        let value76 = cie1976Difference(source, target)
        XCTAssertEqual(value76, diff76)

        let diff94: CGFloat = 0
        let value94 = cie1994Difference(source, target)
        XCTAssertEqual(value94, diff94)

        let diff2000: CGFloat = 0
        let value2000 = cie2000Difference(source, target)
        XCTAssertEqual(value2000, diff2000)

        let diffCMC: CGFloat = 0
        let valueCMC = cieCMCDifference_1_1(source, target)
        XCTAssertEqual(valueCMC, diffCMC)
    }

    func testColors1() {
        // these diff values are maximum

        let source = CIELabColorRep(l: 0, a: -128, b: -128)
        let target = CIELabColorRep(l: 100, a: 127, b: 127)

        let diff76: CGFloat = 374.2325480232846
        let value76 = cie1976Difference(source, target)
        XCTAssertEqual(value76, diff76)

        let diff94: CGFloat = 139.36098591992206
        let value94 = cie1994Difference(source, target)
        XCTAssertEqual(value94, diff94)

        let diff2000: CGFloat = 126.92072411714965
        let value2000 = cie2000Difference(source, target)
        XCTAssertEqual(value2000, diff2000)

        let diffCMC_1_1: CGFloat = 227.93247927799558
        let valueCMC_1_1 = cieCMCDifference_1_1(source, target)
        XCTAssertEqual(valueCMC_1_1, diffCMC_1_1)

        let diffCMC_2_1: CGFloat = 152.41685810907546
        let valueCMC_2_1 = cieCMCDifference_2_1(source, target)
        XCTAssertEqual(valueCMC_2_1, diffCMC_2_1)
    }

    func testColors2() {
        let source = CIELabColorRep(l: 0, a: 0, b: 0)
        let target = CIELabColorRep(l: 100, a: 0, b: 0)

        let diff76: CGFloat = 100
        let value76 = cie1976Difference(source, target)
        XCTAssertEqual(value76, diff76)

        let diff94: CGFloat = 100
        let value94 = cie1994Difference(source, target)
        XCTAssertEqual(value94, diff94)

        let diff2000: CGFloat = 100
        let value2000 = cie2000Difference(source, target)
        XCTAssertEqual(value2000, diff2000)

        let diffCMC_1_1: CGFloat = 195.69471624266146
        let valueCMC_1_1 = cieCMCDifference_1_1(source, target)
        XCTAssertEqual(valueCMC_1_1, diffCMC_1_1)

        let diffCMC_2_1: CGFloat = 97.84735812133073
        let valueCMC_2_1 = cieCMCDifference_2_1(source, target)
        XCTAssertEqual(valueCMC_2_1, diffCMC_2_1)
    }

    func testColors3() {
        let source = CIELabColorRep(l: 50, a: -128, b: 0)
        let target = CIELabColorRep(l: 50, a: 127, b: 0)

        let diff76: CGFloat = 255
        let value76 = cie1976Difference(source, target)
        XCTAssertEqual(value76, diff76)

        let diff94: CGFloat = 87.32822091093138
        let value94 = cie1994Difference(source, target)
        XCTAssertEqual(value94, diff94)

        let diff2000: CGFloat = 116.90438389514031
        let value2000 = cie2000Difference(source, target)
        XCTAssertEqual(value2000, diff2000)

        let diffCMC_1_1: CGFloat = 95.81414767653992
        let valueCMC_1_1 = cieCMCDifference_1_1(source, target)
        XCTAssertEqual(valueCMC_1_1, diffCMC_1_1)

        let diffCMC_2_1: CGFloat = 95.81414767653992
        let valueCMC_2_1 = cieCMCDifference_2_1(source, target)
        XCTAssertEqual(valueCMC_2_1, diffCMC_2_1)
    }

    func testColors4() {
        let source = CIELabColorRep(l: 50, a: 0, b: -128)
        let target = CIELabColorRep(l: 50, a: 0, b: 127)

        let diff76: CGFloat = 255
        let value76 = cie1976Difference(source, target)
        XCTAssertEqual(value76, diff76)

        let diff94: CGFloat = 87.32822091093138
        let value94 = cie1994Difference(source, target)
        XCTAssertEqual(value94, diff94)

        let diff2000: CGFloat = 88.82585348467067
        let value2000 = cie2000Difference(source, target)
        XCTAssertEqual(value2000, diff2000)

        let diffCMC_1_1: CGFloat = 105.05122046232101
        let valueCMC_1_1 = cieCMCDifference_1_1(source, target)
        XCTAssertEqual(valueCMC_1_1, diffCMC_1_1)

        let diffCMC_2_1: CGFloat = 105.05122046232101
        let valueCMC_2_1 = cieCMCDifference_2_1(source, target)
        XCTAssertEqual(valueCMC_2_1, diffCMC_2_1)
    }
}
