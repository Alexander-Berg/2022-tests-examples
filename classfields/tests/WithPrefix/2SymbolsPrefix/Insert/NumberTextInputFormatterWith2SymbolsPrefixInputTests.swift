//
//  Created by Alexey Aleshkov on 03/10/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRENumberTextFormatter

// swiftlint:disable:next type_name
class NumberTextInputFormatterWith2SymbolsPrefixInputTests: XCTestCase {
    let formatter = NumberTextInputFormatter(
        style: .init(groupingSeparator: ",", decimalSeparator: ".", positivePrefix: "$ ")
    )
    
    // |  ->  $ 1|
    func test1() {
        let actualResult = self.formatter.format(
            text: "",
            range: NSRange(location: 0, length: 0),
            replacementText: "1"
        )
        let expectedResult = TextInputFormatterResult(text: "$ 1", carriagePosition: 3, unformattedText: "1")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // $ 1|  ->  $ 1.|
    func test2() {
        let actualResult = self.formatter.format(
            text: "$ 1",
            range: NSRange(location: 3, length: 0),
            replacementText: "."
        )
        let expectedResult = TextInputFormatterResult(text: "$ 1.", carriagePosition: 4, unformattedText: "1.0")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // $ 1.|  ->  $ 1.2|
    func test3() {
        let actualResult = self.formatter.format(
            text: "$ 1.",
            range: NSRange(location: 4, length: 0),
            replacementText: "2"
        )
        let expectedResult = TextInputFormatterResult(text: "$ 1.2", carriagePosition: 5, unformattedText: "1.2")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // $ 1,234,567,891,234,567,891|.6 -> $ 12,345,678,912,345,678,912|.6
    func test4() {
        let actualResult = self.formatter.format(
            text: "$ 1,234,567,891,234,567,891.6",
            range: NSRange(location: 27, length: 0),
            replacementText: "2"
        )
        let expectedResult = TextInputFormatterResult(
            text: "$ 12,345,678,912,345,678,912.6",
            carriagePosition: 28,
            unformattedText: "12345678912345678912.6"
        )
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
}
