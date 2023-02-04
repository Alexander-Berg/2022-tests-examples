//
//  NumberRangePriceFormatterTests.swift
//  YREFormatters-Unit-Tests
//
//  Created by Pavel Zhuravlev on 31.07.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import verticalios
import enum Typograf.SpecialSymbol
import YREFiltersModel
import YREFormatters

/// See also CustomPriceValueFormatterTests and CustomPriceTypePeriodValueFormatterTests 
final class NumberRangePriceFormatterTests: XCTestCase {
    func testWithBothValues() {
        let priceRange = YVNumberRange(from: 123, to: 245)
        let formatter = CustomPriceValuesFormatter()

        let value = formatter.string(fromPriceRange: priceRange)
        XCTAssertEqual(value, "от 123 до 245\(SpecialSymbol.nbsp)₽")
    }

    func testWithEqualValues() {
        let priceRange = YVNumberRange(from: 123, to: 123)
        let formatter = CustomPriceValuesFormatter()

        let value = formatter.string(fromPriceRange: priceRange)
        XCTAssertEqual(value, "123\(SpecialSymbol.nbsp)₽")
    }

    func testWithFromOnlyValue() {
        let priceRange = YVNumberRange(from: 123, to: nil)
        let formatter = CustomPriceValuesFormatter()

        let value = formatter.string(fromPriceRange: priceRange)
        XCTAssertEqual(value, "от 123\(SpecialSymbol.nbsp)₽")
    }

    func testWithToOnlyValue() {
        let priceRange = YVNumberRange(from: nil, to: 245)
        let formatter = CustomPriceValuesFormatter()

        let value = formatter.string(fromPriceRange: priceRange)
        XCTAssertEqual(value, "до 245\(SpecialSymbol.nbsp)₽")
    }

    func testWithEmptyValues() {
        let priceRange = YVNumberRange(from: nil, to: nil)
        let formatter = CustomPriceValuesFormatter()

        let value = formatter.string(fromPriceRange: priceRange)
        XCTAssertEqual(value, "")
    }
}
