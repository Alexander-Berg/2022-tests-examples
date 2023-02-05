//
//  KeyValueCollectionTests.swift
//  YandexGeoToolboxTestApp
//
//  Created by Aleksey Fedotov on 15.11.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest

class KeyValueCollectionTest: XCTestCase {
    
    private class func makeEmptyDatabase() -> KeyValueCollection {
        let databaseAdapter = DatabaseAdapterTestImpl(id: "KeyValueCollection")
        return KeyValueCollection(database: Database(adapter: databaseAdapter),
                                  collectionId: "KeyValueCollection")
    }
    
    func testThatBeginAndEndEditingChangesEditingField() {
        let exp = expectation(description: "")
        
        let database = type(of: self).makeEmptyDatabase()
        
        database.open { err in
            exp.fulfill()
            
            guard err == nil else {
                XCTFail()
                return
            }
            
            XCTAssert(!database.editing)
            database.beginEditing()
            XCTAssert(database.editing)
            database.endEditingAndSave()
            XCTAssert(!database.editing)
            XCTAssert(database.isValid)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testAddNewValueToEmptyDatabase() {
        let exp = expectation(description: "")
        
        let database = type(of: self).makeEmptyDatabase()
        
        database.open { err in
            exp.fulfill()
            
            guard err == nil else {
                XCTFail()
                return
            }
            
            XCTAssert(database.isEmpty)
            
            database.beginEditing()
            database["first"] = .value(WrappedValue(0))
            database.endEditingAndSave()
            
            XCTAssert(database.count == 1)
            guard let el = database["first"] else { XCTFail(); return }
            guard let v = el.value else { XCTFail(); return }
            guard let i = v.int else { XCTFail(); return }
            XCTAssert(i == 0)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testAddAndUpdateValueToEmptyDatabase() {
        let exp = expectation(description: "")
        
        let database = type(of: self).makeEmptyDatabase()
        
        database.open { err in
            exp.fulfill()
            
            guard err == nil else { XCTFail(); return }
            
            database.beginEditing()
            database["first"] = .value(WrappedValue(0))
            database["first"] = .value(WrappedValue("first"))
            database.endEditingAndSave()
            
            XCTAssert(database.count == 1)
            guard let el = database["first"] else { XCTFail(); return }
            guard let val = el.value else { XCTFail(); return }
            guard let str = val.string else { XCTFail(); return }
            XCTAssert(str == "first")
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testAddAndRemoveItems() {
        let exp = expectation(description: "")
        
        let database = type(of: self).makeEmptyDatabase()
        
        database.open { err in
            exp.fulfill()
            
            guard err == nil else { XCTFail(); return }
            
            database.beginEditing()
            database["first"] = .value(WrappedValue(0))
            database["second"] = .value(WrappedValue(true))
            database.endEditingAndSave()
            
            XCTAssert(database.count == 2)
            
            database.beginEditing()
            database["first"] = nil
            database.endEditingAndSave()
            
            XCTAssert(database.count == 1)
            
            guard database["second"] != nil else { XCTFail(); return }
            guard database["first"] == nil else { XCTFail(); return }
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testRemoveNonExistantItem() {
        let exp = expectation(description: "")
        
        let database = type(of: self).makeEmptyDatabase()
        
        database.open { err in
            exp.fulfill()
            
            guard err == nil else { XCTFail(); return }
            
            database.beginEditing()
            database["first"] = .value(WrappedValue(0))
            database["second"] = .value(WrappedValue(true))
            database.endEditingAndSave()
            
            XCTAssert(database.count == 2)
            
            database.beginEditing()
            database["random"] = nil
            database.endEditingAndSave()
            
            XCTAssert(database.count == 2)
            XCTAssert(database["random"] == nil)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }

    func testAddArray() {
        let exp = expectation(description: "")
        
        let database = type(of: self).makeEmptyDatabase()
        
        database.open { err in
            guard err == nil else { XCTFail(); return }
            
            database.beginEditing()
            let array = KeyValueCollection.ArrayValue()
            array.append(WrappedValue(1))
            array.append(WrappedValue("one"))
            array.append(WrappedValue(true))
            
            database["array"] = .array(array)
            database.endEditingAndSave()
            
            XCTAssert(database.count == 1)
            
            guard let el = database["array"] else { XCTFail(); return }
            guard let a = el.array else { XCTFail(); return }
            let elements = a.elements
            XCTAssert(elements.count == 3)
            XCTAssert(elements[0] == WrappedValue(1))
            XCTAssert(elements[1] == WrappedValue("one"))
            XCTAssert(elements[2] == WrappedValue(true))
            
            exp.fulfill()
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testAddAndModifyArray() {
        let exp = expectation(description: "")
        
        let database = type(of: self).makeEmptyDatabase()
        
        database.open { err in
            guard err == nil else { XCTFail(); return }
            
            let array = KeyValueCollection.ArrayValue([WrappedValue(1), WrappedValue("one")])
            database.beginEditing()
            database["array"] = .array(array)
            database.endEditingAndSave()
            
            XCTAssert(database.count == 1)
            
            database.beginEditing()
            guard let el1 = database["array"]?.array else { XCTFail(); return }
            XCTAssert(el1.elements.count == 2)
            el1.append(WrappedValue(2))
            database.endEditingAndSave()
            
            XCTAssert(database.count == 1)
            
            guard let a = database["array"]?.array else { XCTFail(); return }
            XCTAssert(a.elements.count == 3)
            XCTAssert(a.elements[0] == WrappedValue(1))
            XCTAssert(a.elements[1] == WrappedValue("one"))
            XCTAssert(a.elements[2] == WrappedValue(2))
            
            database.beginEditing()
            guard let el2 = database["array"]?.array else { XCTFail(); return }
            el2.insert(WrappedValue("two"), at: 1)
            database.endEditingAndSave()
            
            guard let a1 = database["array"]?.array else { XCTFail(); return }
            XCTAssert(a1.elements.count == 4)
            XCTAssert(a1.elements[0] == WrappedValue(1))
            XCTAssert(a1.elements[1] == WrappedValue("two"))
            XCTAssert(a1.elements[2] == WrappedValue("one"))
            XCTAssert(a1.elements[3] == WrappedValue(2))
            
            exp.fulfill()
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }

    func testRewriteValueToArray() {
        let exp = expectation(description: "")
        let database = type(of: self).makeEmptyDatabase()
        
        database.open { err in
            guard err == nil else { XCTFail(); return }
            
            database.beginEditing()
            database["el"] = .value(WrappedValue(1))
            database.endEditingAndSave()
            
            XCTAssert(database.count == 1)
            
            database.beginEditing()
            let array = KeyValueCollection.ArrayValue([WrappedValue(1), WrappedValue("one")])
            database["el"] = .array(array)
            database.endEditingAndSave()
            
            XCTAssert(database.count == 1)
            guard database["el"]?.array != nil else { XCTFail(); return }
            
            exp.fulfill()
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testModifyArrayAfterAdding() {
        let exp = expectation(description: "")
        let database = type(of: self).makeEmptyDatabase()
        
        database.open { err in
            guard err == nil else { XCTFail(); return }
            
            let array = KeyValueCollection.ArrayValue([WrappedValue(1), WrappedValue(2)])
            database.beginEditing()
            database["array"] = .array(array)
            database.endEditingAndSave()
            
            guard let aUnmod = database["array"]?.array else { XCTFail(); return }
            XCTAssert(aUnmod.elements.count == 2)
            
            database.beginEditing()
            array.remove(at: 0)
            database.endEditingAndSave()
            
            guard let aMod = database["array"]?.array else { XCTFail(); return }
            XCTAssert(aMod.elements.count == 1)
            XCTAssert(aMod.elements[0] == WrappedValue(2))
            
            exp.fulfill()
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
}
