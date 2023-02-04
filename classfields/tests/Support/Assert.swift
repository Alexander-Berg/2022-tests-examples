//
//  Created by Alexey Aleshkov on 03.02.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

// swiftlint:disable large_tuple

import XCTest
import CoreGraphics

func XCTAssertEqual(
    _ value0: (CGFloat, CGFloat, CGFloat),
    _ value1: (CGFloat, CGFloat, CGFloat),
    file: StaticString = #filePath,
    line: UInt = #line
) {
    XCTAssert(value0.0 == value1.0 && value0.1 == value1.1 && value0.2 == value1.2, "\(value0) is not equal to \(value1)", file: file, line: line)
}

func XCTAssertEqual(
    _ value0: (Float, Float, Float),
    _ value1: (Float, Float, Float),
    file: StaticString = #filePath,
    line: UInt = #line
) {
    XCTAssert(value0.0 == value1.0 && value0.1 == value1.1 && value0.2 == value1.2, "\(value0) is not equal to \(value1)", file: file, line: line)
}

func XCTAssertEqual(
    _ value0: (Double, Double, Double),
    _ value1: (Double, Double, Double),
    file: StaticString = #filePath,
    line: UInt = #line
) {
    XCTAssert(value0.0 == value1.0 && value0.1 == value1.1 && value0.2 == value1.2, "\(value0) is not equal to \(value1)", file: file, line: line)
}
