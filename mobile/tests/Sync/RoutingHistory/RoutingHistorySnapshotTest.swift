//
//  RoutingHistorySnapshotTest.swift
//  YandexGeoToolboxTestApp
//
//  Created by Ilya Lobanov on 06/05/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation
import XCTest


class RoutingHistorySnapshotTest: XCTestCase {
    
    struct Static {
        static let databaseAdapter = DatabaseAdapterTestImpl(id: "RoutingHistory")
        static let database = RoutingHistoryDatabase(database: Database(adapter: databaseAdapter))
        
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
            snapshot.addItem(title: "title", description: "description", latitude: 1.1, longitude: 3.1, uri: "uri")
            snapshot.endEditing()
            
            XCTAssert(snapshot.items.count == 1)
            XCTAssert(snapshot.valid)
            XCTAssert(snapshot.items[0].title == "title")
            XCTAssert(snapshot.items[0].description == "description")
            XCTAssert(snapshot.items[0].latitude == 1.1)
            XCTAssert(snapshot.items[0].longitude == 3.1)
            XCTAssert(snapshot.items[0].uri == "uri")
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
            snapshot.addItem(title: "title1", description: "description1", latitude: 1.02, longitude: 2.01, uri: nil)
            snapshot.addItem(title: "title2", description: "description2", latitude: 1.021, longitude: 22.01, uri: nil)
            snapshot.endEditing()
            
            XCTAssert(snapshot.items.count == 2)
            XCTAssert(snapshot.valid)
            let item0 = snapshot.items[0]
            let item1 = snapshot.items[1]
            XCTAssert(item0.title == "title2")
            XCTAssert(item1.title == "title1")
            XCTAssert(item0 > item1)
            XCTAssert(item0.lastUsed.timeIntervalSince1970 > item1.lastUsed.timeIntervalSince1970)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatAddItemMethodUpdatesItemWithTheSamePoint() {
        let exp = expectation(description: "")
        
        Static.database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            snapshot.addItem(title: "title1", description: "description1", latitude: 1.02, longitude: 2.01, uri: nil)
            snapshot.addItem(title: "title2", description: "description2", latitude: 1.02, longitude: 2.01, uri: "uri2")
            snapshot.endEditing()
            
            XCTAssert(snapshot.items.count == 1)
            XCTAssert(snapshot.valid)
            let item0 = snapshot.items[0]
            XCTAssert(item0.title == "title2")
            XCTAssert(item0.description == "description2")
            XCTAssert(item0.latitude == 1.02)
            XCTAssert(item0.longitude == 2.01)
            XCTAssert(item0.uri == "uri2")
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatAddItemMethodUpdatesItemWithTheSamePointOnly() {
        let exp = expectation(description: "")
        
        Static.database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            snapshot.addItem(title: "title1", description: "description1", latitude: 1.01, longitude: 2.01, uri: "uri1")
            snapshot.addItem(title: "title1", description: "description1", latitude: 1.01, longitude: 2567.02, uri: "uri1")
            snapshot.addItem(title: "title1", description: "description1", latitude: 13.03, longitude: 2.01, uri: "uri1")
            snapshot.endEditing()
            
            XCTAssert(snapshot.items.count == 3)
            XCTAssert(snapshot.valid)
            let item0 = snapshot.items[0]
            let item1 = snapshot.items[1]
            let item2 = snapshot.items[2]
            XCTAssert(item0.latitude == 13.03 && item0.longitude == 2.01)
            XCTAssert(item1.latitude == 1.01 && item1.longitude == 2567.02)
            XCTAssert(item2.latitude == 1.01 && item2.longitude == 2.01)
            
            XCTAssert(item0 > item1)
            XCTAssert(item1 > item2)
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
            snapshot.addItem(title: "title1", description: "description1", latitude: 1.01, longitude: 2.01, uri: "uri1")
            snapshot.addItem(title: "title2", description: "description2", latitude: 123.1, longitude: 21.01, uri: "uri2")
            
            let id0 = snapshot.items[0].id
            let id1 = snapshot.items[1].id
            snapshot.deleteItem(id: id0)
            
            snapshot.endEditing()
            
            XCTAssert(snapshot.items.count == 1)
            XCTAssert(snapshot.valid)
            let item0 = snapshot.items[0]
            XCTAssert(item0.latitude == 1.01 && item0.longitude == 2.01)
            XCTAssert(item0.id == id1)
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
    
    func testThatClearMethodRemovesAllItems() {
        let exp = expectation(description: "")
        
        Static.database.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            snapshot.beginEditing()
            snapshot.addItem(title: "title1", description: "description1", latitude: 1.01, longitude: 2.01, uri: "uri1")
            snapshot.addItem(title: "title2", description: "description2", latitude: 13.41, longitude: 12.9, uri: "uri2")
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
                snapshot.addItem(title: "title\(i)", description: "description\(i)", latitude: Double(i),
                    longitude: Double(i), uri: "uri\(i)")
            }
            snapshot.endEditing()
            
            XCTAssert(snapshot.valid)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
}
