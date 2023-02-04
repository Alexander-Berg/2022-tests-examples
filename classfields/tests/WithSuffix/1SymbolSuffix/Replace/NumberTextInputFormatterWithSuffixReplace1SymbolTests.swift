//
//  Created by Alexey Aleshkov on 03/10/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRENumberTextFormatter

// swiftlint:disable:next type_name
class NumberTextInputFormatterWithSuffixReplace1SymbolTests: XCTestCase {
    let formatter = NumberTextInputFormatter(
        style: .init(groupingSeparator: ",", decimalSeparator: ".", positiveSuffix: "$", maximumFractionDigits: 2)
    )

    // |1|2,345.67$  ->  9|2,345.67$
    func test1() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 0, length: 1),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "92,345.67$", carriagePosition: 1, unformattedText: "92345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1|2|,345.67$  ->  19|,345.67$
    func test2() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 1, length: 1),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "19,345.67$", carriagePosition: 2, unformattedText: "19345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12|,|345.67$  ->  129|,345.67$
    func test3() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 2, length: 1),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "129,345.67$", carriagePosition: 3, unformattedText: "129345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,|3|45.67$  ->  12,9|45.67$
    func test4() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 3, length: 1),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "12,945.67$", carriagePosition: 4, unformattedText: "12945.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,3|4|5.67$  ->  12,39|5.67$
    func test5() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 4, length: 1),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "12,395.67$", carriagePosition: 5, unformattedText: "12395.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,34|5|.67$  ->  12,349|.67$
    func test6() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 5, length: 1),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "12,349.67$", carriagePosition: 6, unformattedText: "12349.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345|.|67$  ->  12,345,9|67$
    func test7() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 6, length: 1),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "12,345,967$", carriagePosition: 8, unformattedText: "12345967")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345.|6|7$  ->  12,345.9|7$
    func test8() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 7, length: 1),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "12,345.97$", carriagePosition: 8, unformattedText: "12345.97")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345.6|7|$  ->  12,345.69|$
    func test9() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 8, length: 1),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "12,345.69$", carriagePosition: 9, unformattedText: "12345.69")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345.67|$|  ->  12,345.67$|
    func test10() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 9, length: 1),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "12,345.67$", carriagePosition: 9, unformattedText: "12345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
}
