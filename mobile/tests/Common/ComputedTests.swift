//
//  ComputedTests.swift
//  YandexGeoToolboxTestApp
//
//  Created by Alexander Shchavrovskiy on 24.08.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest

class ComputedTests: XCTestCase {
    typealias SimpleComputed = Computed<Bool>
    typealias SimpleComputedBuilder = ComputedBuilder<Bool>
    
    func testComputedValueByReference() {
        var val = true
        let compValue: SimpleComputed = Computed{ val }
        XCTAssertEqual(val, compValue.value)
        val = false
        XCTAssertEqual(val, compValue.value)
        
    }
    
    func testComputedVaueByValue() {
        var val = true
        let compValue: SimpleComputed = Computed(val)
        XCTAssertEqual(val, compValue.value)
        val = false
        XCTAssertNotEqual(val, compValue.value)
    }

    
    func testInvalidation() {
        let val = true
        let compValue: SimpleComputed = Computed{ val }
        
        let exp = expectation(description: "")
        let listener = compValue.addListener{ compValue in
            exp.fulfill()
        }
        compValue.invalidate()
        waitForExpectations(timeout: 0, handler: nil)
    }
    
    func testComputedMapWithReference() {
        var val = true
        let firstComputed: SimpleComputed = Computed{ val }
        let mappedComputed: SimpleComputed = firstComputed.map{ val in return val }
        
        XCTAssertEqual(val, mappedComputed.value)
        
        val = false
        XCTAssertEqual(val, mappedComputed.value)
    }
    
    func testComputedMapWithValue() {
        var val = true
        let firstComputed: SimpleComputed = Computed(val)
        let mappedComputed: SimpleComputed = firstComputed.map{ value in return value }
        
        XCTAssertEqual(firstComputed.value, mappedComputed.value)
        
        val = false
        XCTAssertNotEqual(val, mappedComputed.value)
    }
    
    func testComputedBuilder() {
        var val = true
        let builder: SimpleComputedBuilder = SimpleComputedBuilder(initial: val)
        
        XCTAssertEqual(val, builder.value)
        XCTAssertEqual(builder.value, builder.computed.value)
        
        val = false
        XCTAssertEqual(builder.value, builder.computed.value)
        XCTAssertNotEqual(val, builder.value)
        
        let computed: SimpleComputed = builder.computed
        XCTAssertEqual(computed.value, builder.value)
        
        builder.value = !builder.value
        XCTAssertEqual(computed.value, builder.value)
    }
    
}
