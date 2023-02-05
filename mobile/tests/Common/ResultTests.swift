//
//  ResultTests.swift
//  YandexGeoToolboxTestApp
//
//  Created by Alexander Shchavrovskiy on 29.08.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest

class ResultTests: XCTestCase {
    
    func testThatValueOrErrorIsNil() {
        let resOk: Result<Bool,Bool> = .ok(true)
        XCTAssertNil(resOk.error)
        XCTAssertNotNil(resOk.value)
        
        let resError: Result<Bool,Bool> = .err(true)
        XCTAssertNotNil(resError.error)
        XCTAssertNil(resError.value)
        
        for ok in [true,false] {
            let res: Result<Bool,Bool> = ok ? .ok(ok) : .err(ok)
            
            switch res {
            case .ok(let r):
                XCTAssertNil(res.error)
                XCTAssertNotNil(res.value)
                XCTAssertEqual(r, ok)
                XCTAssertEqual(r, res.value)
            case.err(let r):
                XCTAssertNil(res.value)
                XCTAssertNotNil(res.error)
                XCTAssertEqual(r, ok)
                XCTAssertEqual(r, res.error)
            }
        }
    }
    
    func testMapChangeTypeForRightCase() {
        let res: Result<String, String> = .ok("ok")
        
        let newRes = res.map(value: { str in return true}, error: {str in return true} )
        
        XCTAssertEqual(newRes.value, true)
        XCTAssertNil(newRes.error)
    }
    
    func testMapChangeTypeForAnotherCase() {
        let res: Result<String, String> = .err("err")
        
        let newRes = res.map(value: { _ in true })
        
        XCTAssertNil(newRes.value)
        XCTAssertEqual(newRes.error, "err")
    }
    
}
