//
//  Created by Alexey Aleshkov on 03/10/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRENumberTextFormatter

// swiftlint:disable:next type_name
class NumberTextInputFormatterWith2SymbolsSuffixBy1SymbolErasingTests: XCTestCase {
    let formatter = NumberTextInputFormatter(
        style: .init(groupingSeparator: ",", decimalSeparator: ".", positiveSuffix: " $")
    )

    // 12,345.67 |$|  ->  12,345.67| $
    func test1() {
        let actualResult = self.formatter.format(
            text: "12,345.67 $",
            range: NSRange(location: 10, length: 1),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "12,345.67 $", carriagePosition: 9, unformattedText: "12345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345.67| |$  ->  12,345.67| $
    func test2() {
        let actualResult = self.formatter.format(
            text: "12,345.67 $",
            range: NSRange(location: 9, length: 1),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "12,345.67 $", carriagePosition: 9, unformattedText: "12345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345.6|7| $  ->  12,345.6| $
    func test3() {
        let actualResult = self.formatter.format(
            text: "12,345.67 $",
            range: NSRange(location: 8, length: 1),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "12,345.6 $", carriagePosition: 8, unformattedText: "12345.6")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345.|6| $  ->  12,345.| $
    func test4() {
        let actualResult = self.formatter.format(
            text: "12,345.6 $",
            range: NSRange(location: 7, length: 1),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "12,345. $", carriagePosition: 7, unformattedText: "12345.0")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345|.| $  ->  12,345| $
    func test5() {
        let actualResult = self.formatter.format(
            text: "12,345. $",
            range: NSRange(location: 6, length: 1),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "12,345 $", carriagePosition: 6, unformattedText: "12345")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,34|5| $  ->  1,234| $
    func test6() {
        let actualResult = self.formatter.format(
            text: "12,345 $",
            range: NSRange(location: 5, length: 1),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "1,234 $", carriagePosition: 5, unformattedText: "1234")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1,23|4| $  ->  123| $
    func test7() {
        let actualResult = self.formatter.format(
            text: "1,234 $",
            range: NSRange(location: 4, length: 1),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "123 $", carriagePosition: 3, unformattedText: "123")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12|3| $  ->  12| $
    func test8() {
        let actualResult = self.formatter.format(
            text: "123 $",
            range: NSRange(location: 2, length: 1),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "12 $", carriagePosition: 2, unformattedText: "12")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1|2| $  ->  1| $
    func test9() {
        let actualResult = self.formatter.format(
            text: "12 $",
            range: NSRange(location: 1, length: 1),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "1 $", carriagePosition: 1, unformattedText: "1")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // |1| $  ->  "| $"
    func test10() {
        let actualResult = self.formatter.format(
            text: "1 $",
            range: NSRange(location: 0, length: 1),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "", carriagePosition: 0, unformattedText: "")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
}
