//
//  Created by Alexey Aleshkov on 24.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

// swiftlint:disable number_separator

import XCTest
@testable import ColorDifferenceKit
import simd

final class LinearSRGBColorConverterTests: XCTestCase {
    typealias RGBColorRep = (r: Double, g: Double, b: Double)

    let scalarConverter = LinearSRGBColorConverter<Double, Double>(pow: pow)

    func scalarLinearConvert(_ source: RGBColorRep) -> RGBColorRep {
        return self.scalarConverter.SRGBToLinearSRGB(source)
    }

    func scalarConvert(_ source: RGBColorRep) -> RGBColorRep {
        return self.scalarConverter.LinearSRGBToSRGB(source)
    }

    func testColors1() {
        let source = RGBColorRep(r: 0.5, g: 0, b: 0)
        let target = RGBColorRep(r: 0.21404114048223255, g: 0, b: 0)

        let scalarLinearValue = scalarLinearConvert(source)
        XCTAssertEqual(scalarLinearValue, target)

        let scalarValue = scalarConvert(scalarLinearValue)
        XCTAssertEqual(scalarValue, source)
    }
}
