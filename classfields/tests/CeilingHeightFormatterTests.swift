//
//  CeilingHeightFormatterTests.swift
//  YandexRealtyTests
//
//  Created by Dmitry Barillo on 19/06/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest
import YREFormatters

class CeilingHeightFormatterTests: XCTestCase {
    func testZeroCeilingHeight() {
        let formatter: CeilingHeightFormatter = Self.sharedCeilingHeightFormatter()
        
        let zeroHeight = NSNumber(value: 0)
        
        let zeroHeightInMetersString: String? = formatter.string(fromHeightInMeters: zeroHeight)
        let zeroHeightInCentimetersString: String? = formatter.string(fromHeightInCentimeters: zeroHeight)
        
        XCTAssertEqual(zeroHeightInMetersString, "0\u{00a0}м")
        XCTAssertEqual(zeroHeightInCentimetersString, "0\u{00a0}м")
    }
    
    func testCeilingHeightInCentimeters() {
        let formatter: CeilingHeightFormatter = Self.sharedCeilingHeightFormatter()
        
        let heightInCentimeters300 = NSNumber(value: 300)
        let heightInCentimeters310 = NSNumber(value: 310)
        let heightInCentimeters315 = NSNumber(value: 315)
        
        let heightInCentimeters300String: String? = formatter.string(fromHeightInCentimeters: heightInCentimeters300)
        let heightInCentimeters310String: String? = formatter.string(fromHeightInCentimeters: heightInCentimeters310)
        let heightInCentimeters315String: String? = formatter.string(fromHeightInCentimeters: heightInCentimeters315)
        
        XCTAssertEqual(heightInCentimeters300String, "3\u{00a0}м")
        XCTAssertEqual(heightInCentimeters310String, "3,1\u{00a0}м")
        XCTAssertEqual(heightInCentimeters315String, "3,15\u{00a0}м")
    }
    
    func testCeilingHeightInMeters() {
        let formatter: CeilingHeightFormatter = Self.sharedCeilingHeightFormatter()
        
        let heightInMeters300 = NSNumber(value: 3.00)
        let heightInMeters310 = NSNumber(value: 3.10)
        let heightInMeters315 = NSNumber(value: 3.15)
        let heightInMeters3155 = NSNumber(value: 3.155)
        
        let heightInMeters300String: String? = formatter.string(fromHeightInMeters: heightInMeters300)
        let heightInMeters310String: String? = formatter.string(fromHeightInMeters: heightInMeters310)
        let heightInMeters315String: String? = formatter.string(fromHeightInMeters: heightInMeters315)
        let heightInMeters3155String: String? = formatter.string(fromHeightInMeters: heightInMeters3155)
        
        XCTAssertEqual(heightInMeters300String, "3\u{00a0}м")
        XCTAssertEqual(heightInMeters310String, "3,1\u{00a0}м")
        XCTAssertEqual(heightInMeters315String, "3,15\u{00a0}м")
        XCTAssertEqual(heightInMeters3155String, "3,16\u{00a0}м")
    }
    
    //
    // MARK: - Interoperability
    
    func testZeroCeilingHeightFromObjc() {
        let formatter: CeilingHeightFormatter = Self.sharedCeilingHeightFormatter()
        
        let zeroHeight = NSNumber(value: 0)
        
        let zeroHeightInMetersString: String? = formatter.objc_string(fromHeightInMeters: zeroHeight)
        let zeroHeightInCentimetersString: String? = formatter.objc_string(fromHeightInCentimeters: zeroHeight)
        
        XCTAssertEqual(zeroHeightInMetersString, "0\u{00a0}м")
        XCTAssertEqual(zeroHeightInCentimetersString, "0\u{00a0}м")
    }
    
    func testCeilingHeightInCentimetersFromObjc() {
        let formatter: CeilingHeightFormatter = Self.sharedCeilingHeightFormatter()
        
        let heightInCentimeters300 = NSNumber(value: 300)
        let heightInCentimeters310 = NSNumber(value: 310)
        let heightInCentimeters315 = NSNumber(value: 315)
        
        let heightInCentimeters300String: String? = formatter.objc_string(fromHeightInCentimeters: heightInCentimeters300)
        let heightInCentimeters310String: String? = formatter.objc_string(fromHeightInCentimeters: heightInCentimeters310)
        let heightInCentimeters315String: String? = formatter.objc_string(fromHeightInCentimeters: heightInCentimeters315)
        
        XCTAssertEqual(heightInCentimeters300String, "3\u{00a0}м")
        XCTAssertEqual(heightInCentimeters310String, "3,1\u{00a0}м")
        XCTAssertEqual(heightInCentimeters315String, "3,15\u{00a0}м")
    }
    
    func testCeilingHeightInMetersFromObjc() {
        let formatter: CeilingHeightFormatter = Self.sharedCeilingHeightFormatter()
        
        let heightInMeters300 = NSNumber(value: 3.00)
        let heightInMeters310 = NSNumber(value: 3.10)
        let heightInMeters315 = NSNumber(value: 3.15)
        let heightInMeters3155 = NSNumber(value: 3.155)
        
        let heightInMeters300String: String? = formatter.objc_string(fromHeightInMeters: heightInMeters300)
        let heightInMeters310String: String? = formatter.objc_string(fromHeightInMeters: heightInMeters310)
        let heightInMeters315String: String? = formatter.objc_string(fromHeightInMeters: heightInMeters315)
        let heightInMeters3155String: String? = formatter.objc_string(fromHeightInMeters: heightInMeters3155)
        
        XCTAssertEqual(heightInMeters300String, "3\u{00a0}м")
        XCTAssertEqual(heightInMeters310String, "3,1\u{00a0}м")
        XCTAssertEqual(heightInMeters315String, "3,15\u{00a0}м")
        XCTAssertEqual(heightInMeters3155String, "3,16\u{00a0}м")
    }
}

//
// MARK: - Private

extension CeilingHeightFormatterTests {
    private static func sharedCeilingHeightFormatter() -> CeilingHeightFormatter {
        let numberFormatter: YRENumberFormatter = YRENumberFormatter()
        let formatter: CeilingHeightFormatter = CeilingHeightFormatter(numberFormatter: numberFormatter)
        
        return formatter
    }
}
