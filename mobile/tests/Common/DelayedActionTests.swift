//
//  DelayedActionTests.swift
//  YandexGeoToolboxTestApp
//
//  Created by Alexander Shchavrovskiy on 25.08.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest

class DelayedActionTests: XCTestCase {
    
    var action: DelayedAction? = nil
    
    func testThatBlockCallInTime() {
        var flag: Bool? = nil
        let exp = expectation(description: "")
        action = DelayedAction.performAfterDelay(0.2) {
            flag = true
            exp.fulfill()
        }
        dispatch(after: 0.1) {
            XCTAssertNil(flag)
        }
        waitForExpectations(timeout: 0.3, handler: nil)
    }
    
    func testThatBlockDoesntCallWhenActionIsNil() {
        var flag: Bool? = nil
        
        action = DelayedAction.performAfterDelay(0.1) {
            flag = true
        }
        action = nil
        dispatch(after: 0.2) {
            XCTAssertNil(flag)
        }
    }
    
}
