//
//  Created by Alexey Aleshkov on 03/10/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRENumberTextFormatter

class NumberTextInputFormatterInputTests: XCTestCase {
    let formatter = NumberTextInputFormatter(
        style: .init(groupingSeparator: ",", decimalSeparator: ".")
    )

    // "|"  ->  1|
    func test1() {
        let actualResult = self.formatter.format(
            text: "",
            range: NSRange(location: 0, length: 0),
            replacementText: "1"
        )
        let expectedResult = TextInputFormatterResult(text: "1", carriagePosition: 1, unformattedText: "1")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1|  ->  12|
    func test2() {
        let actualResult = self.formatter.format(
            text: "1",
            range: NSRange(location: 1, length: 0),
            replacementText: "2"
        )
        let expectedResult = TextInputFormatterResult(text: "12", carriagePosition: 2, unformattedText: "12")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12|  ->  123|
    func test3() {
        let actualResult = self.formatter.format(
            text: "12",
            range: NSRange(location: 2, length: 0),
            replacementText: "3"
        )
        let expectedResult = TextInputFormatterResult(text: "123", carriagePosition: 3, unformattedText: "123")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 123|  ->  1,234|
    func test4() {
        let actualResult = self.formatter.format(
            text: "123",
            range: NSRange(location: 3, length: 0),
            replacementText: "4"
        )
        let expectedResult = TextInputFormatterResult(text: "1,234", carriagePosition: 5, unformattedText: "1234")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1,234|  ->  12,345|
    func test5() {
        let actualResult = self.formatter.format(
            text: "1,234",
            range: NSRange(location: 5, length: 0),
            replacementText: "5"
        )
        let expectedResult = TextInputFormatterResult(text: "12,345", carriagePosition: 6, unformattedText: "12345")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345|  ->  123,456|
    func test6() {
        let actualResult = self.formatter.format(
            text: "12,345",
            range: NSRange(location: 6, length: 0),
            replacementText: "6"
        )
        let expectedResult = TextInputFormatterResult(text: "123,456", carriagePosition: 7, unformattedText: "123456")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 123,456|  ->  1,234,567|
    func test7() {
        let actualResult = self.formatter.format(
            text: "123,456",
            range: NSRange(location: 7, length: 0),
            replacementText: "7"
        )
        let expectedResult = TextInputFormatterResult(text: "1,234,567", carriagePosition: 9, unformattedText: "1234567")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1,234|  ->  1,234.|
    func test8() {
        let actualResult = self.formatter.format(
            text: "1,234",
            range: NSRange(location: 5, length: 0),
            replacementText: "."
        )
        let expectedResult = TextInputFormatterResult(text: "1,234.", carriagePosition: 6, unformattedText: "1234.0")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1,234.|  ->  1,234.5|
    func test9() {
        let actualResult = self.formatter.format(
            text: "1,234.",
            range: NSRange(location: 6, length: 0),
            replacementText: "5"
        )
        let expectedResult = TextInputFormatterResult(text: "1,234.5", carriagePosition: 7, unformattedText: "1234.5")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1,234.5|  ->  1,234.56|
    func test10() {
        let actualResult = self.formatter.format(
            text: "1,234.5",
            range: NSRange(location: 7, length: 0),
            replacementText: "6"
        )
        let expectedResult = TextInputFormatterResult(text: "1,234.56", carriagePosition: 8, unformattedText: "1234.56")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // "|"  ->  .|
    func test11() {
        let actualResult = self.formatter.format(
            text: "",
            range: NSRange(location: 0, length: 0),
            replacementText: "."
        )
        let expectedResult = TextInputFormatterResult(text: ".", carriagePosition: 1, unformattedText: "")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // .|  ->  .5|
    func test12() {
        let actualResult = self.formatter.format(
            text: ".",
            range: NSRange(location: 1, length: 0),
            replacementText: "5"
        )
        let expectedResult = TextInputFormatterResult(text: ".5", carriagePosition: 2, unformattedText: "0.5")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // .5|  ->  .56|
    func test13() {
        let actualResult = self.formatter.format(
            text: ".5",
            range: NSRange(location: 2, length: 0),
            replacementText: "6"
        )
        let expectedResult = TextInputFormatterResult(text: ".56", carriagePosition: 3, unformattedText: "0.56")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // |.56  ->  3|.56
    func test14() {
        let actualResult = self.formatter.format(
            text: ".56",
            range: NSRange(location: 0, length: 0),
            replacementText: "3"
        )
        let expectedResult = TextInputFormatterResult(text: "3.56", carriagePosition: 1, unformattedText: "3.56")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // |3.56  ->  2|3.56
    func test15() {
        let actualResult = self.formatter.format(
            text: "3.56",
            range: NSRange(location: 0, length: 0),
            replacementText: "2"
        )
        let expectedResult = TextInputFormatterResult(text: "23.56", carriagePosition: 1, unformattedText: "23.56")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // |23.56  ->  1|23.56
    func test16() {
        let actualResult = self.formatter.format(
            text: "23.56",
            range: NSRange(location: 0, length: 0),
            replacementText: "1"
        )
        let expectedResult = TextInputFormatterResult(text: "123.56", carriagePosition: 1, unformattedText: "123.56")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 123|.56  ->  1,234|.56
    func test17() {
        let actualResult = self.formatter.format(
            text: "123.56",
            range: NSRange(location: 3, length: 0),
            replacementText: "4"
        )
        let expectedResult = TextInputFormatterResult(text: "1,234.56", carriagePosition: 5, unformattedText: "1234.56")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1,234,567,891,234,568 -> 12,345,678,912,345,689
    func test18() {
        let actualResult = self.formatter.format(
            text: "1,234,567,891,234,568",
            range: NSRange(location: 21, length: 0),
            replacementText: "9"
        )

        let expectedResult = TextInputFormatterResult(
            text: "12,345,678,912,345,689",
            carriagePosition: 22,
            unformattedText: "12345678912345689"
        )
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 555| -> 5,555|
    func test19() {
        let actualResult = self.formatter.format(
            text: "555",
            range: NSRange(location: 3, length: 0),
            replacementText: "5"
        )
        let expectedResult = TextInputFormatterResult(
            text: "5,555",
            carriagePosition: 5,
            unformattedText: "5555"
        )
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1,234,567,891,234,567,891| -> 12,345,678,912,345,678,912|
    func test20() {
        let actualResult = self.formatter.format(
            text: "1,234,567,891,234,567,891",
            range: NSRange(location: 25, length: 0),
            replacementText: "2"
        )
        let expectedResult = TextInputFormatterResult(
            text: "12,345,678,912,345,678,912",
            carriagePosition: 26,
            unformattedText: "12345678912345678912"
        )
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
}
