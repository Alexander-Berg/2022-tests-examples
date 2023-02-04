//
//  CustomPriceValueFormatterTests.swift
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

/// See also NumberRangePriceFormatterTests and CustomPriceTypePeriodValueFormatterTests
final class CustomPriceValueFormatterTests: XCTestCase {
    func testWithBothValues() {
        let formatter = CustomPriceValuesFormatter()

        let priceRange = YVNumberRange(from: 123, to: 245)
        let priceValue = CustomPriceValue(priceRange: priceRange, priceType: .perOffer)

        let value = formatter.string(fromPrice: priceValue)
        XCTAssertEqual(value, "от 123 до 245\(SpecialSymbol.nbsp)₽")
    }

    func testWithEqualValues() {
        let formatter = CustomPriceValuesFormatter()

        let priceRange = YVNumberRange(from: 123, to: 123)
        let priceValue = CustomPriceValue(priceRange: priceRange, priceType: .perOffer)

        let value = formatter.string(fromPrice: priceValue)
        XCTAssertEqual(value, "123\(SpecialSymbol.nbsp)₽")
    }

    func testWithFromOnlyValue() {
        let formatter = CustomPriceValuesFormatter()

        let priceRange = YVNumberRange(from: 123, to: nil)
        let priceValue = CustomPriceValue(priceRange: priceRange, priceType: .perOffer)

        let value = formatter.string(fromPrice: priceValue)
        XCTAssertEqual(value, "от 123\(SpecialSymbol.nbsp)₽")
    }

    func testWithToOnlyValue() {
        let formatter = CustomPriceValuesFormatter()

        let priceRange = YVNumberRange(from: nil, to: 245)
        let priceValue = CustomPriceValue(priceRange: priceRange, priceType: .perOffer)

        let value = formatter.string(fromPrice: priceValue)
        XCTAssertEqual(value, "до 245\(SpecialSymbol.nbsp)₽")
    }

    func testPerSquareMeter() {
        let formatter = CustomPriceValuesFormatter()

        let priceRange = YVNumberRange(from: 123, to: 245)
        let priceValue = CustomPriceValue(priceRange: priceRange, priceType: .perM2)

        let value = formatter.string(fromPrice: priceValue)
        XCTAssertEqual(value, "от 123 до 245\(SpecialSymbol.nbsp)₽\(SpecialSymbol.nbsp)за м²")
    }

    func testWithEmptyValuePerSquareMeter() {
        let formatter = CustomPriceValuesFormatter()

        let priceRange = YVNumberRange(from: nil, to: nil)
        let priceValue = CustomPriceValue(priceRange: priceRange, priceType: .perM2)

        let value = formatter.string(fromPrice: priceValue)
        XCTAssertEqual(value, "за м²")
    }

    func testPerAre() {
        let formatter = CustomPriceValuesFormatter()

        let priceRange = YVNumberRange(from: 123, to: 245)
        let priceValue = CustomPriceValue(priceRange: priceRange, priceType: .perAre)

        let value = formatter.string(fromPrice: priceValue)
        XCTAssertEqual(value, "от 123 до 245\(SpecialSymbol.nbsp)₽\(SpecialSymbol.nbsp)за сотку")
    }

    func testWithEmptyValuePerAre() {
        let formatter = CustomPriceValuesFormatter()

        let priceRange = YVNumberRange(from: nil, to: nil)
        let priceValue = CustomPriceValue(priceRange: priceRange, priceType: .perAre)

        let value = formatter.string(fromPrice: priceValue)
        XCTAssertEqual(value, "за сотку")
    }
}
