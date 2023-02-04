//
//  Created by Alexey Aleshkov on 19.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

// swiftlint:disable discouraged_object_literal

import XCTest
@testable import ImageDifferenceKit

final class PerceptualColorDifferenceTests: XCTestCase {
    lazy var algorithmFactory = ColorDifferenceAlgorithmFactory(srgbToLabConverter: SRGBToLabColorConverterFactory.makeCoreGraphics())

    lazy var srgbDifference = algorithmFactory.makeSRGB()
    lazy var cie1976Difference = algorithmFactory.makeCIE1976()
    lazy var cie1994Difference = algorithmFactory.makeCIE1994()
    lazy var cie2000Difference = algorithmFactory.makeCIE2000()
    lazy var cmcDifference = algorithmFactory.makeCMC()

    func testSameColors() {
        let sourceColor = #colorLiteral(red: 0.1176470588, green: 0.3411764706, blue: 0.03529411765, alpha: 1) // (red: 30, green: 87, blue: 9)
        let source = sourceColor.toRGB8()
        let targetColor = #colorLiteral(red: 0.1215686275, green: 0.3450980392, blue: 0.03921568627, alpha: 1) // (red: 31, green: 88, blue: 10)
        let target = targetColor.toRGB8()

        XCTAssertTrue(srgbDifference.similar(source, target))
        XCTAssertTrue(cie1976Difference.similar(source, target))
        XCTAssertTrue(cie1994Difference.similar(source, target))
        XCTAssertTrue(cie2000Difference.similar(source, target))
        XCTAssertTrue(cmcDifference.similar(source, target))
    }

    func testImperceptibleColors() {
        let sourceColor = #colorLiteral(red: 1, green: 0, blue: 0, alpha: 1) // (red: 255, green: 0, blue: 0)
        let source = sourceColor.toRGB8()
        let targetColor = #colorLiteral(red: 0.9843137255, green: 0, blue: 0, alpha: 1) // (red: 251, green: 0, blue: 0)
        let target = targetColor.toRGB8()

        XCTAssertTrue(srgbDifference.similar(source, target))
        XCTAssertTrue(cie1976Difference.similar(source, target))
        XCTAssertTrue(cie1994Difference.similar(source, target))
        XCTAssertTrue(cie2000Difference.similar(source, target))
        XCTAssertTrue(cmcDifference.similar(source, target))
    }

    func testAlmostNoticableColors() {
        let sourceColor = #colorLiteral(red: 1, green: 0.09803921569, blue: 0.537254902, alpha: 1) // (red: 255, green: 25, blue: 137)
        let source = sourceColor.toRGB8()
        let targetColor = #colorLiteral(red: 1, green: 0.09803921569, blue: 0.5137254902, alpha: 1) // (red: 255, green: 25, blue: 131)
        let target = targetColor.toRGB8()

        XCTAssertFalse(srgbDifference.similar(source, target))

        XCTAssertTrue(cie1976Difference.similar(source, target))
        XCTAssertTrue(cie1994Difference.similar(source, target))
        XCTAssertTrue(cie2000Difference.similar(source, target))
        XCTAssertTrue(cmcDifference.similar(source, target))
    }

    func testClearlyNoticableColors() {
        let sourceColor = #colorLiteral(red: 0.1215686275, green: 0.5725490196, blue: 1, alpha: 1) // (red: 31, green: 146, blue: 255)
        let source = sourceColor.toRGB8()
        let targetColor = #colorLiteral(red: 0.1215686275, green: 0.5490196078, blue: 1, alpha: 1) // (red: 31, green: 140, blue: 255)
        let target = targetColor.toRGB8()

        XCTAssertFalse(srgbDifference.similar(source, target))
        XCTAssertFalse(cie1976Difference.similar(source, target))
        XCTAssertFalse(cie1994Difference.similar(source, target))
        XCTAssertFalse(cie2000Difference.similar(source, target))
        XCTAssertFalse(cmcDifference.similar(source, target))
    }

    func testDifferentColors() {
        let sourceColor = #colorLiteral(red: 0.5725490196, green: 0.5725490196, blue: 0.1215686275, alpha: 1) // (red: 146, green: 146, blue: 31)
        let source = sourceColor.toRGB8()
        let targetColor = #colorLiteral(red: 0.5137254902, green: 0.5137254902, blue: 0.1215686275, alpha: 1) // (red: 131, green: 131, blue: 31)
        let target = targetColor.toRGB8()

        XCTAssertFalse(srgbDifference.similar(source, target))
        XCTAssertFalse(cie1976Difference.similar(source, target))
        XCTAssertFalse(cie1994Difference.similar(source, target))
        XCTAssertFalse(cie2000Difference.similar(source, target))
        XCTAssertFalse(cmcDifference.similar(source, target))
    }
}
