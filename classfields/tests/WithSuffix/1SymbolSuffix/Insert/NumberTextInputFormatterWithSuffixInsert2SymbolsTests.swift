//
//  Created by Alexey Aleshkov on 03/10/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRENumberTextFormatter

// swiftlint:disable:next type_name
class NumberTextInputFormatterWithSuffixInsert2SymbolsTests: XCTestCase {
    let formatter = NumberTextInputFormatter(
        style: .init(groupingSeparator: ",", decimalSeparator: ".", positiveSuffix: "$", maximumFractionDigits: 2)
    )

    // |12,345.67$  ->  8,9|12,345.67$
    func test1() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 0, length: 0),
            replacementText: "89"
        )
        let expectedResult = TextInputFormatterResult(text: "8,912,345.67$", carriagePosition: 3, unformattedText: "8912345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1|2,345.67$  ->  1,89|2,345.67$
    func test2() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 1, length: 0),
            replacementText: "89"
        )
        let expectedResult = TextInputFormatterResult(text: "1,892,345.67$", carriagePosition: 4, unformattedText: "1892345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12|,345.67$  ->  1,289|,345.67$
    func test3() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 2, length: 0),
            replacementText: "89"
        )
        let expectedResult = TextInputFormatterResult(text: "1,289,345.67$", carriagePosition: 5, unformattedText: "1289345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,|345.67$  ->  1,289|,345.67$
    func test4() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 3, length: 0),
            replacementText: "89"
        )
        let expectedResult = TextInputFormatterResult(text: "1,289,345.67$", carriagePosition: 5, unformattedText: "1289345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,3|45.67$  ->  1,238,9|45.67$
    func test5() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 4, length: 0),
            replacementText: "89"
        )
        let expectedResult = TextInputFormatterResult(text: "1,238,945.67$", carriagePosition: 7, unformattedText: "1238945.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,34|5.67$  ->  1,234,89|5.67$
    func test6() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 5, length: 0),
            replacementText: "89"
        )
        let expectedResult = TextInputFormatterResult(text: "1,234,895.67$", carriagePosition: 8, unformattedText: "1234895.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345|.67$  ->  1,234,589|.67$
    func test7() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 6, length: 0),
            replacementText: "89"
        )
        let expectedResult = TextInputFormatterResult(text: "1,234,589.67$", carriagePosition: 9, unformattedText: "1234589.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345.|67$  ->  12,345.89|$
    func test8() { // to_fix
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 7, length: 0),
            replacementText: "89"
        )
        let expectedResult = TextInputFormatterResult(text: "12,345.89$", carriagePosition: 9, unformattedText: "12345.89")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345.6|7$  ->  12,345.68|$
    func test9() { // to_fix
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 8, length: 0),
            replacementText: "89"
        )
        let expectedResult = TextInputFormatterResult(text: "12,345.68$", carriagePosition: 9, unformattedText: "12345.68")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345.67|$  ->  12,345.67|$
    func test10() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 9, length: 0),
            replacementText: "89"
        )
        let expectedResult = TextInputFormatterResult(text: "12,345.67$", carriagePosition: 9, unformattedText: "12345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345.67$|  ->  12,345.67|$
    func test11() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 10, length: 0),
            replacementText: "89"
        )
        let expectedResult = TextInputFormatterResult(text: "12,345.67$", carriagePosition: 9, unformattedText: "12345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
}
