//
//  Created by Alexey Aleshkov on 03/10/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRENumberTextFormatter

class NumberTextInputFormatterWithSuffixInputTests: XCTestCase {
    let formatter = NumberTextInputFormatter(
        style: .init(groupingSeparator: ",", decimalSeparator: ".", positiveSuffix: "$", maximumFractionDigits: 2)
    )

    // "|"  ->  1|$
    func testIto1I$() {
        let actualResult = self.formatter.format(
            text: "",
            range: NSRange(location: 0, length: 0),
            replacementText: "1"
        )
        let expectedResult = TextInputFormatterResult(text: "1$", carriagePosition: 1, unformattedText: "1")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1|$  ->  1.|$
    func test1I$to1pI$() {
        let actualResult = self.formatter.format(
            text: "1$",
            range: NSRange(location: 1, length: 0),
            replacementText: "."
        )
        let expectedResult = TextInputFormatterResult(text: "1.$", carriagePosition: 2, unformattedText: "1.0")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1.|$  ->  1.5|$
    func test1pI$to1p5I$() {
        let actualResult = self.formatter.format(
            text: "1.$",
            range: NSRange(location: 2, length: 0),
            replacementText: "5"
        )
        let expectedResult = TextInputFormatterResult(text: "1.5$", carriagePosition: 3, unformattedText: "1.5")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1.5|$  ->  1.56|$
    func test1p5I$to1p56I$() {
        let actualResult = self.formatter.format(
            text: "1.5$",
            range: NSRange(location: 3, length: 0),
            replacementText: "6"
        )
        let expectedResult = TextInputFormatterResult(text: "1.56$", carriagePosition: 4, unformattedText: "1.56")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1|.56$  ->  12|.56$
    func test1Ip56$to12Ip56$() {
        let actualResult = self.formatter.format(
            text: "1.56$",
            range: NSRange(location: 1, length: 0),
            replacementText: "2"
        )
        let expectedResult = TextInputFormatterResult(text: "12.56$", carriagePosition: 2, unformattedText: "12.56")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12|.56$  ->  123|.56$
    func test12Ip56$to123Ip56$() {
        let actualResult = self.formatter.format(
            text: "12.56$",
            range: NSRange(location: 2, length: 0),
            replacementText: "3"
        )
        let expectedResult = TextInputFormatterResult(text: "123.56$", carriagePosition: 3, unformattedText: "123.56")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 123|.56$  ->  1,234|.56$
    func test123Ip56$to1c234Ip56$() {
        let actualResult = self.formatter.format(
            text: "123.56$",
            range: NSRange(location: 3, length: 0),
            replacementText: "4"
        )
        let expectedResult = TextInputFormatterResult(text: "1,234.56$", carriagePosition: 5, unformattedText: "1234.56")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // "|"  ->  .|$
    func testItopI$() {
        let actualResult = self.formatter.format(
            text: "",
            range: NSRange(location: 0, length: 0),
            replacementText: "."
        )
        let expectedResult = TextInputFormatterResult(text: ".$", carriagePosition: 1, unformattedText: "")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // .|$  ->  .5|$
    func testpI$toP5I$() {
        let actualResult = self.formatter.format(
            text: ".$",
            range: NSRange(location: 1, length: 0),
            replacementText: "5"
        )
        let expectedResult = TextInputFormatterResult(text: ".5$", carriagePosition: 2, unformattedText: "0.5")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // .5|$  ->  .56|$
    func testP5I$toP56I$() {
        let actualResult = self.formatter.format(
            text: ".5$",
            range: NSRange(location: 2, length: 0),
            replacementText: "6"
        )
        let expectedResult = TextInputFormatterResult(text: ".56$", carriagePosition: 3, unformattedText: "0.56")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // .|56$  ->  .4|5$
    func testPI56$toP4I5$() {
        let actualResult = self.formatter.format(
            text: ".56$",
            range: NSRange(location: 1, length: 0),
            replacementText: "4"
        )
        let expectedResult = TextInputFormatterResult(text: ".45$", carriagePosition: 2, unformattedText: "0.45")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // .5|6$  ->  .50|$
    func testP5I6$ToP50I$() {
        let actualResult = self.formatter.format(
            text: ".56$",
            range: NSRange(location: 2, length: 0),
            replacementText: "0"
        )
        let expectedResult = TextInputFormatterResult(text: ".50$", carriagePosition: 3, unformattedText: "0.5")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // .56|$  ->  .56|$
    func testP56I$toP56I$() {
        let actualResult = self.formatter.format(
            text: ".56$",
            range: NSRange(location: 3, length: 0),
            replacementText: "7"
        )
        let expectedResult = TextInputFormatterResult(text: ".56$", carriagePosition: 3, unformattedText: "0.56")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // |.5$  ->  1|.5$
    func testIP5$to1Ip5$() {
        let actualResult = self.formatter.format(
            text: ".5$",
            range: NSRange(location: 0, length: 0),
            replacementText: "1"
        )
        let expectedResult = TextInputFormatterResult(text: "1.5$", carriagePosition: 1, unformattedText: "1.5")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1,234,567,891,234,567,891|.6$ -> $ 12,345,678,912,345,678,912|.6$
    func test1() {
        let actualResult = self.formatter.format(
            text: "1,234,567,891,234,567,891.6$",
            range: NSRange(location: 25, length: 0),
            replacementText: "2"
        )
        let expectedResult = TextInputFormatterResult(
            text: "12,345,678,912,345,678,912.6$",
            carriagePosition: 26,
            unformattedText: "12345678912345678912.6"
        )
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
}
