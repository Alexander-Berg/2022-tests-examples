//
//  Created by Alexey Aleshkov on 03/10/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRENumberTextFormatter

class NumberTextInputFormatterWithPrefixInputTests: XCTestCase {
    let formatter = NumberTextInputFormatter(
        style: .init(groupingSeparator: ",", decimalSeparator: ".", positivePrefix: "$")
    )

    // "|"  ->  $1|
    func test1() {
        let actualResult = self.formatter.format(
            text: "",
            range: NSRange(location: 0, length: 0),
            replacementText: "1"
        )
        let expectedResult = TextInputFormatterResult(text: "$1", carriagePosition: 2, unformattedText: "1")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // $1|  ->  $12|
    func test2() {
        let actualResult = self.formatter.format(
            text: "$1",
            range: NSRange(location: 2, length: 0),
            replacementText: "2"
        )
        let expectedResult = TextInputFormatterResult(text: "$12", carriagePosition: 3, unformattedText: "12")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // $12|  ->  $123|
    func test3() {
        let actualResult = self.formatter.format(
            text: "$12",
            range: NSRange(location: 3, length: 0),
            replacementText: "3"
        )
        let expectedResult = TextInputFormatterResult(text: "$123", carriagePosition: 4, unformattedText: "123")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // $123|  ->  $1,234|
    func test4() {
        let actualResult = self.formatter.format(
            text: "$123",
            range: NSRange(location: 4, length: 0),
            replacementText: "4"
        )
        let expectedResult = TextInputFormatterResult(text: "$1,234", carriagePosition: 6, unformattedText: "1234")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // $1,234|  ->  $12,345|
    func test5() {
        let actualResult = self.formatter.format(
            text: "$1,234",
            range: NSRange(location: 6, length: 0),
            replacementText: "5"
        )
        let expectedResult = TextInputFormatterResult(text: "$12,345", carriagePosition: 7, unformattedText: "12345")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // $12,345|  ->  $12,345.|
    func test6() {
        let actualResult = self.formatter.format(
            text: "$12,345",
            range: NSRange(location: 7, length: 0),
            replacementText: "."
        )
        let expectedResult = TextInputFormatterResult(text: "$12,345.", carriagePosition: 8, unformattedText: "12345.0")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // $12,345.|  ->  $12.345.6|
    func test7() {
        let actualResult = self.formatter.format(
            text: "$12,345.",
            range: NSRange(location: 8, length: 0),
            replacementText: "6"
        )
        let expectedResult = TextInputFormatterResult(text: "$12,345.6", carriagePosition: 9, unformattedText: "12345.6")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // $12,345.6|  ->  $12,345.67|
    func test8() {
        let actualResult = self.formatter.format(
            text: "$12,345.6",
            range: NSRange(location: 9, length: 0),
            replacementText: "7"
        )
        let expectedResult = TextInputFormatterResult(text: "$12,345.67", carriagePosition: 10, unformattedText: "12345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // $1,234,567,891,234,567,891|.6 -> $12,345,678,912,345,678,912|.6
    func test9() {
        let actualResult = self.formatter.format(
            text: "$1,234,567,891,234,567,891.6",
            range: NSRange(location: 26, length: 0),
            replacementText: "2"
        )
        let expectedResult = TextInputFormatterResult(
            text: "$12,345,678,912,345,678,912.6",
            carriagePosition: 27,
            unformattedText: "12345678912345678912.6"
        )
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
}
