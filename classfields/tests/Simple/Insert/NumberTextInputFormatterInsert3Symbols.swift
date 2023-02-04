//
//  Created by Alexey Aleshkov on 03/10/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRENumberTextFormatter

class NumberTextInputFormatterInsert3Symbols: XCTestCase {
    let formatter = NumberTextInputFormatter(
        style: .init(groupingSeparator: ",", decimalSeparator: ".")
    )

    // |12,345.67  ->  80,9|12,345.67
    func test1() {
        let actualResult = self.formatter.format(
            text: "12,345.67",
            range: NSRange(location: 0, length: 0),
            replacementText: "809"
        )
        let expectedResult = TextInputFormatterResult(text: "80,912,345.67", carriagePosition: 4, unformattedText: "80912345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1|2,345.67  ->  18,09|2,345.67
    func test2() {
        let actualResult = self.formatter.format(
            text: "12,345.67",
            range: NSRange(location: 1, length: 0),
            replacementText: "809"
        )
        let expectedResult = TextInputFormatterResult(text: "18,092,345.67", carriagePosition: 5, unformattedText: "18092345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12|,345.67  ->  12,809|,345.67
    func test3() {
        let actualResult = self.formatter.format(
            text: "12,345.67",
            range: NSRange(location: 2, length: 0),
            replacementText: "809"
        )
        let expectedResult = TextInputFormatterResult(text: "12,809,345.67", carriagePosition: 6, unformattedText: "12809345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,|345.67  ->  12,809|,345.67
    func test4() {
        let actualResult = self.formatter.format(
            text: "12,345.67",
            range: NSRange(location: 3, length: 0),
            replacementText: "809"
        )
        let expectedResult = TextInputFormatterResult(text: "12,809,345.67", carriagePosition: 6, unformattedText: "12809345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,3|45.67  ->  12,380,9|45.67
    func test5() {
        let actualResult = self.formatter.format(
            text: "12,345.67",
            range: NSRange(location: 4, length: 0),
            replacementText: "809"
        )
        let expectedResult = TextInputFormatterResult(text: "12,380,945.67", carriagePosition: 8, unformattedText: "12380945.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,34|5.67  ->  12,348,09|5.67
    func test6() {
        let actualResult = self.formatter.format(
            text: "12,345.67",
            range: NSRange(location: 5, length: 0),
            replacementText: "809"
        )
        let expectedResult = TextInputFormatterResult(text: "12,348,095.67", carriagePosition: 9, unformattedText: "12348095.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345|.67  ->  12,345,809|.67
    func test7() {
        let actualResult = self.formatter.format(
            text: "12,345.67",
            range: NSRange(location: 6, length: 0),
            replacementText: "809"
        )
        let expectedResult = TextInputFormatterResult(text: "12,345,809.67", carriagePosition: 10, unformattedText: "12345809.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345.|67  ->  12,345.80|
    func test8() {
        let actualResult = self.formatter.format(
            text: "12,345.67",
            range: NSRange(location: 7, length: 0),
            replacementText: "809"
        )
        let expectedResult = TextInputFormatterResult(text: "12,345.80967", carriagePosition: 10, unformattedText: "12345.80967")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345.6|7  ->  12,345.68|
    func test9() {
        let actualResult = self.formatter.format(
            text: "12,345.67",
            range: NSRange(location: 8, length: 0),
            replacementText: "809"
        )
        let expectedResult = TextInputFormatterResult(text: "12,345.68097", carriagePosition: 11, unformattedText: "12345.68097")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345.67|  ->  12,345.67|
    func test10() {
        let actualResult = self.formatter.format(
            text: "12,345.67",
            range: NSRange(location: 9, length: 0),
            replacementText: "809"
        )
        let expectedResult = TextInputFormatterResult(text: "12,345.67809", carriagePosition: 12, unformattedText: "12345.67809")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
}
