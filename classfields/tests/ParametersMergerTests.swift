//
//  ParametersMergerTests.swift
//  YandexRealtyTests
//
//  Created by Pavel Zhuravlev on 14.11.17.
//  Copyright © 2017 Yandex. All rights reserved.
//

import XCTest
import YREWeb

// swiftlint:disable file_length
// swiftlint:disable type_body_length
class ParametersMergerTests: XCTestCase {
    
    // MARK: - Common merging
    
    // Merging without repeated keys
    func testSimpleMerging() {
        let sourceDictionary = [
            "key 1": "value 1" as NSString,
            "key 2": "value 2" as NSString,
            "key 4": [
                "value 5" as NSString,
                "value 6" as NSString
            ] as NSArray
        ]
        
        let anotherDictionary = [
            "key 3": "value 3" as NSString,
            "key 5": [
                "value 7" as NSString,
                "value 8" as NSString
            ] as NSArray
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": "value 1" as NSString,
            "key 2": "value 2" as NSString,
            "key 3": "value 3" as NSString,
            "key 4": [
                "value 5" as NSString,
                "value 6" as NSString
            ] as NSArray,
            "key 5": [
                "value 7" as NSString,
                "value 8" as NSString
            ] as NSArray
        ]))
    }
    
    func testSimpleMergingWithInvertedOrder() {
        let sourceDictionary = [
            "key 1": "value 1" as NSString,
            "key 2": "value 2" as NSString
        ]
        
        let anotherDictionary = [
            "key 3": "value 3" as NSString,
            "key 4": [
                "value 5" as NSString,
                "value 6" as NSString
            ] as NSArray
        ]
        
        let result1 = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        let result2 = ParametersMerger.dictionaryByMerging(anotherDictionary, into: sourceDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result1, result2))
    }
    
    func testMutableMerging() {
        let sourceDictionary = [
            "key 1": "value 1" as NSString,
            "key 2": "value 2" as NSString
        ]
        
        let mutableDictionary = NSMutableDictionary()
        mutableDictionary["key 2"] = "value 4" as NSString
        mutableDictionary["key 3"] = "value 3" as NSString
        
        ParametersMerger.merge(sourceDictionary, into: mutableDictionary)
        
        if let dictionaryToCompare = mutableDictionary as? [String: AnyObject] {
            XCTAssertNoThrow(try ensureCollectionsEqual(dictionaryToCompare, [
                "key 1": "value 1" as NSString,
                "key 2": [
                    "value 4" as NSString,
                    "value 2" as NSString
                ] as NSOrderedSet,
                "key 3": "value 3" as NSString
            ]))
        }
        else {
            XCTFail("Couldn't convert resulting dictionary to a proper format")
        }
    }
    
    func testSimpleMergingWithSimilarKeys() {
        let sourceDictionary = [
            "key 1": "value 1" as NSString,
            "key 2": "value 2" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": "value 3" as NSString,
            "key 2": [
                "value 4" as NSString,
                "value 5" as NSString
            ] as NSArray
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": [
                "value 3" as NSString,
                "value 1" as NSString
            ] as NSOrderedSet,
            "key 2": [
                "value 4" as NSString,
                "value 5" as NSString,
                "value 2" as NSString
            ] as NSArray
        ]))
    }
    
    func testSimpleMergingWithRepeatedValues() {
        let sourceDictionary = [
            "key 1": "value 1" as NSString,
            "key 2": "value 2" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": "value 1" as NSString,
            "key 3": "value 3"
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": "value 1" as NSString,
            "key 2": "value 2" as NSString,
            "key 3": "value 3" as NSString
        ]))
    }
    
    // MARK: - Merging collections
    
    func testMergingWithArray() {
        let sourceDictionary = [
            "key 1": "value 1" as NSString,
            "key 2": "value 2" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": [
                "value 4" as NSString,
                "value 5" as NSString
            ] as NSArray
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": [
                "value 4" as NSString,
                "value 5" as NSString,
                "value 1" as NSString
            ] as NSArray,
            "key 2": "value 2" as NSString
        ]))
    }
    
    func testMergingWithSet() {
        let sourceDictionary = [
            "key 1": "value 1" as NSString,
            "key 2": "value 2" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": [
                "value 3" as NSString,
                "value 4" as NSString
            ] as NSSet,
            "key 3": "value 7" as NSString
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": [
                "value 3" as NSString,
                "value 4" as NSString,
                "value 1" as NSString
            ] as NSOrderedSet,
            "key 2": "value 2" as NSString,
            "key 3": "value 7" as NSString
        ]))
    }
    
    func testMergingWithOrderedSet() {
        let sourceDictionary = [
            "key 1": "value 1" as NSString,
            "key 2": "value 2" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": [
                "value 3" as NSString,
                "value 4" as NSString
            ] as NSOrderedSet,
            "key 3": "value 7" as NSString
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": [
                "value 3" as NSString,
                "value 4" as NSString,
                "value 1" as NSString
            ] as NSOrderedSet,
            "key 2": "value 2" as NSString,
            "key 3": "value 7" as NSString
        ]))
    }
    
    func testMergingFromArrayToOrderedSet() {
        let sourceDictionary = [
            "key 1": [
                "value 1" as NSString,
                "value 2" as NSString
            ] as NSArray,
            "key 2": "value 3" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": [
                "value 1" as NSString,
                "value 3" as NSString
            ] as NSOrderedSet,
            "key 3": "value 4" as NSString
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": [
                "value 1" as NSString,
                "value 3" as NSString,
                "value 2" as NSString
            ] as NSOrderedSet,
            "key 2": "value 3" as NSString,
            "key 3": "value 4" as NSString
        ]))
    }
    
    func testMergingFromArrayToArray() {
        let sourceDictionary = [
            "key 1": [
                "value 1" as NSString,
                "value 2" as NSString
            ] as NSArray,
            "key 2": "value 3" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": [
                "value 1" as NSString,
                "value 3" as NSString
            ] as NSArray,
            "key 3": "value 4" as NSString
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": [
                "value 1" as NSString,
                "value 3" as NSString,
                "value 1" as NSString,
                "value 2" as NSString
            ] as NSArray,
            "key 2": "value 3" as NSString,
            "key 3": "value 4" as NSString
        ]))
    }
    
    func testMergingFromOrderedSetToArray() {
        let sourceDictionary = [
            "key 1": [
                "value 1" as NSString,
                "value 2" as NSString
            ] as NSOrderedSet,
            "key 2": "value 3" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": [
                "value 1" as NSString,
                "value 3" as NSString
            ] as NSArray,
            "key 3": "value 4" as NSString
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": [
                "value 1" as NSString,
                "value 3" as NSString,
                "value 1" as NSString,
                "value 2" as NSString
            ] as NSArray,
            "key 2": "value 3" as NSString,
            "key 3": "value 4" as NSString
        ]))
    }
    
    func testMergingFromOrderedSetToOrderedSet() {
        let sourceDictionary = [
            "key 1": [
                "value 1" as NSString,
                "value 2" as NSString
            ] as NSOrderedSet,
            "key 2": "value 3" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": [
                "value 1" as NSString,
                "value 3" as NSString
            ] as NSOrderedSet,
            "key 3": "value 4" as NSString
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": [
                "value 1" as NSString,
                "value 3" as NSString,
                "value 2" as NSString
            ] as NSOrderedSet,
            "key 2": "value 3" as NSString,
            "key 3": "value 4" as NSString
        ]))
    }
    
    // MARK: - Merging with empty collection
    
    func testMergingFromSingleValueArrayToEmptyArray() {
        let sourceDictionary = [
            "key 1": ["value 1" as NSString] as NSArray,
            "key 2": "value 3" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": [] as NSArray,
            "key 3": "value 4" as NSString
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": ["value 1" as NSString] as NSArray,
            "key 2": "value 3" as NSString,
            "key 3": "value 4" as NSString
        ]))
    }
    
    func testMergingFromNonSingleValueArrayToEmptyArray() {
        let sourceDictionary = [
            "key 1": [
                "value 1" as NSString,
                "value 2" as NSString
            ] as NSArray,
            "key 2": "value 3" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": [] as NSArray,
            "key 3": "value 4" as NSString
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": [
                "value 1" as NSString,
                "value 2" as NSString
            ] as NSArray,
            "key 2": "value 3" as NSString,
            "key 3": "value 4" as NSString
        ]))
    }
    
    func testMergingFromEmptyArrayToSingleValueArray() {
        let sourceDictionary = [
            "key 1": [] as NSArray,
            "key 2": "value 3" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": ["value 1" as NSString] as NSArray,
            "key 3": "value 4" as NSString
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": ["value 1" as NSString] as NSArray,
            "key 2": "value 3" as NSString,
            "key 3": "value 4" as NSString
        ]))
    }
    
    func testMergingFromEmptyArrayToNonSingleValueArray() {
        let sourceDictionary = [
            "key 1": [] as NSArray,
            "key 2": "value 3" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": [
                "value 1" as NSString,
                "value 2" as NSString
            ] as NSArray,
            "key 3": "value 4" as NSString
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": [
                "value 1" as NSString,
                "value 2" as NSString
            ] as NSArray,
            "key 2": "value 3" as NSString,
            "key 3": "value 4" as NSString
        ]))
    }
    
    func testMergingFromSingleValueOrderedSetToEmptyOrderedSet() {
        let sourceDictionary = [
            "key 1": ["value 1" as NSString] as NSOrderedSet,
            "key 2": "value 3" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": [] as NSOrderedSet,
            "key 3": "value 4" as NSString
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": ["value 1" as NSString] as NSOrderedSet,
            "key 2": "value 3" as NSString,
            "key 3": "value 4" as NSString
        ]))
    }
    
    func testMergingFromNonSingleValueOrderedSetToEmptyOrderedSet() {
        let sourceDictionary = [
            "key 1": [
                "value 1" as NSString,
                "value 2" as NSString
            ] as NSOrderedSet,
            "key 2": "value 3" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": [] as NSOrderedSet,
            "key 3": "value 4" as NSString
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": [
                "value 1" as NSString,
                "value 2" as NSString
            ] as NSOrderedSet,
            "key 2": "value 3" as NSString,
            "key 3": "value 4" as NSString
        ]))
    }
    
    func testMergingFromEmptyOrderedSetToSingleValueOrderedSet() {
        let sourceDictionary = [
            "key 1": [] as NSOrderedSet,
            "key 2": "value 3" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": ["value 1" as NSString] as NSOrderedSet,
            "key 3": "value 4" as NSString
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": ["value 1" as NSString] as NSOrderedSet,
            "key 2": "value 3" as NSString,
            "key 3": "value 4" as NSString
        ]))
    }
    
    func testMergingFromEmptyOrderedSetToNonSingleValueOrderedSet() {
        let sourceDictionary = [
            "key 1": [] as NSOrderedSet,
            "key 2": "value 3" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": [
                "value 1" as NSString,
                "value 2" as NSString
            ] as NSOrderedSet,
            "key 3": "value 4" as NSString
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": [
                "value 1" as NSString,
                "value 2" as NSString
            ] as NSOrderedSet,
            "key 2": "value 3" as NSString,
            "key 3": "value 4" as NSString
        ]))
    }
    
    func testMergingFromSingleValueSetToEmptySet() {
        let sourceDictionary = [
            "key 1": ["value 1" as NSString] as NSSet,
            "key 2": "value 3" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": [] as NSSet,
            "key 3": "value 4" as NSString
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": ["value 1" as NSString] as NSOrderedSet,
            "key 2": "value 3" as NSString,
            "key 3": "value 4" as NSString
        ]))
    }
    
    func testMergingFromNonSingleValueSetToEmptySet() {
        let sourceDictionary = [
            "key 1": [
                "value 1" as NSString,
                "value 2" as NSString
            ] as NSSet,
            "key 2": "value 3" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": [] as NSSet,
            "key 3": "value 4" as NSString
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": [
                "value 1" as NSString,
                "value 2" as NSString
            ] as NSOrderedSet,
            "key 2": "value 3" as NSString,
            "key 3": "value 4" as NSString
        ]))
    }
    
    func testMergingFromEmptySetToSingleValueSet() {
        let sourceDictionary = [
            "key 1": [] as NSSet,
            "key 2": "value 3" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": ["value 1" as NSString] as NSSet,
            "key 3": "value 4" as NSString
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": ["value 1" as NSString] as NSOrderedSet,
            "key 2": "value 3" as NSString,
            "key 3": "value 4" as NSString
        ]))
    }
    
    func testMergingFromEmptySetToNonSingleValueSet() {
        let sourceDictionary = [
            "key 1": [] as NSSet,
            "key 2": "value 3" as NSString
        ]
        
        let anotherDictionary = [
            "key 1": [
                "value 1" as NSString,
                "value 2" as NSString
            ] as NSSet,
            "key 3": "value 4" as NSString
        ]
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": [
                "value 1" as NSString,
                "value 2" as NSString
            ] as NSOrderedSet,
            "key 2": "value 3" as NSString,
            "key 3": "value 4" as NSString
        ]))
    }
    
    // MARK: - Special cases
    
    func testMergingRepeatedRoomAreas() {
        let sourceDictionary = [
            "key 1": [
                "4" as NSString,
                "1" as NSString,
                "1" as NSString,
                "1" as NSString
            ] as NSArray
        ]
        let anotherDictionary = [String: AnyObject]()
        
        let result = ParametersMerger.dictionaryByMerging(sourceDictionary, into: anotherDictionary)
        
        XCTAssertNoThrow(try ensureCollectionsEqual(result, [
            "key 1": [
                "4" as NSString,
                "1" as NSString,
                "1" as NSString,
                "1" as NSString
            ] as NSArray
        ]))
    }
    
    // MARK: - Private
    
    private enum ComparisonError: Error {
        case collectionsHaveDifferentSize(size1: Int, size2: Int)
        case collectionHasNoValue(forKey: String)
        
        case plainValuesNotEqual(value1: AnyObject, value2: AnyObject)
        
        case complexValuesHaveDifferentSize(size1: Int, size2: Int)
        case complexValuesHaveDifferentItems(complexValue1: AnyObject, complexValue2: AnyObject)
        
        case wrongFormat
        case unsupportedTypes(type1: String, type2: String)
    }
    
    private func ensureCollectionsEqual(_ list: [String: AnyObject], _ anotherList: [String: AnyObject]) throws {
        if list.count != anotherList.count {
            throw ComparisonError.collectionsHaveDifferentSize(size1: list.count, size2: anotherList.count)
        }
        
        for (key, value) in list {
            guard let anotherValue = anotherList[key] else {
                throw ComparisonError.collectionHasNoValue(forKey: key)
            }
            
            try ensureAnyValuesEqual(value, anotherValue)
        }
    }
    
    private func ensureAnyValuesEqual(_ value: AnyObject, _ anotherValue: AnyObject) throws {
        
        if let valueString = value as? NSString,
           let anotherValueString = anotherValue as? NSString {
            
            if valueString != anotherValueString {
                throw ComparisonError.plainValuesNotEqual(value1: valueString, value2: anotherValueString)
            }
            
            return
        }
        
        if let setValue = value as? NSOrderedSet,
           let anotherSetValue = anotherValue as? NSOrderedSet {

            try ensureOrderedSetsEqual(setValue, anotherSetValue)

            return
        }

        if let setValue = value as? NSSet,
           let anotherSetValue = anotherValue as? NSSet {
            
            try ensureSetsEqual(setValue, anotherSetValue)
            
            return
        }
        
        if let arrayValue = value as? NSArray,
           let anotherArrayValue = anotherValue as? NSArray {
            
            try ensureArraysEqual(arrayValue, anotherArrayValue)
            
            return
        }

        throw ComparisonError.unsupportedTypes(type1: "\(type(of: value))", type2: "\(type(of: anotherValue))")
    }
    
    private func ensureSetsEqual(_ set: NSSet, _ anotherSet: NSSet) throws {
        guard set.count == anotherSet.count else {
            throw ComparisonError.complexValuesHaveDifferentSize(size1: set.count, size2: anotherSet.count)
        }
        
        if set != anotherSet {
            throw ComparisonError.complexValuesHaveDifferentItems(complexValue1: set, complexValue2: anotherSet)
        }
    }
    
    private func ensureOrderedSetsEqual(_ set: NSOrderedSet, _ anotherSet: NSOrderedSet) throws {
        guard set.count == anotherSet.count else {
            throw ComparisonError.complexValuesHaveDifferentSize(size1: set.count, size2: anotherSet.count)
        }

        if set != anotherSet {
            throw ComparisonError.complexValuesHaveDifferentItems(complexValue1: set, complexValue2: anotherSet)
        }
    }

    private func ensureArraysEqual(_ array: NSArray, _ anotherArray: NSArray) throws {
        guard array.count == anotherArray.count else {
            throw ComparisonError.complexValuesHaveDifferentSize(size1: array.count, size2: anotherArray.count)
        }
        
        if array != anotherArray {
            throw ComparisonError.complexValuesHaveDifferentItems(complexValue1: array, complexValue2: anotherArray)
        }
    }
}
