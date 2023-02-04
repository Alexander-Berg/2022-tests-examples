//
//  Created by Alexey Aleshkov on 03/10/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRENumberTextFormatter

class NumberTextInputFormatterReplace3SymbolsTests: XCTestCase {
    let formatter = NumberTextInputFormatter(
        style: .init(groupingSeparator: ",", decimalSeparator: ".")
    )

    // |12,|345.67  ->  809|,345.67
    func test1() {
        let actualResult = self.formatter.format(
            text: "12,345.67",
            range: NSRange(location: 0, length: 3),
            replacementText: "809"
        )
        let expectedResult = TextInputFormatterResult(text: "809,345.67", carriagePosition: 3, unformattedText: "809345.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1|2,3|45.67  ->  180,9|45.67
    func test2() {
        let actualResult = self.formatter.format(
            text: "12,345.67",
            range: NSRange(location: 1, length: 3),
            replacementText: "809"
        )
        let expectedResult = TextInputFormatterResult(text: "180,945.67", carriagePosition: 5, unformattedText: "180945.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12|,34|5.67  ->  128,09|5.67
    func test3() {
        let actualResult = self.formatter.format(
            text: "12,345.67",
            range: NSRange(location: 2, length: 3),
            replacementText: "809"
        )
        let expectedResult = TextInputFormatterResult(text: "128,095.67", carriagePosition: 6, unformattedText: "128095.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,|345|.67  ->  12,809|.67
    func test4() {
        let actualResult = self.formatter.format(
            text: "12,345.67",
            range: NSRange(location: 3, length: 3),
            replacementText: "809"
        )
        let expectedResult = TextInputFormatterResult(text: "12,809.67", carriagePosition: 6, unformattedText: "12809.67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,3|45.|67  ->  12,380,9|67
    func test5() {
        let actualResult = self.formatter.format(
            text: "12,345.67",
            range: NSRange(location: 4, length: 3),
            replacementText: "809"
        )
        let expectedResult = TextInputFormatterResult(text: "12,380,967", carriagePosition: 8, unformattedText: "12380967")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,34|5.6|7  ->  12,348,09|7
    func test6() {
        let actualResult = self.formatter.format(
            text: "12,345.67",
            range: NSRange(location: 5, length: 3),
            replacementText: "809"
        )
        let expectedResult = TextInputFormatterResult(text: "12,348,097", carriagePosition: 9, unformattedText: "12348097")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12,345|.67|  ->  12,345,809|
    func test7() {
        let actualResult = self.formatter.format(
            text: "12,345.67",
            range: NSRange(location: 6, length: 3),
            replacementText: "809"
        )
        let expectedResult = TextInputFormatterResult(text: "12,345,809", carriagePosition: 10, unformattedText: "12345809")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
}
