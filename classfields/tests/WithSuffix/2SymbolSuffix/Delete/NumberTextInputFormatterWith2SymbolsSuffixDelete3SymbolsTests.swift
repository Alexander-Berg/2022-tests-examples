//
//  Created by Alexey Aleshkov on 03/10/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRENumberTextFormatter

// swiftlint:disable:next type_name
class NumberTextInputFormatterWith2SymbolsSuffixDelete3SymbolsTests: XCTestCase {
    let formatter = NumberTextInputFormatter(
        style: .init(groupingSeparator: ",", decimalSeparator: ".", positiveSuffix: " $")
    )

    // |12,|345.67 $  ->  |345.67 $
    func test1() {
        let actualResult = self.formatter.format(
            text: "12,345.67 $",
            range: NSRange(location: 0, length: 3),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "345.67 $", carriagePosition: 0, unformattedText: "345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1|2,3|45.67 $  ->  1|45.67 $
    func test2() {
        let actualResult = self.formatter.format(
            text: "12,345.67 $",
            range: NSRange(location: 1, length: 3),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "145.67 $", carriagePosition: 1, unformattedText: "145.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12|,34|5.67 $  ->  12|5.67 $
    func test3() {
        let actualResult = self.formatter.format(
            text: "12,345.67 $",
            range: NSRange(location: 2, length: 3),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "125.67 $", carriagePosition: 2, unformattedText: "125.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,|345|.67 $  ->  12|.67 $
    func test4() {
        let actualResult = self.formatter.format(
            text: "12,345.67 $",
            range: NSRange(location: 3, length: 3),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "12.67 $", carriagePosition: 2, unformattedText: "12.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,3|45.|67 $  ->  12,3|67 $
    func test5() {
        let actualResult = self.formatter.format(
            text: "12,345.67 $",
            range: NSRange(location: 4, length: 3),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "12,367 $", carriagePosition: 4, unformattedText: "12367")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,34|5.6|7 $  ->  12,34|7 $
    func test6() {
        let actualResult = self.formatter.format(
            text: "12,345.67 $",
            range: NSRange(location: 5, length: 3),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "12,347 $", carriagePosition: 5, unformattedText: "12347")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345|.67| $  ->  12,345| $
    func test7() {
        let actualResult = self.formatter.format(
            text: "12,345.67 $",
            range: NSRange(location: 6, length: 3),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "12,345 $", carriagePosition: 6, unformattedText: "12345")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345.|67 |$  ->  12,345.| $
    func test8() {
        let actualResult = self.formatter.format(
            text: "12,345.67 $",
            range: NSRange(location: 7, length: 3),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "12,345. $", carriagePosition: 7, unformattedText: "12345.0")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345.6|7 $|  ->  12,345.6| $
    func test9() {
        let actualResult = self.formatter.format(
            text: "12,345.67 $",
            range: NSRange(location: 8, length: 3),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "12,345.6 $", carriagePosition: 8, unformattedText: "12345.6")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
}
