//
//  CustomPriceTypePeriodValueFormatterTests.swift
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

/// See also NumberRangePriceFormatterTests and CustomPriceValueFormatterTests
final class CustomPriceTypePeriodValueFormatterTests: XCTestCase {
    func testPerOfferPerMonth() {
        let formatter = CustomPriceValuesFormatter()

        let priceRange = YVNumberRange(from: 123, to: 245)
        let priceValue = CustomPriceTypePeriodValue(priceRange: priceRange, priceType: .perOffer, pricePeriod: .perMonth)

        let value = formatter.string(fromPrice: priceValue)
        XCTAssertEqual(value, "от 123 до 245\(SpecialSymbol.nbsp)₽\(SpecialSymbol.nbsp)в месяц")
    }

    func testPerOfferPerYear() {
        let formatter = CustomPriceValuesFormatter()

        let priceRange = YVNumberRange(from: 123, to: 245)
        let priceValue = CustomPriceTypePeriodValue(priceRange: priceRange, priceType: .perOffer, pricePeriod: .perYear)

        let value = formatter.string(fromPrice: priceValue)
        XCTAssertEqual(value, "от 123 до 245\(SpecialSymbol.nbsp)₽\(SpecialSymbol.nbsp)в год")
    }

    func testPerSquareMeterPerMonth() {
        let formatter = CustomPriceValuesFormatter()

        let priceRange = YVNumberRange(from: 123, to: 245)
        let priceValue = CustomPriceTypePeriodValue(priceRange: priceRange, priceType: .perM2, pricePeriod: .perMonth)

        let value = formatter.string(fromPrice: priceValue)
        XCTAssertEqual(value, "от 123 до 245\(SpecialSymbol.nbsp)₽\(SpecialSymbol.nbsp)за м²\(SpecialSymbol.nbsp)в месяц")
    }

    func testPerSquareMeterPerYear() {
        let formatter = CustomPriceValuesFormatter()

        let priceRange = YVNumberRange(from: 123, to: 245)
        let priceValue = CustomPriceTypePeriodValue(priceRange: priceRange, priceType: .perM2, pricePeriod: .perYear)

        let value = formatter.string(fromPrice: priceValue)
        XCTAssertEqual(value, "от 123 до 245\(SpecialSymbol.nbsp)₽\(SpecialSymbol.nbsp)за м²\(SpecialSymbol.nbsp)в год")
    }

    func testPerArePerMonth() {
        let formatter = CustomPriceValuesFormatter()

        let priceRange = YVNumberRange(from: 123, to: 245)
        let priceValue = CustomPriceTypePeriodValue(priceRange: priceRange, priceType: .perAre, pricePeriod: .perMonth)

        let value = formatter.string(fromPrice: priceValue)
        XCTAssertEqual(value, "от 123 до 245\(SpecialSymbol.nbsp)₽\(SpecialSymbol.nbsp)за сотку\(SpecialSymbol.nbsp)в месяц")
    }

    func testPerArePerYear() {
        let formatter = CustomPriceValuesFormatter()

        let priceRange = YVNumberRange(from: 123, to: 245)
        let priceValue = CustomPriceTypePeriodValue(priceRange: priceRange, priceType: .perAre, pricePeriod: .perYear)

        let value = formatter.string(fromPrice: priceValue)
        XCTAssertEqual(value, "от 123 до 245\(SpecialSymbol.nbsp)₽\(SpecialSymbol.nbsp)за сотку\(SpecialSymbol.nbsp)в год")
    }

    func testEmptyValues() {
        let formatter = CustomPriceValuesFormatter()

        let priceRange = YVNumberRange(from: nil, to: nil)

        let priceValue1 = CustomPriceTypePeriodValue(priceRange: priceRange, priceType: .perOffer, pricePeriod: .perYear)
        let value1 = formatter.string(fromPrice: priceValue1)
        XCTAssertEqual(value1, "за всё\(SpecialSymbol.nbsp)в год")

        let priceValue2 = CustomPriceTypePeriodValue(priceRange: priceRange, priceType: .perOffer, pricePeriod: .perMonth)
        let value2 = formatter.string(fromPrice: priceValue2)
        XCTAssertEqual(value2, "за всё\(SpecialSymbol.nbsp)в месяц")

        let priceValue3 = CustomPriceTypePeriodValue(priceRange: priceRange, priceType: .perM2, pricePeriod: .perYear)
        let value3 = formatter.string(fromPrice: priceValue3)
        XCTAssertEqual(value3, "за м²\(SpecialSymbol.nbsp)в год")

        let priceValue4 = CustomPriceTypePeriodValue(priceRange: priceRange, priceType: .perM2, pricePeriod: .perMonth)
        let value4 = formatter.string(fromPrice: priceValue4)
        XCTAssertEqual(value4, "за м²\(SpecialSymbol.nbsp)в месяц")

        let priceValue5 = CustomPriceTypePeriodValue(priceRange: priceRange, priceType: .perAre, pricePeriod: .perYear)
        let value5 = formatter.string(fromPrice: priceValue5)
        XCTAssertEqual(value5, "за сотку\(SpecialSymbol.nbsp)в год")

        let priceValue6 = CustomPriceTypePeriodValue(priceRange: priceRange, priceType: .perAre, pricePeriod: .perMonth)
        let value6 = formatter.string(fromPrice: priceValue6)
        XCTAssertEqual(value6, "за сотку\(SpecialSymbol.nbsp)в месяц")
    }
}
