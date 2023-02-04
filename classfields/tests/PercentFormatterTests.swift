//
//  PercentFormatterTests.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 4/8/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import enum Typograf.SpecialSymbol
import YREFormatters

final class PercentFormatterTests: XCTestCase {
    func testIncorrectNumbers() {
        let formatter = PercentFormatter()

        let negativeNumber = NSNumber(value: -1)
        let negativeActualString: String? = formatter.string(from: negativeNumber)
        let negativeExpectedString: String? = nil
        XCTAssertEqual(negativeActualString, negativeExpectedString)

        let infinityNumber = NSNumber(value: Double.infinity)
        let infinityActualString: String? = formatter.string(from: infinityNumber)
        let infinityExpectedString: String? = nil
        XCTAssertEqual(infinityActualString, infinityExpectedString)
    }

    func testCorrectNumbers() {
        let formatter = PercentFormatter()

        let zeroNumber = NSNumber(value: 0)
        let zeroActualString: String? = formatter.string(from: zeroNumber)
        let zeroExpectedString: String = "0\(SpecialSymbol.nnbsp)%"
        XCTAssertEqual(zeroActualString, zeroExpectedString)

        let number = NSNumber(value: 36)
        let actualString: String? = formatter.string(from: number)
        let expectedString: String = "36\(SpecialSymbol.nnbsp)%"
        XCTAssertEqual(actualString, expectedString)

        let fractionDigitsNumber = NSNumber(value: 12.42412)
        let fractionDigitsActualString: String? = formatter.string(from: fractionDigitsNumber)
        let fractionDigitsExpectedString: String = "12,42\(SpecialSymbol.nnbsp)%"
        XCTAssertEqual(fractionDigitsActualString, fractionDigitsExpectedString)
    }

    func testPrefix() {
        let formatter = PercentFormatter()
        formatter.prefix = "от "

        let number = NSNumber(value: 36)
        let actualString: String? = formatter.string(from: number)
        let expectedString: String? = "от 36\(SpecialSymbol.nnbsp)%"
        XCTAssertEqual(actualString, expectedString)
    }
}
