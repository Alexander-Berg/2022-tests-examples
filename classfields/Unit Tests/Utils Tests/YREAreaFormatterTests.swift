//
//  YREAreaFormatterTests.swift
//  YandexRealtyTests
//
//  Created by Dmitry Barillo on 26/10/2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

import XCTest
import YREFormatters
import YREModel
import YREModelObjc

final class YREAreaFormatterTests: XCTestCase {
    
    func testDefaultFormatter() {
        let formatter = YREAreaFormatter()
        
        XCTAssertEqual(formatter.allowZeroValue, false)
        XCTAssertEqual(formatter.invalidAreaSubstitution, "-")
        XCTAssertEqual(formatter.style, AreaFormatterStyle.auto)
        XCTAssertEqual(formatter.fractionalDigits, 2)
        XCTAssertEqual(formatter.includeUnit, true)
    }
    
    // MARK: - allowZeroValue tests
    
    func testZeroValuesAllowed() {
        let formatter = YREAreaFormatter()
        formatter.allowZeroValue = true
        
        let zeroUnknownArea = YREArea(unit: .unknown, value: 0.0)
        let zeroM2Area = YREArea(unit: .m2, value: 0.0)
        let zeroHectareArea = YREArea(unit: .hectare, value: 0.0)
        let zeroAreArea = YREArea(unit: .are, value: 0.0)
        let zeroSquareKilometerArea = YREArea(unit: .squareKilometer, value: 0.0)

        XCTAssertEqual(formatter.string(from: zeroUnknownArea), "0")
        XCTAssertEqual(formatter.string(from: zeroM2Area), "0\u{00a0}м²")
        XCTAssertEqual(formatter.string(from: zeroHectareArea), "0\u{00a0}га")
        XCTAssertEqual(formatter.string(from: zeroAreArea), "0\u{00a0}соток")
        XCTAssertEqual(formatter.string(from: zeroSquareKilometerArea), "0\u{00a0}км²")
    }
    
    func testZeroValueAllowedWithoutUnits() {
        let formatter = YREAreaFormatter()
        formatter.allowZeroValue = true
        formatter.includeUnit = false
        
        let zeroUnknownArea = YREArea(unit: .unknown, value: 0.0)
        let zeroM2Area = YREArea(unit: .m2, value: 0.0)
        let zeroHectareArea = YREArea(unit: .hectare, value: 0.0)
        let zeroAreArea = YREArea(unit: .are, value: 0.0)
        let zeroSquareKilometerArea = YREArea(unit: .squareKilometer, value: 0.0)
        
        XCTAssertEqual(formatter.string(from: zeroUnknownArea), "0")
        XCTAssertEqual(formatter.string(from: zeroM2Area), "0")
        XCTAssertEqual(formatter.string(from: zeroHectareArea), "0")
        XCTAssertEqual(formatter.string(from: zeroAreArea), "0")
        XCTAssertEqual(formatter.string(from: zeroSquareKilometerArea), "0")
    }

    func testZeroValuesDisabled() {
        let formatter = YREAreaFormatter()
        
        let zeroUnknownArea = YREArea(unit: .unknown, value: 0.0)
        let zeroM2Area = YREArea(unit: .m2, value: 0.0)
        let zeroHectareArea = YREArea(unit: .hectare, value: 0.0)
        let zeroAreArea = YREArea(unit: .are, value: 0.0)
        let zeroSquareKilometerArea = YREArea(unit: .squareKilometer, value: 0.0)
        
        let invalidAreaSubstitution = formatter.invalidAreaSubstitution
        
        XCTAssertEqual(formatter.string(from: zeroUnknownArea), invalidAreaSubstitution)
        XCTAssertEqual(formatter.string(from: zeroM2Area), invalidAreaSubstitution)
        XCTAssertEqual(formatter.string(from: zeroHectareArea), invalidAreaSubstitution)
        XCTAssertEqual(formatter.string(from: zeroAreArea), invalidAreaSubstitution)
        XCTAssertEqual(formatter.string(from: zeroSquareKilometerArea), invalidAreaSubstitution)
    }
    
    
    func testZeroValuesDisabledWithCustomInvalidAreaSubstitution() {
        let formatter = YREAreaFormatter()
        formatter.invalidAreaSubstitution = "@"
        
        let zeroUnknownArea = YREArea(unit: .unknown, value: 0.0)
        let zeroM2Area = YREArea(unit: .m2, value: 0.0)
        let zeroHectareArea = YREArea(unit: .hectare, value: 0.0)
        let zeroAreArea = YREArea(unit: .are, value: 0.0)
        let zeroSquareKilometerArea = YREArea(unit: .squareKilometer, value: 0.0)
        
        let invalidAreaSubstitution = formatter.invalidAreaSubstitution
        
        XCTAssertEqual(formatter.string(from: zeroUnknownArea), invalidAreaSubstitution)
        XCTAssertEqual(formatter.string(from: zeroM2Area), invalidAreaSubstitution)
        XCTAssertEqual(formatter.string(from: zeroHectareArea), invalidAreaSubstitution)
        XCTAssertEqual(formatter.string(from: zeroAreArea), invalidAreaSubstitution)
        XCTAssertEqual(formatter.string(from: zeroSquareKilometerArea), invalidAreaSubstitution)
    }
    
    // MARK: - canDisplayArea tests
    
    func testCannotDisplayNilAreaForAutoStyle() {
        let formatter = YREAreaFormatter()
        
        let nilArea: YREArea? = nil
        
        XCTAssertFalse(formatter.canDisplay(nilArea))
    }
    
    func testCannotDisplayNilAreaForCustomStyle() {
        let formatter = YREAreaFormatter()
        formatter.style = .fixedSignificantFractionalDigits
        
        let nilArea: YREArea? = nil
        
        XCTAssertFalse(formatter.canDisplay(nilArea))
    }
    
    
    func testCanDisplayAreaZeroValueNotAllowed() {
        let formatter = YREAreaFormatter()

        let unknownArea: YREArea = YREArea(unit: .unknown, value: 0.00)
        let m2Area: YREArea = YREArea(unit: .m2, value: 0.00)
        let hectareArea: YREArea = YREArea(unit: .hectare, value: 0.00)
        let areArea: YREArea = YREArea(unit: .are, value: 0.00)
        let squareKilometerArea: YREArea = YREArea(unit: .squareKilometer, value: 0.00)

        XCTAssertFalse(formatter.canDisplay(unknownArea))
        XCTAssertFalse(formatter.canDisplay(m2Area))
        XCTAssertFalse(formatter.canDisplay(hectareArea))
        XCTAssertFalse(formatter.canDisplay(areArea))
        XCTAssertFalse(formatter.canDisplay(squareKilometerArea))
    }
    
    func testCanDisplayAreaZeroValueAllowed() {
        let formatter = YREAreaFormatter()
        formatter.allowZeroValue = true
        
        let unknownArea: YREArea = YREArea(unit: .unknown, value: 0.00)
        let m2Area: YREArea = YREArea(unit: .m2, value: 0.00)
        let hectareArea: YREArea = YREArea(unit: .hectare, value: 0.00)
        let areArea: YREArea = YREArea(unit: .are, value: 0.00)
        let squareKilometerArea: YREArea = YREArea(unit: .squareKilometer, value: 0.00)
        
        XCTAssertTrue(formatter.canDisplay(unknownArea))
        XCTAssertTrue(formatter.canDisplay(m2Area))
        XCTAssertTrue(formatter.canDisplay(hectareArea))
        XCTAssertTrue(formatter.canDisplay(areArea))
        XCTAssertTrue(formatter.canDisplay(squareKilometerArea))
    }
    
    func testCanDisplayArea() {
        let formatter = YREAreaFormatter()
        
        let unknownArea42: YREArea = YREArea(unit: .unknown, value: 42.00)
        let m2Area42: YREArea = YREArea(unit: .m2, value: 42.00)
        let hectareArea42: YREArea = YREArea(unit: .hectare, value: 42.00)
        let areArea42: YREArea = YREArea(unit: .are, value: 42.00)
        let squareKilometerArea42: YREArea = YREArea(unit: .squareKilometer, value: 42.00)
        
        XCTAssertTrue(formatter.canDisplay(unknownArea42))
        XCTAssertTrue(formatter.canDisplay(m2Area42))
        XCTAssertTrue(formatter.canDisplay(hectareArea42))
        XCTAssertTrue(formatter.canDisplay(areArea42))
        XCTAssertTrue(formatter.canDisplay(squareKilometerArea42))
    }
    
    // MARK: - Default formatter with .auto style
    
    func testFormatterWithAutoStyleForUnknownUnit() {
        let formatter = YREAreaFormatter()
    
        let area1 = YREArea(unit: .unknown, value: 1.00)
        let area100 = YREArea(unit: .unknown, value: 100.00)
        let area100_10 = YREArea(unit: .unknown, value: 100.10)
        let area100_50 = YREArea(unit: .unknown, value: 100.50)
        let area100_90 = YREArea(unit: .unknown, value: 100.90)
        
        XCTAssertEqual(formatter.string(from: area1), "1")
        XCTAssertEqual(formatter.string(from: area100), "100")
        XCTAssertEqual(formatter.string(from: area100_10), "100")
        XCTAssertEqual(formatter.string(from: area100_50), "100")
        XCTAssertEqual(formatter.string(from: area100_90), "100")
    }
    
    func testFormatterWithAutoStyleForM2Unit() {
        let formatter = YREAreaFormatter()
        
        let area1 = YREArea(unit: .m2, value: 1.00)
        let area100 = YREArea(unit: .m2, value: 100.00)
        let area100_10 = YREArea(unit: .m2, value: 100.10)
        let area100_50 = YREArea(unit: .m2, value: 100.50)
        let area100_95 = YREArea(unit: .m2, value: 100.95)
        let area100_942 = YREArea(unit: .m2, value: 100.942)
        
        let unitString = "м²"
        
        XCTAssertEqual(formatter.string(from: area1), "1\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100), "100\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_10), "100,1\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_50), "100,5\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_95), "101\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_942), "100,9\u{00a0}" + unitString)
    }
    
    func testFormatterWithAutoStyleForHectareUnit() {
        let formatter = YREAreaFormatter()
        
        let area1 = YREArea(unit: .hectare, value: 1.00)
        let area100 = YREArea(unit: .hectare, value: 100.00)
        let area100_10 = YREArea(unit: .hectare, value: 100.10)
        let area100_50 = YREArea(unit: .hectare, value: 100.50)
        let area100_90 = YREArea(unit: .hectare, value: 100.90)
        let area100_94 = YREArea(unit: .hectare, value: 100.94)
        let area100_942 = YREArea(unit: .hectare, value: 100.942)
        let area100_955 = YREArea(unit: .hectare, value: 100.955)
        let area100_101 = YREArea(unit: .hectare, value: 100.101)
        
        let unitString = "га"
        
        XCTAssertEqual(formatter.string(from: area1), "1\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100), "100\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_10), "100,1\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_50), "100,5\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_90), "100,9\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_94), "100,94\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_942), "100,94\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_955), "100,96\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_101), "100,1\u{00a0}" + unitString)
    }
    
    func testFormatterWithAutoStyleForAreUnit() {
        let formatter = YREAreaFormatter()
        
        let area1 = YREArea(unit: .are, value: 1.00)
        let area100 = YREArea(unit: .are, value: 100.00)
        let area100_10 = YREArea(unit: .are, value: 100.10)
        let area100_50 = YREArea(unit: .are, value: 100.50)
        let area100_90 = YREArea(unit: .are, value: 100.90)
        let area100_94 = YREArea(unit: .are, value: 100.94)
        let area100_942 = YREArea(unit: .are, value: 100.942)
        let area100_955 = YREArea(unit: .are, value: 100.955)
        let area100_101 = YREArea(unit: .are, value: 100.101)
        
        let unitStringOne = "сотка"
        let unitStringFew = "сотки"
        let unitStringMany = "соток"
        
        XCTAssertEqual(formatter.string(from: area1), "1\u{00a0}" + unitStringOne)
        XCTAssertEqual(formatter.string(from: area100), "100\u{00a0}" + unitStringMany)
        XCTAssertEqual(formatter.string(from: area100_10), "100,1\u{00a0}" + unitStringFew)
        XCTAssertEqual(formatter.string(from: area100_50), "100,5\u{00a0}" + unitStringFew)
        XCTAssertEqual(formatter.string(from: area100_90), "100,9\u{00a0}" + unitStringFew)
        XCTAssertEqual(formatter.string(from: area100_94), "100,94\u{00a0}" + unitStringFew)
        XCTAssertEqual(formatter.string(from: area100_942), "100,94\u{00a0}" + unitStringFew)
        XCTAssertEqual(formatter.string(from: area100_955), "100,96\u{00a0}" + unitStringFew)
        XCTAssertEqual(formatter.string(from: area100_101), "100,1\u{00a0}" + unitStringFew)
    }
    
    func testFormatterWithAutoStyleForSquareKilometerUnit() {
        let formatter = YREAreaFormatter()
        
        let area1 = YREArea(unit: .squareKilometer, value: 1.00)
        let area100 = YREArea(unit: .squareKilometer, value: 100.00)
        let area100_10 = YREArea(unit: .squareKilometer, value: 100.10)
        let area100_50 = YREArea(unit: .squareKilometer, value: 100.50)
        let area100_90 = YREArea(unit: .squareKilometer, value: 100.90)
        let area100_94 = YREArea(unit: .squareKilometer, value: 100.94)
        let area100_942 = YREArea(unit: .squareKilometer, value: 100.942)
        let area100_955 = YREArea(unit: .squareKilometer, value: 100.955)
        let area100_101 = YREArea(unit: .squareKilometer, value: 100.101)
        
        let unitString = "км²"
        
        XCTAssertEqual(formatter.string(from: area1), "1\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100), "100\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_10), "100,1\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_50), "100,5\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_90), "100,9\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_94), "100,94\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_942), "100,94\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_955), "100,96\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_101), "100,1\u{00a0}" + unitString)
    }

    // MARK: - Default formatter with .highlights style

    func testFormatterWithHighlightsStyleForUnknownUnit() {
        let formatter = YREAreaFormatter()
        formatter.style = .highlights

        let area1 = YREArea(unit: .unknown, value: 1.00)
        let area100 = YREArea(unit: .unknown, value: 100.00)
        let area100_10 = YREArea(unit: .unknown, value: 100.10)
        let area100_50 = YREArea(unit: .unknown, value: 100.50)
        let area100_90 = YREArea(unit: .unknown, value: 100.90)

        XCTAssertEqual(formatter.string(from: area1), "1")
        XCTAssertEqual(formatter.string(from: area100), "100")
        XCTAssertEqual(formatter.string(from: area100_10), "100")
        XCTAssertEqual(formatter.string(from: area100_50), "100")
        XCTAssertEqual(formatter.string(from: area100_90), "100")
    }

    func testFormatterWithHighlightsStyleForM2Unit() {
        let formatter = YREAreaFormatter()
        formatter.style = .highlights

        let area1 = YREArea(unit: .m2, value: 1.00)
        let area100 = YREArea(unit: .m2, value: 100.00)
        let area100_10 = YREArea(unit: .m2, value: 100.10)
        let area100_50 = YREArea(unit: .m2, value: 100.50)
        let area100_95 = YREArea(unit: .m2, value: 100.95)
        let area100_942 = YREArea(unit: .m2, value: 100.942)

        let unitString = "м²"

        XCTAssertEqual(formatter.string(from: area1), "1\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100), "100\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_10), "100,1\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_50), "100,5\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_95), "101\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_942), "100,9\u{00a0}" + unitString)
    }

    func testFormatterWithHighlightsStyleForHectareUnit() {
        let formatter = YREAreaFormatter()
        formatter.style = .highlights

        let area1 = YREArea(unit: .hectare, value: 1.00)
        let area100 = YREArea(unit: .hectare, value: 100.00)
        let area100_10 = YREArea(unit: .hectare, value: 100.10)
        let area100_50 = YREArea(unit: .hectare, value: 100.50)
        let area100_90 = YREArea(unit: .hectare, value: 100.90)
        let area100_94 = YREArea(unit: .hectare, value: 100.94)
        let area100_942 = YREArea(unit: .hectare, value: 100.942)
        let area100_955 = YREArea(unit: .hectare, value: 100.955)
        let area100_101 = YREArea(unit: .hectare, value: 100.101)

        let unitString = "га"

        XCTAssertEqual(formatter.string(from: area1), "1\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100), "100\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_10), "100,1\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_50), "100,5\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_90), "100,9\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_94), "100,94\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_942), "100,94\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_955), "100,96\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_101), "100,1\u{00a0}" + unitString)
    }

    func testFormatterWithHighlightsStyleForAreUnit() {
        let formatter = YREAreaFormatter()
        formatter.style = .highlights

        let area1 = YREArea(unit: .are, value: 1.00)
        let area100 = YREArea(unit: .are, value: 100.00)
        let area100_10 = YREArea(unit: .are, value: 100.10)
        let area100_50 = YREArea(unit: .are, value: 100.50)
        let area100_90 = YREArea(unit: .are, value: 100.90)
        let area100_94 = YREArea(unit: .are, value: 100.94)
        let area100_942 = YREArea(unit: .are, value: 100.942)
        let area100_955 = YREArea(unit: .are, value: 100.955)
        let area100_101 = YREArea(unit: .are, value: 100.101)

        let unitString = "сот."

        XCTAssertEqual(formatter.string(from: area1), "1\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100), "100\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_10), "100,1\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_50), "100,5\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_90), "100,9\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_94), "100,94\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_942), "100,94\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_955), "100,96\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_101), "100,1\u{00a0}" + unitString)
    }

    func testFormatterWithHighlightsStyleForSquareKilometerUnit() {
        let formatter = YREAreaFormatter()
        formatter.style = .highlights

        let area1 = YREArea(unit: .squareKilometer, value: 1.00)
        let area100 = YREArea(unit: .squareKilometer, value: 100.00)
        let area100_10 = YREArea(unit: .squareKilometer, value: 100.10)
        let area100_50 = YREArea(unit: .squareKilometer, value: 100.50)
        let area100_90 = YREArea(unit: .squareKilometer, value: 100.90)
        let area100_94 = YREArea(unit: .squareKilometer, value: 100.94)
        let area100_942 = YREArea(unit: .squareKilometer, value: 100.942)
        let area100_955 = YREArea(unit: .squareKilometer, value: 100.955)
        let area100_101 = YREArea(unit: .squareKilometer, value: 100.101)

        let unitString = "км²"

        XCTAssertEqual(formatter.string(from: area1), "1\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100), "100\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_10), "100,1\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_50), "100,5\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_90), "100,9\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_94), "100,94\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_942), "100,94\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_955), "100,96\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_101), "100,1\u{00a0}" + unitString)
    }
    
    // MARK: - Default formatter with .fixedSignificantFractionalDigits style
    
    func testFormatterWithCustomStyleForUnknownUnit() {
        let formatter = YREAreaFormatter()
        formatter.style = .fixedSignificantFractionalDigits
        formatter.fractionalDigits = 1
        
        let area1 = YREArea(unit: .unknown, value: 1.00)
        let area100 = YREArea(unit: .unknown, value: 100.00)
        let area100_10 = YREArea(unit: .unknown, value: 100.10)
        let area100_50 = YREArea(unit: .unknown, value: 100.50)
        let area100_90 = YREArea(unit: .unknown, value: 100.90)
        
        XCTAssertEqual(formatter.string(from: area1), "1,0")
        XCTAssertEqual(formatter.string(from: area100), "100,0")
        XCTAssertEqual(formatter.string(from: area100_10), "100,1")
        XCTAssertEqual(formatter.string(from: area100_50), "100,5")
        XCTAssertEqual(formatter.string(from: area100_90), "100,9")
    }
    
    func testFormatterWithCustomStyleForM2Unit() {
        let formatter = YREAreaFormatter()
        formatter.style = .fixedSignificantFractionalDigits
        formatter.fractionalDigits = 1
        
        let area1 = YREArea(unit: .m2, value: 1.00)
        let area100 = YREArea(unit: .m2, value: 100.00)
        let area100_10 = YREArea(unit: .m2, value: 100.10)
        let area100_50 = YREArea(unit: .m2, value: 100.50)
        let area100_90 = YREArea(unit: .m2, value: 100.90)
        
        let unitString = "м²"
        
        XCTAssertEqual(formatter.string(from: area1), "1,0\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100), "100,0\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_10), "100,1\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_50), "100,5\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_90), "100,9\u{00a0}" + unitString)
    }
    
    func testFormatterWithCustomStyleForHectareUnit() {
        let formatter = YREAreaFormatter()
        formatter.style = .fixedSignificantFractionalDigits
        formatter.fractionalDigits = 1
        
        let area1 = YREArea(unit: .hectare, value: 1.00)
        let area100 = YREArea(unit: .hectare, value: 100.00)
        let area100_10 = YREArea(unit: .hectare, value: 100.10)
        let area100_50 = YREArea(unit: .hectare, value: 100.50)
        let area100_90 = YREArea(unit: .hectare, value: 100.90)
        
        let unitString = "га"
        
        XCTAssertEqual(formatter.string(from: area1), "1,0\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100), "100,0\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_10), "100,1\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_50), "100,5\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_90), "100,9\u{00a0}" + unitString)
    }
    
    func testFormatterWithCustomStyleForAreUnit() {
        let formatter = YREAreaFormatter()
        formatter.style = .fixedSignificantFractionalDigits
        formatter.fractionalDigits = 1
        
        let area1 = YREArea(unit: .are, value: 1.00)
        let area100 = YREArea(unit: .are, value: 100.00)
        let area100_10 = YREArea(unit: .are, value: 100.10)
        let area100_50 = YREArea(unit: .are, value: 100.50)
        let area100_90 = YREArea(unit: .are, value: 100.90)
        
        let unitStringOne = "сотка"
        let unitStringFew = "сотки"
        let unitStringMany = "соток"
        
        XCTAssertEqual(formatter.string(from: area1), "1,0\u{00a0}" + unitStringOne)
        XCTAssertEqual(formatter.string(from: area100), "100,0\u{00a0}" + unitStringMany)
        XCTAssertEqual(formatter.string(from: area100_10), "100,1\u{00a0}" + unitStringFew)
        XCTAssertEqual(formatter.string(from: area100_50), "100,5\u{00a0}" + unitStringFew)
        XCTAssertEqual(formatter.string(from: area100_90), "100,9\u{00a0}" + unitStringFew)
    }
    
    func testFormatterWithCustomStyleForSquareKilometerUnit() {
        let formatter = YREAreaFormatter()
        formatter.style = .fixedSignificantFractionalDigits
        formatter.fractionalDigits = 1
        
        let area1 = YREArea(unit: .squareKilometer, value: 1.00)
        let area100 = YREArea(unit: .squareKilometer, value: 100.00)
        let area100_10 = YREArea(unit: .squareKilometer, value: 100.10)
        let area100_50 = YREArea(unit: .squareKilometer, value: 100.50)
        let area100_90 = YREArea(unit: .squareKilometer, value: 100.90)
        
        let unitString = "км²"
        
        XCTAssertEqual(formatter.string(from: area1), "1,0\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100), "100,0\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_10), "100,1\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_50), "100,5\u{00a0}" + unitString)
        XCTAssertEqual(formatter.string(from: area100_90), "100,9\u{00a0}" + unitString)
    }
    
    func testValuesLessThanOne() {
        let formatter = YREAreaFormatter()
        formatter.style = .fixedSignificantFractionalDigits
        formatter.fractionalDigits = 1
        
        let zeroUnknownArea = YREArea(unit: .unknown, value: 0.5)
        let zeroM2Area = YREArea(unit: .m2, value: 0.5)
        let zeroHectareArea = YREArea(unit: .hectare, value: 0.5)
        let zeroAreArea = YREArea(unit: .are, value: 0.5)
        let zeroSquareKilometerArea = YREArea(unit: .squareKilometer, value: 0.5)
        
        XCTAssertEqual(formatter.string(from: zeroUnknownArea), "0,5")
        XCTAssertEqual(formatter.string(from: zeroM2Area), "0,5\u{00a0}м²")
        XCTAssertEqual(formatter.string(from: zeroHectareArea), "0,5\u{00a0}га")
        XCTAssertEqual(formatter.string(from: zeroAreArea), "0,5\u{00a0}сотки")
        XCTAssertEqual(formatter.string(from: zeroSquareKilometerArea), "0,5\u{00a0}км²")
    }
    
    func testDecimalRounding() {
        let formatter = YREAreaFormatter()
        formatter.style = .auto
        
        
        let m2UnitString: String = "м²"
        let m2Unit: ConstantParamAreaUnit = .m2
        
        // Yeah, this won't work, because 1.15 is still 1.49(9), we need `Decimal`
        // (i.e. parse server responce into `Decimal`)
//        let unrepresentableDot15M2 = YREArea(unit: m2Unit, value: Double(1.15))
//        XCTAssertEqual(formatter.string(from: unrepresentableDot15M2), "1,2\u{00a0}" + m2UnitString)
        
        let onePointSeventyFiveM2 = YREArea(unit: m2Unit, value: 1.75)
        XCTAssertEqual(formatter.string(from: onePointSeventyFiveM2), "1,8\u{00a0}" + m2UnitString)
    }
}
