//
//  FloorRangeFormatterTests.swift
//  YandexRealtyTests
//
//  Created by Dmitry Barillo on 19/06/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest
import YREFormatters

class FloorRangeFormatterTests: XCTestCase {
    func testFromFloorGreaterThanToFloor() {
        let formatter = FloorRangeFormatter()
        let fromFloor: UInt = 5
        let toFloor: UInt = 12
        
        let floorRangeString = formatter.string(fromFloor: fromFloor, toFloor: toFloor)
        
        let floorRangeExpectedString: String? = "от \(fromFloor) до \(toFloor)"
        
        XCTAssertEqual(floorRangeString, floorRangeExpectedString)
    }
    
    func testFromFloorEqualToFloor() {
        let formatter = FloorRangeFormatter()
        let floor: UInt = 42
        
        let floorRangeString = formatter.string(fromFloor: floor, toFloor: floor)
        
        let floorRangeExpectedString: String? = "\(floor)"
        
        XCTAssertEqual(floorRangeString, floorRangeExpectedString)
    }
    
    func testFromFloor() {
        let formatter = FloorRangeFormatter()
        let fromFloor: UInt = 42
        let toFloor: UInt? = nil
        
        let floorRangeString = formatter.string(fromFloor: fromFloor, toFloor: toFloor)
        
        let floorRangeExpectedString: String? = "от \(fromFloor)"
        
        XCTAssertEqual(floorRangeString, floorRangeExpectedString)
    }
    
    func testToFloor() {
        let formatter = FloorRangeFormatter()
        let fromFloor: UInt? = nil
        let toFloor: UInt = 42
        
        let floorRangeString = formatter.string(fromFloor: fromFloor, toFloor: toFloor)
        
        let floorRangeExpectedString: String? = "до \(toFloor)"
        
        XCTAssertEqual(floorRangeString, floorRangeExpectedString)
    }
    
    func testFromZeroFloor() {
        let formatter = FloorRangeFormatter()
        let fromFloor: UInt = 0
        let toFloor: UInt = 42
        
        let floorRangeString = formatter.string(fromFloor: fromFloor, toFloor: toFloor)
        
        let floorRangeExpectedString: String? = "до \(toFloor)"
        
        XCTAssertEqual(floorRangeString, floorRangeExpectedString)
    }
    
    func testToZeroFloor() {
        let formatter = FloorRangeFormatter()
        let fromFloor: UInt = 42
        let toFloor: UInt = 0
        
        let floorRangeString = formatter.string(fromFloor: fromFloor, toFloor: toFloor)
        
        let floorRangeExpectedString: String? = nil
        
        XCTAssertEqual(floorRangeString, floorRangeExpectedString)
    }
    
    func testZeroFloor() {
        let formatter = FloorRangeFormatter()
        let fromFloor: UInt = 0
        let toFloor: UInt = 0
        
        let floorRangeString = formatter.string(fromFloor: fromFloor, toFloor: toFloor)

        let floorRangeExpectedString: String? = nil
        
        XCTAssertEqual(floorRangeString, floorRangeExpectedString)
    }
    
    func testWrongFloorsOrder() {
        let formatter = FloorRangeFormatter()
        let fromFloor: UInt = 10
        let toFloor: UInt = 9
        
        let floorRangeString = formatter.string(fromFloor: fromFloor, toFloor: toFloor)
        let floorRangeExpectedString: String? = nil
        
        XCTAssertEqual(floorRangeString, floorRangeExpectedString)
    }
}
