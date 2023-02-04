//
//  PriceRangeFormatterTests.swift
//  YREFormatters-Unit-YREFormattersTests
//
//  Created by Alexey Salangin on 7/13/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import enum Typograf.SpecialSymbol
import XCTest
import YREFormatters
import YREModel
import YREModelObjc

final class PriceRangeFormatterTests: XCTestCase {
    func testFromOnlyPrice() {
        let formatter = PriceRangeFormatter()
        let priceRange = YREPriceRange(
            from: YREPrice(currency: .RUB, value: NSNumber(value: 4_190_015), unit: .unknown, period: .unknown),
            to: nil,
            averagePrice: nil
        )

        let dashShortStyle = PriceRangeFormatter.Style(intervalDelimiterStyle: .dash, numberFormat: .short)
        XCTAssertEqual(
            "от 4,19 млн\(SpecialSymbol.nbsp)₽",
            formatter.string(fromPriceRange: priceRange, style: dashShortStyle)
        )

        let dashGroupedStyle = PriceRangeFormatter.Style(intervalDelimiterStyle: .dash, numberFormat: .grouped)
        XCTAssertEqual(
            "от 4\(SpecialSymbol.nbsp)190\(SpecialSymbol.nbsp)015\(SpecialSymbol.nbsp)₽",
            formatter.string(fromPriceRange: priceRange, style: dashGroupedStyle)
        )

        let fromToShortStyle = PriceRangeFormatter.Style(intervalDelimiterStyle: .fromTo, numberFormat: .short)
        XCTAssertEqual(
            "от 4,19 млн\(SpecialSymbol.nbsp)₽",
            formatter.string(fromPriceRange: priceRange, style: fromToShortStyle)
        )

        let fromToGroupedStyle = PriceRangeFormatter.Style(intervalDelimiterStyle: .fromTo, numberFormat: .grouped)
        XCTAssertEqual(
            "от 4\(SpecialSymbol.nbsp)190\(SpecialSymbol.nbsp)015\(SpecialSymbol.nbsp)₽",
            formatter.string(fromPriceRange: priceRange, style: fromToGroupedStyle)
        )
    }

    func testToOnlyPrice() {
        let formatter = PriceRangeFormatter()
        let priceRange = YREPriceRange(
            from: nil,
            to: YREPrice(currency: .RUB, value: NSNumber(value: 17_541_015), unit: .unknown, period: .unknown),
            averagePrice: nil
        )

        let dashShortStyle = PriceRangeFormatter.Style(intervalDelimiterStyle: .dash, numberFormat: .short)
        XCTAssertEqual(
            "до 17,54 млн\(SpecialSymbol.nbsp)₽",
            formatter.string(fromPriceRange: priceRange, style: dashShortStyle)
        )

        let dashGroupedStyle = PriceRangeFormatter.Style(intervalDelimiterStyle: .dash, numberFormat: .grouped)
        XCTAssertEqual(
            "до 17\(SpecialSymbol.nbsp)541\(SpecialSymbol.nbsp)015\(SpecialSymbol.nbsp)₽",
            formatter.string(fromPriceRange: priceRange, style: dashGroupedStyle)
        )

        let fromToShortStyle = PriceRangeFormatter.Style(intervalDelimiterStyle: .fromTo, numberFormat: .short)
        XCTAssertEqual(
            "до 17,54 млн\(SpecialSymbol.nbsp)₽",
            formatter.string(fromPriceRange: priceRange, style: fromToShortStyle)
        )

        let fromToGroupedStyle = PriceRangeFormatter.Style(intervalDelimiterStyle: .fromTo, numberFormat: .grouped)
        XCTAssertEqual(
            "до 17\(SpecialSymbol.nbsp)541\(SpecialSymbol.nbsp)015\(SpecialSymbol.nbsp)₽",
            formatter.string(fromPriceRange: priceRange, style: fromToGroupedStyle)
        )
    }

    func testBothPrice() {
        let formatter = PriceRangeFormatter()
        let priceRange = YREPriceRange(
            from: YREPrice(currency: .RUB, value: NSNumber(value: 4_190_015), unit: .unknown, period: .unknown),
            to: YREPrice(currency: .RUB, value: NSNumber(value: 17_541_015), unit: .unknown, period: .unknown),
            averagePrice: nil
        )

        let dashShortStyle = PriceRangeFormatter.Style(intervalDelimiterStyle: .dash, numberFormat: .short)
        XCTAssertEqual(
            "4,19 \(SpecialSymbol.mdash) 17,54 млн\(SpecialSymbol.nbsp)₽",
            formatter.string(fromPriceRange: priceRange, style: dashShortStyle)
        )

        let dashGroupedStyle = PriceRangeFormatter.Style(intervalDelimiterStyle: .dash, numberFormat: .grouped)
        XCTAssertEqual(
            "4\(SpecialSymbol.nbsp)190\(SpecialSymbol.nbsp)015 \(SpecialSymbol.mdash) 17\(SpecialSymbol.nbsp)" +
            "541\(SpecialSymbol.nbsp)015\(SpecialSymbol.nbsp)₽",
            formatter.string(fromPriceRange: priceRange, style: dashGroupedStyle)
        )

        let fromToShortStyle = PriceRangeFormatter.Style(intervalDelimiterStyle: .fromTo, numberFormat: .short)
        XCTAssertEqual(
            "от 4,19 до 17,54 млн\(SpecialSymbol.nbsp)₽",
            formatter.string(fromPriceRange: priceRange, style: fromToShortStyle)
        )

        let fromToGroupedStyle = PriceRangeFormatter.Style(intervalDelimiterStyle: .fromTo, numberFormat: .grouped)
        XCTAssertEqual(
            "от 4\(SpecialSymbol.nbsp)190\(SpecialSymbol.nbsp)015 до 17\(SpecialSymbol.nbsp)541" +
            "\(SpecialSymbol.nbsp)015\(SpecialSymbol.nbsp)₽",
            formatter.string(fromPriceRange: priceRange, style: fromToGroupedStyle)
        )
    }
}
