//
//  NSTimeZone+ISO3339Tests.swift
//  YandexRealtyTests
//
//  Created by Mikhail Solodovnichenko on 6/6/18.
//  Copyright © 2018 Yandex. All rights reserved.
//

import XCTest

class NSTimeZoneISO3339Tests: XCTestCase {
    
    func testMoscowTimeZone() {
        let mskTimeZone = NSTimeZone(name: "Europe/Moscow")
        let timeZoneString = mskTimeZone?.yre_RFC3339_UTCOffsetString()
        
        // there's no DST for that timezone, so it should always be +03:00
        XCTAssertEqual(timeZoneString, "+03:00")
    }
    
    func testUT1TimeZone() {
        let UT1TimeZone = NSTimeZone(forSecondsFromGMT: 0)
        let timeZoneString = UT1TimeZone.yre_RFC3339_UTCOffsetString()
        
        XCTAssertEqual(timeZoneString, "+00:00")
    }
    
    func testNegativeOffsetTimeZone() {
        let negativeOffsetTimeZone = NSTimeZone(forSecondsFromGMT: -3600)
        let timeZoneString = negativeOffsetTimeZone.yre_RFC3339_UTCOffsetString()
        
        XCTAssertEqual(timeZoneString, "-01:00")
    }
}
