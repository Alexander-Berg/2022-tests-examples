//
//  FunctionalTests.swift
//  YandexGeoToolboxTestApp
//
//  Created by Alexander Shchavrovskiy on 25.08.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest
import Darwin

class FunctionalTests: XCTestCase {
    
    func testTrim() {
        var val = 1
        let interval = ClosedRange<Int>(uncheckedBounds: (lower: 0, upper: 2))
        
        var result = trim(val, interval: interval)
        XCTAssertEqual(result, val)
        
        val = interval.lowerBound
        result = trim(val, interval: interval)
        XCTAssertEqual(result, interval.lowerBound)
        
        val = interval.upperBound
        result = trim(val, interval: interval)
        XCTAssertEqual(result, interval.upperBound)
        
        val = interval.lowerBound - 1
        result = trim(val, interval: interval)
        XCTAssertEqual(result, interval.lowerBound)
        
        val = interval.upperBound + 1
        result = trim(val, interval: interval)
        XCTAssertEqual(result, interval.upperBound)
    }
    
    func testApplyEquality() {
        let obj = NSObject()
        
        let testObj = apply(obj) {_ in}
        XCTAssertEqual(obj, testObj)
    }

    func testApplyBlock() {
        let storage = UserDefaults.standard.makeScoped("testApply")
        let key = "flag"
        storage[key] = nil
        let testObj = apply(storage) { o in
            o[key] = true
        }
        XCTAssertEqual(testObj[key] as? Bool, true)
    }
}
