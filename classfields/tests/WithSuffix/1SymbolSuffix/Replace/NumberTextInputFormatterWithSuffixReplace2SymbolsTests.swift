//
//  Created by Alexey Aleshkov on 03/10/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRENumberTextFormatter

// swiftlint:disable:next type_name
class NumberTextInputFormatterWithSuffixReplace2SymbolsTests: XCTestCase {
    let formatter = NumberTextInputFormatter(
        style: .init(groupingSeparator: ",", decimalSeparator: ".", positiveSuffix: "$")
    )

    // |12|,345.67$  ->  9|,345.67$
    func test1() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 0, length: 2),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "9,345.67$", carriagePosition: 1, unformattedText: "9345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1|2,|345.67$  ->  19|,345.67$
    func test2() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 1, length: 2),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "19,345.67$", carriagePosition: 2, unformattedText: "19345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12|,3|45.67$  ->  12,9|45.67$
    func test3() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 2, length: 2),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "12,945.67$", carriagePosition: 4, unformattedText: "12945.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,|34|5.67$  ->  1,29|5.67$
    func test4() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 3, length: 2),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "1,295.67$", carriagePosition: 4, unformattedText: "1295.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,3|45|.67$  ->  1,239|.67$
    func test5() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 4, length: 2),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "1,239.67$", carriagePosition: 5, unformattedText: "1239.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,34|5.|67$  ->  1,234,9|67$
    func test6() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 5, length: 2),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "1,234,967$", carriagePosition: 7, unformattedText: "1234967")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345|.6|7$  ->  1,234,59|7$
    func test7() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 6, length: 2),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "1,234,597$", carriagePosition: 8, unformattedText: "1234597")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345.|67|$  ->  12,345.9|$
    func test8() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 7, length: 2),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "12,345.9$", carriagePosition: 8, unformattedText: "12345.9")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345.6|7$|  ->  12,345.69|$
    func test9() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 8, length: 2),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "12,345.69$", carriagePosition: 9, unformattedText: "12345.69")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
}
