//
//  Created by Alexey Aleshkov on 03/10/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRENumberTextFormatter

// swiftlint:disable:next type_name
class NumberTextInputFormatterSpaceGroupSeparatorDelete1SymbolsTests: XCTestCase {
    let formatter = NumberTextInputFormatter(
        style: .init(groupingSeparator: " ", decimalSeparator: ",")
    )

    // |1|2 345,67  ->  |2 345,67
    func test1() {
        let actualResult = self.formatter.format(
          text: "12 345,67",
          range: NSRange(location: 0, length: 1),
          replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "2 345,67", carriagePosition: 0, unformattedText: "2345,67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
    
    // 1|2| 345,67  ->  1| 345,67
    func test2() {
        let actualResult = self.formatter.format(
          text: "12 345,67",
          range: NSRange(location: 1, length: 1),
          replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "1 345,67", carriagePosition: 1, unformattedText: "1345,67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
    
    // 12| |345,67  ->  12| 345,67
    func test3() {
        let actualResult = self.formatter.format(
          text: "12 345,67",
          range: NSRange(location: 2, length: 1),
          replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "12 345,67", carriagePosition: 2, unformattedText: "12345,67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
    
    // 12 |3|45,67  ->  1 2|45,67
    func test4() {
        let actualResult = self.formatter.format(
          text: "12 345,67",
          range: NSRange(location: 3, length: 1),
          replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "1 245,67", carriagePosition: 3, unformattedText: "1245,67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
    
    // 12 3|4|5,67  ->  1 23|5,67
    func test5() {
        let actualResult = self.formatter.format(
          text: "12 345,67",
          range: NSRange(location: 4, length: 1),
          replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "1 235,67", carriagePosition: 4, unformattedText: "1235,67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
    
    // 12 34|5|,67  ->  1 234|,67
    func test6() {
        let actualResult = self.formatter.format(
          text: "12 345,67",
          range: NSRange(location: 5, length: 1),
          replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "1 234,67", carriagePosition: 5, unformattedText: "1234,67")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
    
    // 12 345|,|67  ->  1 234 5|67
    func test7() {
        let actualResult = self.formatter.format(
          text: "12 345,67",
          range: NSRange(location: 6, length: 1),
          replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "1 234 567", carriagePosition: 7, unformattedText: "1234567")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
    
    // 12 345,|6|7  ->  12 345,|7
    func test8() {
        let actualResult = self.formatter.format(
          text: "12 345,67",
          range: NSRange(location: 7, length: 1),
          replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "12 345,7", carriagePosition: 7, unformattedText: "12345,7")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
    
    // 12 345,6|7|  ->  12 345,6
    func test9() {
        let actualResult = self.formatter.format(
          text: "12 345,67",
          range: NSRange(location: 8, length: 1),
          replacementText: ""
        )
        let expectedResult = TextInputFormatterResult(text: "12 345,6", carriagePosition: 8, unformattedText: "12345,6")
        XCTAssert(actualResult == expectedResult, "\n\(actualResult) must be equal to\n\(expectedResult)")
    }
}
