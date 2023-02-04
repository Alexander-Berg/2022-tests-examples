//
//  YREJSONObjectValidatorTests.swift
//  YandexRealtyTests
//
//  Created by Pavel Zhuravlev on 20.11.17.
//  Copyright © 2017 Yandex. All rights reserved.
//

import XCTest
import YREWeb

// - Top level object is an NSArray or NSDictionary
//     - All objects are NSString, NSNumber, NSArray, NSDictionary, or NSNull
// - All dictionary keys are NSStrings
// - NSNumbers are not NaN or infinity

class YREJSONObjectValidatorTests: XCTestCase {
    
    override func setUp() {
        super.setUp()
        // Put setup code here. This method is called before the invocation of each test method in the class.
    }
    
    override func tearDown() {
        // Put teardown code here. This method is called after the invocation of each test method in the class.
        super.tearDown()
    }
    
    // MARK: - NSSet
    
    func testTopLevelSet() {
        let validator = YREJSONObjectValidator()
        let object = NSSet()
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: false)
        
        XCTAssertNil(result)
    }
    
    func testAllowedTopLevelSet() {
        let validator = YREJSONObjectValidator()
        let object = NSSet()
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: true)
        
        guard let jsonObject = result else {
            XCTFail("Expected non-empty result")
            return
        }
        XCTAssert(JSONSerialization.isValidJSONObject(jsonObject))
    }
    
    func testEmbeddedSet() {
        let validator = YREJSONObjectValidator()
        let object = ["set": NSSet()]
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: false)
        
        guard let jsonObject = result else {
            XCTFail("Expected non-empty result")
            return
        }
        XCTAssert(JSONSerialization.isValidJSONObject(jsonObject))
    }
    
    // MARK: - NSOrderedSet
    
    func testTopLevelOrderedSet() {
        let validator = YREJSONObjectValidator()
        let object = NSOrderedSet()
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: false)
        
        XCTAssertNil(result)
    }
    
    func testAllowedTopLevelOrderedSet() {
        let validator = YREJSONObjectValidator()
        let object = NSOrderedSet()
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: true)
        
        guard let jsonObject = result else {
            XCTFail("Expected non-empty result")
            return
        }
        XCTAssert(JSONSerialization.isValidJSONObject(jsonObject))
    }
    
    func testEmbeddedOrderedSet() {
        let validator = YREJSONObjectValidator()
        let object = ["orderedSet": NSOrderedSet()]
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: false)
        
        guard let jsonObject = result else {
            XCTFail("Expected non-empty result")
            return
        }
        XCTAssert(JSONSerialization.isValidJSONObject(jsonObject))
    }
    
    // MARK: - NSIndexSet
    
    func testTopLevelIndexSet() {
        let validator = YREJSONObjectValidator()
        let object = NSIndexSet()
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: false)
        
        XCTAssertNil(result)
    }
    
    func testAllowedTopLevelIndexSet() {
        let validator = YREJSONObjectValidator()
        let object = NSIndexSet()
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: true)
        
        guard let jsonObject = result else {
            XCTFail("Expected non-empty result")
            return
        }
        XCTAssert(JSONSerialization.isValidJSONObject(jsonObject))
    }
    
    func testEmbeddedIndexSet() {
        let validator = YREJSONObjectValidator()
        let object = ["indexSet": NSIndexSet()]
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: false)
        
        guard let jsonObject = result else {
            XCTFail("Expected non-empty result")
            return
        }
        XCTAssert(JSONSerialization.isValidJSONObject(jsonObject))
    }
    
    // MARK: - NSNumber
    
    func testTopLevelNumber() {
        let validator = YREJSONObjectValidator()
        let object = NSNumber(value: 1)
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: false)
        
        XCTAssertNil(result)
    }
    
    func testAllowedTopLevelNumber() {
        let validator = YREJSONObjectValidator()
        let object = NSNumber(value: 1)
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: true)
        
        guard let jsonObject = result else {
            XCTFail("Expected non-empty result")
            return
        }
        // NSNumber is not valid top-level item for JSONSerialization
        XCTAssertFalse(JSONSerialization.isValidJSONObject(jsonObject))
    }
    
    func testEmbeddedNumber() {
        let validator = YREJSONObjectValidator()
        let object = ["number": NSNumber(value: 1)]
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: false)
        
        guard let jsonObject = result else {
            XCTFail("Expected non-empty result")
            return
        }
        XCTAssert(JSONSerialization.isValidJSONObject(jsonObject))
    }
    
    func testInfiniteNumber() {
        let validator = YREJSONObjectValidator()
        let object = ["number": NSNumber(value: Double.infinity)]
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: false)
        
        guard let jsonObject = result else {
            XCTFail("Expected non-empty result")
            return
        }
        XCTAssert(JSONSerialization.isValidJSONObject(jsonObject))
    }
    
    func testNanNumber() {
        let validator = YREJSONObjectValidator()
        let object = ["number": NSNumber(value: Double.nan)]
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: false)
        
        guard let jsonObject = result else {
            XCTFail("Expected non-empty result")
            return
        }
        XCTAssert(JSONSerialization.isValidJSONObject(jsonObject))
    }
    
    func testEmptyNumber() {
        let validator = YREJSONObjectValidator()
        let object = ["number": NSNumber()]
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: false)
        
        guard let jsonObject = result else {
            XCTFail("Expected non-empty result")
            return
        }
        XCTAssert(JSONSerialization.isValidJSONObject(jsonObject))
    }
    
    // MARK: - NSNull
    
    func testTopLevelNull() {
        let validator = YREJSONObjectValidator()
        let object = NSNull()
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: false)
        
        XCTAssertNil(result)
    }
    
    func testAllowedTopLevelNull() {
        let validator = YREJSONObjectValidator()
        let object = NSNull()
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: true)
        
        guard let jsonObject = result else {
            XCTFail("Expected non-empty result")
            return
        }
        // NSNull is not valid top-level item for JSONSerialization
        XCTAssertFalse(JSONSerialization.isValidJSONObject(jsonObject))
    }
    
    func testEmbeddedNull() {
        let validator = YREJSONObjectValidator()
        let object = ["null": NSNull()]
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: false)
        
        guard let jsonObject = result else {
            XCTFail("Expected non-empty result")
            return
        }
        XCTAssert(JSONSerialization.isValidJSONObject(jsonObject))
    }
    
    // MARK: - NSString
    
    func testTopLevelString() {
        let validator = YREJSONObjectValidator()
        let object = NSString()
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: false)
        
        XCTAssertNil(result)
    }
    
    func testAllowedTopLevelString() {
        let validator = YREJSONObjectValidator()
        let object = NSString()
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: true)
        
        guard let jsonObject = result else {
            XCTFail("Expected non-empty result")
            return
        }
        // NSString is not valid top-level item for JSONSerialization
        XCTAssertFalse(JSONSerialization.isValidJSONObject(jsonObject))
    }
    
    func testEmbeddedString() {
        let validator = YREJSONObjectValidator()
        let object = ["string": NSString()]
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: false)
        
        guard let jsonObject = result else {
            XCTFail("Expected non-empty result")
            return
        }
        XCTAssert(JSONSerialization.isValidJSONObject(jsonObject))
    }
    
    // MARK: - NSArray
    
    func testTopLevelArray() {
        let validator = YREJSONObjectValidator()
        let object = NSArray()
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: false)
        
        guard let jsonObject = result else {
            XCTFail("Expected non-empty result")
            return
        }
        XCTAssert(JSONSerialization.isValidJSONObject(jsonObject))
    }
    
    func testEmbeddedArray() {
        let validator = YREJSONObjectValidator()
        let object = ["array": NSArray()]
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: false)
        
        guard let jsonObject = result else {
            XCTFail("Expected non-empty result")
            return
        }
        XCTAssert(JSONSerialization.isValidJSONObject(jsonObject))
    }
    
    // MARK: - NSDictionary
    
    func testTopLevelDictionary() {
        let validator = YREJSONObjectValidator()
        let object = NSDictionary()
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: false)
        
        guard let jsonObject = result else {
            XCTFail("Expected non-empty result")
            return
        }
        XCTAssert(JSONSerialization.isValidJSONObject(jsonObject))
    }
    
    func testEmbeddedDictionary() {
        let validator = YREJSONObjectValidator()
        let object = ["dictionary": NSDictionary()]
        
        let result = try? validator.validTopLevelJSONObject(from: object,
                                                            allowFragments: false)
        
        guard let jsonObject = result else {
            XCTFail("Expected non-empty result")
            return
        }
        XCTAssert(JSONSerialization.isValidJSONObject(jsonObject))
    }
}
