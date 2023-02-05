//
//  GCDTests.swift
//  YandexGeoToolboxTestApp
//
//  Created by Alexander Shchavrovskiy on 30.08.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest

class GCDTests: XCTestCase {
    
    func testRightTimeForDispatchAfter() {
        let exp = expectation(description: "")
        
        dispatch(after: 2.0) {
            exp.fulfill()
        }
        waitForExpectations(timeout: 2.01, handler: nil)
    }
}
