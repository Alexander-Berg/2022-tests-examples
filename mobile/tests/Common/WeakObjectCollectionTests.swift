//
//  WeakObjectCollectionTests.swift
//  YandexGeoToolboxTestApp
//
//  Created by Alexander Shchavrovskiy on 30.08.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest

class WeakObjectCollectionTests: XCTestCase {
    
    class SimpleClass: NSObject {
        let id: String
        init(id: String) {
            self.id = id
        }
        convenience override init() {
            self.init(id: "")
        }
    }
    
    func testInsertAndRemove() {
        let obj = SimpleClass()
        let collection = WeakObjectCollection<SimpleClass>()
      
        XCTAssert(!collection.contains(obj))
        
        collection.insert(obj)
        XCTAssert(collection.contains(obj))
        
        collection.remove(obj)
        XCTAssert(!collection.contains(obj))
    }
    
    func testThatCanInsertTheSameObjects() {
        let obj = SimpleClass()
        let collection = WeakObjectCollection<SimpleClass>()
        
        collection.insert(obj)
        collection.insert(obj)
        
        let array = collection.array()
        XCTAssertEqual(array.count, 2)
        XCTAssert(array.first == array.last)
        
    }
    
    func testThatCollectionEmptyWhenObjectsNil() {
        var obj: SimpleClass? = SimpleClass()
        let collection = WeakObjectCollection<SimpleClass>()
        collection.insert(obj!)
        
        obj = nil
        XCTAssertEqual(collection.array().count, 0)
        XCTAssertEqual(collection.pairs().count, 0)
    }
    
    func testClearCollection() {
        let obj: SimpleClass? = SimpleClass()
        let collection = WeakObjectCollection<SimpleClass>()
        
        collection.insert(obj!)
        XCTAssertEqual(collection.array().count, 1)
        
        collection.clear()
        XCTAssertEqual(collection.array().count, 0)
    }
    
}
