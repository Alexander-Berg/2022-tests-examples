//
//  StringToolTests.swift
//  YandexGeoToolboxTestApp
//
//  Created by Alexander Shchavrovskiy on 25.08.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest

class StringToolTests: XCTestCase {
    
    func testTrimming() {
        let correctString = "no spacing and newlines on both ends of the string"
        let testString = "\n no spacing and newlines on both ends of the string     \n\n "
        XCTAssertEqual(correctString, testString.trimmed())
    }
    
    func testCapitalizedStringWithSeveralLanguages() {
        let testString = "sлово with two languages"
        let correctString = "Sлово with two languages"
        XCTAssertEqual(correctString, testString.capitalizedString())
    }
    
    func testNoCapitalizationWithoutCapitalVariantsOfSymbol() {
        var testString = " space at first"
        XCTAssertEqual(testString, testString.capitalizedString())
        
        testString = "\nnewline at first"
        XCTAssertEqual(testString, testString.capitalizedString())
        
        testString = "@no letter symbol at first"
        XCTAssertEqual(testString, testString.capitalizedString())
    }
    
    
    func testRangesOfStringWithIntersections() {
        let testString = "foofoofoo"
        let stringToFind = "foofoo"
        
        let ranges = testString.ranges(of: stringToFind)
        let correctRange = testString.range(of: stringToFind)
        
        XCTAssertEqual(ranges, [correctRange!])
    }
}
