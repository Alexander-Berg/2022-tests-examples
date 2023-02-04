//
//  FoldedRangesTests.swift
//  YRECoreUtils-Unit-Tests
//
//  Created by Dmitry Barillo on 12.10.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRECoreUtils
import enum Typograf.SpecialSymbol

final class FoldedRangesTests: XCTestCase {
    func testEmptyRange() {
        let input1: [UInt] = []

        let expectedResult = ""

        XCTAssertEqual(input1.yreJoinedByFoldedRanges(), expectedResult)
    }

    func testRange() {
        let ndash = SpecialSymbol.ndash

        let input1: [UInt] = [1, 2, 3, 4, 5]
        let input2: [UInt] = [5, 4, 3, 2, 1]
        let input3: [UInt] = [5, 1, 3, 4, 2]

        let expectedResult = "1\(ndash)5"

        XCTAssertEqual(input1.yreJoinedByFoldedRanges(), expectedResult)
        XCTAssertEqual(input2.yreJoinedByFoldedRanges(), expectedResult)
        XCTAssertEqual(input3.yreJoinedByFoldedRanges(), expectedResult)
    }

    func testSingleValue() {
        let input1: [UInt] = [1]
        let input2: [UInt] = [1, 3, 5]
        let input3: [UInt] = [1, 5, 3]

        XCTAssertEqual(input1.yreJoinedByFoldedRanges(), "1")
        XCTAssertEqual(input2.yreJoinedByFoldedRanges(), "1, 3, 5")
        XCTAssertEqual(input3.yreJoinedByFoldedRanges(), "1, 3, 5")
    }

    func testMixed() {
        let ndash = SpecialSymbol.ndash

        let input1: [UInt] = [1, 4, 5, 2, 3, 9, 8, 11, 0]
        let input2: [UInt] = [1, 2, 4, 4]
        let input3: [UInt] = [1, 5, 3, 4, 7, 11, 8]
        let input4: [UInt] = [1, 5, 3, 4, 7, 11, 8, 9, 10]

        XCTAssertEqual(input1.yreJoinedByFoldedRanges(), "0\(ndash)5, 8, 9, 11")
        XCTAssertEqual(input2.yreJoinedByFoldedRanges(), "1, 2, 4")
        XCTAssertEqual(input3.yreJoinedByFoldedRanges(), "1, 3\(ndash)5, 7, 8, 11")
        XCTAssertEqual(input4.yreJoinedByFoldedRanges(), "1, 3\(ndash)5, 7\(ndash)11")
    }
}
