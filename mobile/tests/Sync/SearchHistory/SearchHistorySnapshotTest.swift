//
//  SearchHistorySnapshotTest.swift
//  YandexGeoToolboxTestApp
//
//  Created by Ilya Lobanov on 06/05/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation
import XCTest


class SearchHistorySnapshotTest: XCTestCase {
    
    struct Static {
        static let databaseAdapter = DatabaseAdapterTestImpl(id: "SearchHistory")
        static let database = SearchHistoryDatabase(database: Database(adapter: databaseAdapter))
        
    }
    
    func testThatBeginAndEndEditingChangesEditingField() {
        let exp = expectation(description: "")
        
        Static.database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            XCTAssert(!snapshot.editing)
            snapshot.beginEditing()
            XCTAssert(snapshot.editing)
            snapshot.endEditing()
            XCTAssert(!snapshot.editing)
            XCTAssert(snapshot.valid)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatAddItemMethodAddsNewItemToEmptySnapshot() {
        let exp = expectation(description: "")
        
        Static.database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            XCTAssert(snapshot.items.isEmpty)
            
            snapshot.beginEditing()
            snapshot.addItem(searchText: "1", displayText: "2", uri: "3")
            snapshot.endEditing()
            
            XCTAssert(snapshot.items.count == 1)
            XCTAssert(snapshot.valid)
            XCTAssert(snapshot.items[0].searchText == "1")
            XCTAssert(snapshot.items[0].displayText == "2")
            XCTAssert(snapshot.items[0].uri == "3")
            XCTAssertEqualWithAccuracy(snapshot.items[0].lastUsed.timeIntervalSince1970, NSDate().timeIntervalSince1970,
                accuracy: 1.0)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatAddItemMethodAddsNewItemWithCorrectOrder() {
        let exp = expectation(description: "")
        
        Static.database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            snapshot.addItem(searchText: "1_1", displayText: "1_2", uri: nil)
            snapshot.addItem(searchText: "2_1", displayText: "2_2", uri: nil)
            snapshot.endEditing()
            
            XCTAssert(snapshot.items.count == 2)
            XCTAssert(snapshot.valid)
            let item0 = snapshot.items[0]
            let item1 = snapshot.items[1]
            XCTAssert(item0.searchText == "2_1" && item0.displayText == "2_2")
            XCTAssert(item1.searchText == "1_1" && item1.displayText == "1_2")
            XCTAssert(item0 > item1)
            XCTAssert(item0.lastUsed.timeIntervalSince1970 > item1.lastUsed.timeIntervalSince1970)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatAddItemMethodUpdatesItemWithTheSameSearchAndDisplayText() {
        let exp = expectation(description: "")
        
        Static.database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            snapshot.addItem(searchText: "1_1", displayText: "1_2", uri: nil)
            snapshot.addItem(searchText: "2_1", displayText: "2_2", uri: nil)
            snapshot.addItem(searchText: "1_1", displayText: "1_2", uri: nil)
            snapshot.endEditing()
            
            XCTAssert(snapshot.items.count == 2)
            XCTAssert(snapshot.valid)
            let item0 = snapshot.items[0]
            let item1 = snapshot.items[1]
            XCTAssert(item0.searchText == "1_1" && item0.displayText == "1_2")
            XCTAssert(item1.searchText == "2_1" && item1.displayText == "2_2")
            XCTAssert(item0 > item1)
            XCTAssert(item0.lastUsed.timeIntervalSince1970 > item1.lastUsed.timeIntervalSince1970)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatAddItemMethodUpdatesItemWithTheSameSearchAndDisplayTextOnly() {
        let exp = expectation(description: "")
        
        Static.database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            snapshot.addItem(searchText: "1_1", displayText: "1_2", uri: nil)
            snapshot.addItem(searchText: "1_1", displayText: "2_2", uri: nil)
            snapshot.addItem(searchText: "2_1", displayText: "1_2", uri: nil)
            snapshot.endEditing()
            
            XCTAssert(snapshot.items.count == 2)
            XCTAssert(snapshot.valid)
            let item0 = snapshot.items[0]
            let item1 = snapshot.items[1]
        
            XCTAssert(item0.searchText == "2_1" && item0.displayText == "1_2")
            XCTAssert(item1.searchText == "1_1" && item1.displayText == "2_2")
            XCTAssert(item0 > item1)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatAddItemMethodUpdatesItemWithTheSameSearchAndDisplayTextAndDifferentURI() {
        let exp = expectation(description: "")
        
        Static.database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            snapshot.addItem(searchText: "1_1", displayText: "1_2", uri: "uri1")
            snapshot.addItem(searchText: "1_1", displayText: "1_2", uri: "uri2")
            snapshot.endEditing()
            
            XCTAssert(snapshot.items.count == 1)
            XCTAssert(snapshot.valid)
            
            let item0 = snapshot.items[0]
            XCTAssert(item0.searchText == "1_1" && item0.displayText == "1_2")
            XCTAssert(item0.uri == "uri2")
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatClearMethodDoesNothingWithEmptyHistory() {
        let exp = expectation(description: "")
        
        Static.database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            XCTAssert(snapshot.items.isEmpty)
           
            snapshot.beginEditing()
            snapshot.clear()
            snapshot.endEditing()
            
            XCTAssert(snapshot.items.isEmpty)
            XCTAssert(snapshot.valid)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatDeleteItemMethodDeletesItemWithId() {
        let exp = expectation(description: "")
        
        Static.database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            snapshot.addItem(searchText: "1_1", displayText: "1_2", uri: nil)
            snapshot.addItem(searchText: "2_1", displayText: "2_2", uri: nil)
            
            let id0 = snapshot.items[0].id
            let id1 = snapshot.items[1].id
            snapshot.deleteItem(id: id0)
            
            snapshot.endEditing()
            
            XCTAssert(snapshot.items.count == 1)
            XCTAssert(snapshot.valid)
            let item0 = snapshot.items[0]
            XCTAssert(item0.searchText == "1_1")
            XCTAssert(item0.displayText == "1_2")
            XCTAssert(item0.id == id1)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatClearMethodRemovesAllItems() {
        let exp = expectation(description: "")
        
        Static.database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            snapshot.addItem(searchText: "1_1", displayText: "1_2", uri: nil)
            snapshot.addItem(searchText: "2_1", displayText: "2_2", uri: nil)
            snapshot.clear()
            snapshot.endEditing()
            
            XCTAssert(snapshot.items.isEmpty)
            XCTAssert(snapshot.valid)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testAddingLargeNumberOfItems() {
        let exp = expectation(description: "")
        
        Static.database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            
            for i in 0..<3000 {
                snapshot.addItem(searchText: "\(i)", displayText: "\(i)", uri: nil)
            }
            snapshot.endEditing()
            
            XCTAssert(snapshot.valid)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
}
