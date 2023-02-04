//
//  ExpectedMetroFormatterTests.swift
//  YandexRealtyTests
//
//  Created by Dmitry Barillo on 24/01/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest
import YREFormatters
import YREModel
import YREModelObjc

final class ExpectedMetroFormatterTests: XCTestCase {
    func testExpectedMetroWithZeroTime() {
        let time: UInt = 0
        
        let metroWithFootDistanceType: ExpectedMetro = self.expectedMetro(time: time, distanceType: .foot)
        let metroWithTransportDistanceType: ExpectedMetro = self.expectedMetro(time: time, distanceType: .transport)
        
        let footString: String! = ExpectedMetroFormatter.distanceString(from: metroWithFootDistanceType)
        let transportString: String! = ExpectedMetroFormatter.distanceString(from: metroWithTransportDistanceType)
        
        let footExpectedString = "меньше минуты\u{00a0}пешком"
        let transportExpectedString = "меньше минуты\u{00a0}на машине"
        
        XCTAssertEqual(footString, footExpectedString)
        XCTAssertEqual(transportString, transportExpectedString)
    }
    
    func testExpectedMetroWithNonZeroTime() {
        let time: UInt = 1
        
        let metroWithFootDistanceType: ExpectedMetro = self.expectedMetro(time: time, distanceType: .foot)
        let metroWithTransportDistanceType: ExpectedMetro = self.expectedMetro(time: time, distanceType: .transport)
        
        let footString: String! = ExpectedMetroFormatter.distanceString(from: metroWithFootDistanceType)
        let transportString: String! = ExpectedMetroFormatter.distanceString(from: metroWithTransportDistanceType)
        
        let footExpectedString = "1\u{00a0}мин.\u{00a0}пешком"
        let transportExpectedString = "1\u{00a0}мин.\u{00a0}на машине"
        
        XCTAssertEqual(footString, footExpectedString)
        XCTAssertEqual(transportString, transportExpectedString)
    }
    
    //
    // MARK: - Helper
    
    private func expectedMetro(time: UInt, distanceType: DistanceType) -> ExpectedMetro {
        let name: String = "Metro"
        let coordinate: MDCoords2D = MDCoords2D(lat: 0, lon: 0)
        let color: String = "000000"
        let year: UInt = 2019
        
        return ExpectedMetro(name: name,
                             coordinate: coordinate,
                             color: color,
                             year: year,
                             transport: distanceType,
                             time: time)
    }
}
