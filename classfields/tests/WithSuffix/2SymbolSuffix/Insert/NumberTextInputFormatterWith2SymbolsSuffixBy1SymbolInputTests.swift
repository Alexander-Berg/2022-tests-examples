//
//  Created by Alexey Aleshkov on 03/10/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRENumberTextFormatter

// swiftlint:disable:next type_name
class NumberTextInputFormatterWith2SymbolsSuffixBy1SymbolInputTests: XCTestCase {
    let formatter = NumberTextInputFormatter(
        style: .init(groupingSeparator: ",", decimalSeparator: ".", positiveSuffix: " $")
    )

    // "|"  ->  1| $
    func test1() {
        let actualResult = self.formatter.format(
            text: "",
            range: NSRange(location: 0, length: 0),
            replacementText: "1"
        )
        let expectedResult = TextInputFormatterResult(text: "1 $", carriagePosition: 1, unformattedText: "1")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 1| $  ->  12| $
    func test2() {
        let actualResult = self.formatter.format(
            text: "1 $",
            range: NSRange(location: 1, length: 0),
            replacementText: "2"
        )
        let expectedResult = TextInputFormatterResult(text: "12 $", carriagePosition: 2, unformattedText: "12")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12| $  ->  12.| $
    func test3() {
        let actualResult = self.formatter.format(
            text: "12 $",
            range: NSRange(location: 2, length: 0),
            replacementText: "."
        )
        let expectedResult = TextInputFormatterResult(text: "12. $", carriagePosition: 3, unformattedText: "12.0")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12.| $  ->  12.3 $
    func test4() {
        let actualResult = self.formatter.format(
            text: "12. $",
            range: NSRange(location: 3, length: 0),
            replacementText: "3"
        )
        let expectedResult = TextInputFormatterResult(text: "12.3 $", carriagePosition: 4, unformattedText: "12.3")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 12.3| $  ->  12.34| $
    func test5() {
        let actualResult = self.formatter.format(
            text: "12.3 $",
            range: NSRange(location: 4, length: 0),
            replacementText: "4"
        )
        let expectedResult = TextInputFormatterResult(text: "12.34 $", carriagePosition: 5, unformattedText: "12.34")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
}
