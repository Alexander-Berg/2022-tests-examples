//
//  Created by Alexey Aleshkov on 03/10/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRENumberTextFormatter

// swiftlint:disable:next type_name
class NumberTextInputFormatterWithSuffixReplace3SymbolsTests: XCTestCase {
    let formatter = NumberTextInputFormatter(
        style: .init(groupingSeparator: ",", decimalSeparator: ".", positiveSuffix: "$")
    )

    // |12,|345.67$  ->  9|,345.67$
    func test1() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 0, length: 3),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "9,345.67$", carriagePosition: 1, unformattedText: "9345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1|2,3|45.67$  ->  1,9|45.67$
    func test2() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 1, length: 3),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "1,945.67$", carriagePosition: 3, unformattedText: "1945.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12|,34|5.67$  ->  1,29|5.67$
    func test3() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 2, length: 3),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "1,295.67$", carriagePosition: 4, unformattedText: "1295.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,|345|.67$  ->  129|.67$
    func test4() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 3, length: 3),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "129.67$", carriagePosition: 3, unformattedText: "129.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,3|45.|67$  ->  123,9|67$
    func test5() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 4, length: 3),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "123,967$", carriagePosition: 5, unformattedText: "123967")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,34|5.6|7$  ->  123,49|7$
    func test6() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 5, length: 3),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "123,497$", carriagePosition: 6, unformattedText: "123497")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345|.67|$  ->  123,459|$
    func test7() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 6, length: 3),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "123,459$", carriagePosition: 7, unformattedText: "123459")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345.|67$|  ->  12,345.9|$
    func test8() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 7, length: 3),
            replacementText: "9"
        )
        let expectedResult = TextInputFormatterResult(text: "12,345.9$", carriagePosition: 8, unformattedText: "12345.9")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
}
