//
//  Created by Alexey Aleshkov on 03/10/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRENumberTextFormatter

// swiftlint:disable:next type_name
class NumberTextInputFormatterWithSuffixReplaceWith0Tests: XCTestCase {
    let formatter = NumberTextInputFormatter(
        style: .init(groupingSeparator: ",", decimalSeparator: ".", positiveSuffix: "$")
    )

    // |1|2,345.67$  ->  |2,345.67$
    func test1() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 0, length: 1),
            replacementText: "0"
        )
        let expectedResult = TextInputFormatterResult(text: "2,345.67$", carriagePosition: 0, unformattedText: "2345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1|2|,345.67$  ->  10|,345.67$
    func test2() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 1, length: 1),
            replacementText: "0"
        )
        let expectedResult = TextInputFormatterResult(text: "10,345.67$", carriagePosition: 2, unformattedText: "10345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // "|" ->  0|$
    func test3() {
        let actualResult = self.formatter.format(
            text: "",
            range: NSRange(location: 0, length: 0),
            replacementText: "0"
        )
        let expectedResult = TextInputFormatterResult(text: "0$", carriagePosition: 1, unformattedText: "0")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 0|$  ->  0.|$
    func test4() {
        let actualResult = self.formatter.format(
            text: "0$",
            range: NSRange(location: 1, length: 0),
            replacementText: "."
        )
        let expectedResult = TextInputFormatterResult(text: "0.$", carriagePosition: 2, unformattedText: "0.0")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 0.|$  ->  0.0|$
    func test5() {
        let actualResult = self.formatter.format(
            text: "0.$",
            range: NSRange(location: 2, length: 0),
            replacementText: "0"
        )
        let expectedResult = TextInputFormatterResult(text: "0.0$", carriagePosition: 3, unformattedText: "0.0")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 0.0|$  ->  0.00|$
    func test6() {
        let actualResult = self.formatter.format(
            text: "0.0$",
            range: NSRange(location: 3, length: 0),
            replacementText: "0"
        )
        let expectedResult = TextInputFormatterResult(text: "0.00$", carriagePosition: 4, unformattedText: "0.0")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // |0.0$  ->  |0.0$
    func test7() {
        let actualResult = self.formatter.format(
            text: "0.0$",
            range: NSRange(location: 0, length: 0),
            replacementText: "0"
        )
        let expectedResult = TextInputFormatterResult(text: "0.0$", carriagePosition: 0, unformattedText: "0.0")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 0|.0$  ->  1|.0$
    func test8() {
        let actualResult = self.formatter.format(
            text: "0.0$",
            range: NSRange(location: 1, length: 0),
            replacementText: "1"
        )
        let expectedResult = TextInputFormatterResult(text: "1.0$", carriagePosition: 1, unformattedText: "1.0")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12|,|345.67$  ->  120|,345.67$
    func test9() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 2, length: 1),
            replacementText: "0"
        )
        let expectedResult = TextInputFormatterResult(text: "120,345.67$", carriagePosition: 3, unformattedText: "120345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345|.|67$  ->  12,345,0|67$
    func test10() {
        let actualResult = self.formatter.format(
            text: "12,345.67$",
            range: NSRange(location: 6, length: 1),
            replacementText: "0"
        )
        let expectedResult = TextInputFormatterResult(text: "12,345,067$", carriagePosition: 8, unformattedText: "12345067")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
}
