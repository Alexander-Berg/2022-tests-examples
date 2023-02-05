//
//  CommonGeometryTests.swift
//  YandexMaps
//
//  Created by Iskander Yadgarov on 27.03.17.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import XCTest
import YandexMapsUtils

class CommonGeometryTests: XCTestCase {
    
    func testProjection() {
        let a = (x: 0.0, y: 2.0)
        let b = (x: 4.0, y: 0.0)
        let line = (a: a, b: b)
        
        // Проекция должна упасть ровно на a
        if let point = projection(of: (x: 1, y: 4), to: line) {
            XCTAssert(point == a)
        } else {
            XCTAssert(false)
        }
        
        // Проекция падает мимо
        if projection(of: (x: 0.5, y: 4), to: line) != nil {
            XCTAssert(false)
        }
        if projection(of: (x: 6, y: 3), to: line) != nil {
            XCTAssert(false)
        }
        
        // Проекция лежит на линии
        if let point = projection(of: (x: 2, y: 1), to: line) {
            XCTAssert(point == (x: 2, y: 1))
        } else {
            XCTAssert(false)
        }
    }
    
}
