//
//  Created by Timur Turaev on 20.09.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

public final class NumericsTest: XCTestCase {
    func testIncreasingByWithoutOverflow() throws {
        XCTAssertEqual(UInt8(0).increaseWithoutOverflowBy(Int8(0)), 0)
        XCTAssertEqual(UInt8(0).increaseWithoutOverflowBy(Int8(1)), 1)
        XCTAssertEqual(UInt8(0).increaseWithoutOverflowBy(Int8.min), 0)
        XCTAssertEqual(UInt8(0).increaseWithoutOverflowBy(Int8.max), 127)
        XCTAssertEqual(UInt8(250).increaseWithoutOverflowBy(Int8.max), 255)
        XCTAssertEqual(UInt8.max.increaseWithoutOverflowBy(Int8.max), 255)
        XCTAssertEqual(UInt8.max.increaseWithoutOverflowBy(Int8.min), 255 - 128)

        // OK, doesn't compile
        // XCTAssertEqual(UInt8.max.increaseWithoutOverflowBy(Int16.min), 0)
        // XCTAssertEqual(UInt16.max.increaseWithoutOverflowBy(Int8.min), 0)

        XCTAssertEqual(UInt(100).increaseWithoutOverflowBy(-58), 42)
        XCTAssertEqual(UInt(100).increaseWithoutOverflowBy(0), 100)
        XCTAssertEqual(UInt.max.increaseWithoutOverflowBy(Int.min), UInt(Int.max))
        XCTAssertEqual(UInt.min.increaseWithoutOverflowBy(Int.min), 0)
        XCTAssertEqual(UInt.max.increaseWithoutOverflowBy(Int.max), UInt.max)
        XCTAssertEqual(UInt.min.increaseWithoutOverflowBy(Int.max), UInt(Int.max))
    }
}
