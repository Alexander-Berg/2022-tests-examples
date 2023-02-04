//
//  Created by Alexey Aleshkov on 03/10/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRENumberTextFormatter

class NumberTextInputFormatterInput0Tests: XCTestCase {
    let formatter = NumberTextInputFormatter(
        style: .init(groupingSeparator: ",", decimalSeparator: ".")
    )

    // 20.|  ->  20.0|
    func test1() {
        let actualResult = self.formatter.format(
            text: "20.",
            range: NSRange(location: 3, length: 0),
            replacementText: "0"
        )
        let expectedResult = TextInputFormatterResult(text: "20.0", carriagePosition: 4, unformattedText: "20.0")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 20.0|  ->  20.00|
    func test2() {
        let actualResult = self.formatter.format(
            text: "20.0",
            range: NSRange(location: 4, length: 0),
            replacementText: "0"
        )
        let expectedResult = TextInputFormatterResult(text: "20.00", carriagePosition: 5, unformattedText: "20.0")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 20.0|  ->  20.000|
    func test3() {
        let actualResult = self.formatter.format(
            text: "20.0",
            range: NSRange(location: 4, length: 0),
            replacementText: "00"
        )
        let expectedResult = TextInputFormatterResult(text: "20.000", carriagePosition: 6, unformattedText: "20.0")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }

    // 2|.0  ->  200|.0
    func test4() {
        let actualResult = self.formatter.format(
            text: "2.0",
            range: NSRange(location: 1, length: 0),
            replacementText: "00"
        )
        let expectedResult = TextInputFormatterResult(text: "200.0", carriagePosition: 3, unformattedText: "200.0")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
}
