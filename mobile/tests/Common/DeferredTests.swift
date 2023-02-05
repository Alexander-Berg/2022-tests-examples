//
//  DeferredTests.swift
//  YandexGeoToolboxTestApp
//
//  Created by Alexander Shchavrovskiy on 24.08.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest

class DeferredTests: XCTestCase {
    typealias SimpleDefered = Deferred<Bool>
    
    func testThatDeferredFilledAfterFilling() {
        let deff = SimpleDefered()
        deff.fill(true)
        XCTAssert(deff.isFilled)
    }
    
    func testThatAllUponsCalled() {
        let deff = SimpleDefered()
        
        let numOfUpons = 3
        
        var expectations = [XCTestExpectation]()
        for i in 0..<numOfUpons {
            expectations.append(expectation(description: "upon number \(i+1)"))
        }
        
        for i in 0..<numOfUpons {
            deff.upon{ _ in
                expectations[i].fulfill()
            }
        }
        
        deff.fill(true)
        waitForExpectations(timeout: 3, handler: nil)
    }
    
    
    func testThatPeekIsNilBeforeFilling() {
        let deff = SimpleDefered()
        XCTAssertNil(deff.peek())
    }
    
    func testThatPeekNotNilInUponAndEqualToValue() {
        let deff = SimpleDefered()
        let exp = expectation(description: "")
        
        deff.upon{[weak deff] value in
            XCTAssertNotNil(deff!.peek())
            XCTAssertEqual(value, deff!.peek()!)
            exp.fulfill()
        }
        deff.fill(true)
        
        waitForExpectations(timeout: 0.1, handler: nil)
    }
    
    func testThatUponCalledInMomentIfFilled() {
        let exp = expectation(description: "")
        let deff = SimpleDefered()
        
        deff.fill(true)
        deff.upon{ _ in exp.fulfill() }
        
        waitForExpectations(timeout: 0, handler: nil)
    }
}
