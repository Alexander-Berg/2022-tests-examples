//
//  CollectionHelpersTests.swift
//  YandexGeoToolboxTestApp
//
//  Created by Alexander Shchavrovskiy on 24.08.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest

class CollectionHelpersTests: XCTestCase {
    
    func testFlatten() {
        let array = [[1,2,3],[1,2,3],[1,2,3,4],[6,7],[8,9,10]]
        let checkingFlattenArray = [1,2,3,1,2,3,1,2,3,4,6,7,8,9,10]
        let flattenArray = flatten(array)
        XCTAssertEqual(checkingFlattenArray, flattenArray)
    }
    
    func testThatArrayOfEmptyArraysFlattenToEmptyArray() {
        let array:[[Bool]] = [[],[],[]]
        let flattenArray = flatten(array)
        
        XCTAssertEqual(flattenArray, [])
    }
    
    func testThatEmptyArrayFlattenToEmptyArray() {
        let array: [[Bool]] = []
        let flattenArray = flatten(array)
        
        XCTAssertEqual(flattenArray, [])
    }
    
    func testUniq() {
        let checkingUniqArray = [1,2,3,4]
        let array = [1,2,1,1,1,2,3,4]
        let uniqArray = uniq(array)
        
        XCTAssertEqual(checkingUniqArray, uniqArray)
    }
    
    func testThatEmptyArrayUniqToEmptyArray() {
        let array: [Bool] = []
        let uniqArray = uniq(array)
        XCTAssertEqual(uniqArray, [])
    }
    
    func testThatOneElementArrayUniqToSameArray() {
        let array = [true]
        let uniqArray = uniq(array)
        
        XCTAssertEqual(uniqArray, array)
    }
    
    func testThatUniqArrayInSameOrder() {
        let array = [true, false]
        let uniqArray = uniq(array)
        
        XCTAssertEqual(array, uniqArray)
    }
    
    func testChainOrder() {
        var emptyArray: [Int] = []
        let array: [Int] = [1,2,3]
        let exp = expectation(description: "")
        
        array.chain ({elem, index, resume, stop in
            dispatch(async: .main) {
                emptyArray.append(elem)
                resume()
            }
            }, completion: { exp.fulfill() } )
        
        waitForExpectations(timeout: 0.1, handler: nil)
        XCTAssertEqual(emptyArray, array)
    }
    
    func testThatChainStopCorrectly() {
        var emptyArray: [Int] = []
        let array: [Int] = [1,2,3]
        let shortArray: [Int] = [1,2]
        let stopIndex = 2
        
        let exp = expectation(description: "")
        
        array.chain( { (elem, index, resume, stop) in
            if index != stopIndex {
                emptyArray.append(elem)
                resume()
            } else {
                stop()
            }
            }, completion: { exp.fulfill() })
        
        waitForExpectations(timeout: 0.1, handler: nil)
        XCTAssertEqual(emptyArray, shortArray)
    }
    
    func testThatChainCompletionCallsAtTheEnd() {
        let array: [Int] = [1,2,3]
        let completionValue = 4
        let fullArray: [Int] = array + [completionValue]
        var emptyArray: [Int] = []
        let exp = expectation(description: "")
        array.chain( { (elem, index, resume, stop) in
            emptyArray.append(elem)
            resume()
            }, completion: {emptyArray.append(completionValue); exp.fulfill()})
        waitForExpectations(timeout: 0.1) { (err) in
            if err == nil {
                XCTAssertEqual(emptyArray, fullArray)
            } else {
                XCTAssert(false)
            }
        }
    }
    
    func testIndexedByToEmptyArray() {
        let emptyArray: [Bool] = []
        let testDict = emptyArray.indexedBy{ ($0,$0) }
        XCTAssertEqual(testDict, [Bool:Bool]())
    }
    
    func testIndexedByWithReturningNil() {
        let array: [Bool] = [true,false]
        let testDict = array.indexedBy { (elem) -> (key: Bool, value: Bool)? in
            return nil
        }
        XCTAssertEqual(testDict, [Bool:Bool]())
    }
    
    
    
}
