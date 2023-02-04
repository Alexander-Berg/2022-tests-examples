//
//  Created by Alexey Aleshkov on 03/10/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRENumberTextFormatter

// swiftlint:disable:next type_name
class NumberTextInputFormatterWith2SymbolsPrefixBy1SymbolErasingTests: XCTestCase {
    let formatter = NumberTextInputFormatter(
        style: .init(groupingSeparator: ",", decimalSeparator: ".", positivePrefix: "$ ")
    )

    // $ 12.3|4|  ->  $ 12.3|
    func test1() {
        let actualResult = self.formatter.format(
            text: "$ 12.34",
            range: NSRange(location: 6, length: 1),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "$ 12.3", carriagePosition: 6, unformattedText: "12.3")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // $ 12.|3|  ->  $ 12.|
    func test2() {
        let actualResult = self.formatter.format(
            text: "$ 12.3",
            range: NSRange(location: 5, length: 1),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "$ 12.", carriagePosition: 5, unformattedText: "12.0")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // $ 12|.|  ->  $ 12|
    func test3() {
        let actualResult = self.formatter.format(
            text: "$ 12.",
            range: NSRange(location: 4, length: 1),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "$ 12", carriagePosition: 4, unformattedText: "12")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // $ 1|2|  ->  $ 1|
    func test4() {
        let actualResult = self.formatter.format(
            text: "$ 12",
            range: NSRange(location: 3, length: 1),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "$ 1", carriagePosition: 3, unformattedText: "1")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // $ |1|  ->  "|"
    func test5() {
        let actualResult = self.formatter.format(
            text: "$ 1",
            range: NSRange(location: 2, length: 1),
            replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "", carriagePosition: 0, unformattedText: "")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
}
