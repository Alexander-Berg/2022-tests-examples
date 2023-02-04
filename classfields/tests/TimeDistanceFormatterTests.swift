//
//  TimeDistanceFormatterTests.swift
//  YandexRealtyTests
//
//  Created by Dmitry Barillo on 23/01/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest
import YREFormatters
import YREModel
import YREModelObjc

class TimeDistanceFormatterTests: XCTestCase {
    func testTimeDistanceWithZeroTime() {
        let time: UInt = 0
        let distance: UInt = 0
        
        let footTimeDistance: TimeDistance! = TimeDistance(transport: .foot, time: time, distance: distance)
        let publicTransportTimeDistance: TimeDistance! = TimeDistance(transport: .publicTransport, time: time, distance: distance)
        let carTimeDistance: TimeDistance! = TimeDistance(transport: .car, time: time, distance: distance)
        
        let footString: String! = TimeDistanceFormatter.distanceString(from: footTimeDistance)
        let publicTransportString: String! = TimeDistanceFormatter.distanceString(from: publicTransportTimeDistance)
        let carString: String! = TimeDistanceFormatter.distanceString(from: carTimeDistance)
        
        let footExpectedString = "меньше минуты\u{00a0}пешком"
        let publicTransportExpectedString = "меньше минуты\u{00a0}на транспорте"
        let carExpectedString = "меньше минуты\u{00a0}на машине"
        
        XCTAssertEqual(footString, footExpectedString)
        XCTAssertEqual(publicTransportString, publicTransportExpectedString)
        XCTAssertEqual(carString, carExpectedString)
    }
    
    func testTimeDistanceWithNonZeroTime() {
        let time: UInt = 1
        let distance: UInt = 0
        
        let footTimeDistance: TimeDistance! = TimeDistance(transport: .foot, time: time, distance: distance)
        let publicTransportTimeDistance: TimeDistance! = TimeDistance(transport: .publicTransport, time: time, distance: distance)
        let carTimeDistance: TimeDistance! = TimeDistance(transport: .car, time: time, distance: distance)
        
        let footString: String! = TimeDistanceFormatter.distanceString(from: footTimeDistance)
        let publicTransportString: String! = TimeDistanceFormatter.distanceString(from: publicTransportTimeDistance)
        let carString: String! = TimeDistanceFormatter.distanceString(from: carTimeDistance)
        
        let footExpectedString = "1\u{00a0}мин.\u{00a0}пешком"
        let publicTransportExpectedString = "1\u{00a0}мин.\u{00a0}на транспорте"
        let carExpectedString = "1\u{00a0}мин.\u{00a0}на машине"
        
        XCTAssertEqual(footString, footExpectedString)
        XCTAssertEqual(publicTransportString, publicTransportExpectedString)
        XCTAssertEqual(carString, carExpectedString)
    }
}
