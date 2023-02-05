//
//  DefaultImportantPlacesSnapshotTest.swift
//  YandexGeoToolboxTestApp
//
//  Created by Ilya Lobanov on 06/05/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation
import XCTest


class DefaultImportantPlacesSnapshotTest: XCTestCase {
    
    struct Static {
        static let databaseAdapter = DatabaseAdapterTestImpl(id: "important_places")
        static let database = Database(adapter: databaseAdapter)
        static let ipDatabase: ImportantPlacesDatabase =
            DefaultImportantPlacesDatabase(database: Static.database)
        
        static let address1 = "address1"
        static let shortAddress1 = "shortAddress1"
        static let latitude1 = 2.9
        static let longitude1 = 1.2
        
        static let address2 = "address2"
        static let shortAddress2 = "shortAddress2"
        static let latitude2 = 212.9
        static let longitude2 = 91.11
    }
    
    override func setUp() {
        super.setUp()
        Static.databaseAdapter.reset()
    }
    
    func testThatBeginAndEndEditingChangesEditingField() {
        let exp = expectation(description: "")
        
        Static.ipDatabase.openSnapshot { snapshot, error in
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
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatUpdateOrInsertMethodInsertsNewElement() {
        let exp = expectation(description: "")
        
        Static.ipDatabase.openSnapshot { snapshot, error in
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            XCTAssert(snapshot.importantPlaces.isEmpty)
            
            snapshot.beginEditing()
            snapshot.updateOrInsertImportantPlace(kind: .Home, latitude: Static.latitude1, longitude: Static.longitude1,
                address: Static.address1, shortAddress: Static.shortAddress1)
            snapshot.endEditing()
            snapshot.sync()
            
            XCTAssert(snapshot.importantPlaces.count == 1)
            XCTAssert(snapshot.valid)
            
            Static.ipDatabase.openSnapshot { newSnapshot, error in
                exp.fulfill()
                
                guard let newSnapshot = newSnapshot else {
                    XCTFail()
                    return
                }
                
                guard let home = newSnapshot.importantPlaces[.Home] else {
                    XCTFail()
                    return
                }
                
                XCTAssert(home.kind == .Home)
                XCTAssertEqualWithAccuracy(home.created.timeIntervalSince1970, NSDate().timeIntervalSince1970, accuracy: 1.0)
                XCTAssertEqualWithAccuracy(home.modified.timeIntervalSince1970, NSDate().timeIntervalSince1970, accuracy: 1.0)
                XCTAssert(home.latitude == Static.latitude1)
                XCTAssert(home.longitude == Static.longitude1)
                XCTAssert(home.address == Static.address1)
                XCTAssert(home.shortAddress == Static.shortAddress1)
            }
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatUpdateOrInsertMethodUpdateExistingElement() {
        let exp = expectation(description: "")
        
        Static.ipDatabase.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            XCTAssert(snapshot.importantPlaces.isEmpty)
            
            snapshot.beginEditing()
            snapshot.updateOrInsertImportantPlace(kind: .Work, latitude: Static.latitude1, longitude: Static.longitude1,
                address: Static.address1, shortAddress: Static.shortAddress1)
            snapshot.updateOrInsertImportantPlace(kind: .Work, latitude: Static.latitude2, longitude: Static.longitude2,
                address: Static.address2, shortAddress: Static.shortAddress2)
            snapshot.endEditing()
            
            XCTAssert(snapshot.importantPlaces.count == 1)
            XCTAssert(snapshot.valid)
            
            guard let home = snapshot.importantPlaces[.Work] else {
                XCTFail()
                return
            }
            
            XCTAssert(home.kind == .Work)
            XCTAssertEqualWithAccuracy(home.created.timeIntervalSince1970, NSDate().timeIntervalSince1970, accuracy: 1.0)
            XCTAssertEqualWithAccuracy(home.modified.timeIntervalSince1970, NSDate().timeIntervalSince1970, accuracy: 1.0)
            XCTAssert(home.modified.timeIntervalSince1970 > home.created.timeIntervalSince1970)
            XCTAssert(home.latitude == Static.latitude2)
            XCTAssert(home.longitude == Static.longitude2)
            XCTAssert(home.address == Static.address2)
            XCTAssert(home.shortAddress == Static.shortAddress2)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatUpdateLastUsedMethodChangesOnlyLastUsedProperty() {
        let exp = expectation(description: "")
        
        Static.ipDatabase.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            XCTAssert(snapshot.importantPlaces.isEmpty)
            
            snapshot.beginEditing()
            snapshot.updateOrInsertImportantPlace(kind: .Home, latitude: Static.latitude1, longitude: Static.longitude1,
                address: Static.address1, shortAddress: Static.shortAddress1)
            
            let created = snapshot.importantPlaces[.Home]?.created
            let modified = snapshot.importantPlaces[.Home]?.created
            
            snapshot.useImportantPlace(kind: .Home)
            snapshot.endEditing()
            
            XCTAssert(snapshot.importantPlaces.count == 1)
            XCTAssert(snapshot.valid)
            
            guard let home = snapshot.importantPlaces[.Home] else {
                XCTFail()
                return
            }
            
            XCTAssert(home.kind == .Home)
            XCTAssert(home.created.timeIntervalSince1970 == created?.timeIntervalSince1970)
            XCTAssert(home.modified.timeIntervalSince1970 == modified?.timeIntervalSince1970)
            XCTAssert(home.lastUsed.timeIntervalSince1970 > home.created.timeIntervalSince1970)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatUpdateLastUsedMethodDoesntCreateNewPlace() {
        let exp = expectation(description: "")
        
        Static.ipDatabase.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            XCTAssert(snapshot.importantPlaces.isEmpty)
            
            snapshot.beginEditing()
            snapshot.useImportantPlace(kind: .Home)
            snapshot.endEditing()
            
            XCTAssert(snapshot.importantPlaces.isEmpty)
            XCTAssert(snapshot.valid)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatDeleteMethodDeletesNothingFromEmptySnapshot() {
        let exp = expectation(description: "")
        
        Static.ipDatabase.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            XCTAssert(snapshot.importantPlaces.isEmpty)
            
            snapshot.beginEditing()
            snapshot.deleteImportantPlace(kind: .Home)
            snapshot.endEditing()
            
            XCTAssert(snapshot.importantPlaces.isEmpty)
            XCTAssert(snapshot.valid)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatDeleteMethodDeletesPlaceAfterCreating() {
        let exp = expectation(description: "")
        
        Static.ipDatabase.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            XCTAssert(snapshot.importantPlaces.isEmpty)
            
            snapshot.beginEditing()
            snapshot.updateOrInsertImportantPlace(kind: .Home, latitude: Static.latitude1, longitude: Static.longitude1,
                address: Static.address1, shortAddress: Static.shortAddress1)
            snapshot.updateOrInsertImportantPlace(kind: .Work, latitude: Static.latitude2, longitude: Static.longitude2,
                address: Static.address2, shortAddress: Static.shortAddress2)
            snapshot.deleteImportantPlace(kind: .Home)
            snapshot.endEditing()
            
            XCTAssert(snapshot.importantPlaces.count == 1)
            XCTAssertNotNil(snapshot.importantPlaces[.Work])
            XCTAssert(snapshot.valid)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
    func testThatUpdateOrInsertMethodCreatePlaceAfterDeleting() {
        let exp = expectation(description: "")
        
        Static.ipDatabase.openSnapshot { snapshot, error in
            exp.fulfill()
            
            guard let snapshot = snapshot else {
                XCTFail()
                return
            }
            
            XCTAssert(snapshot.importantPlaces.isEmpty)
            
            snapshot.beginEditing()
            snapshot.updateOrInsertImportantPlace(kind: .Work, latitude: Static.latitude1, longitude: Static.longitude1,
                address: Static.address1, shortAddress: Static.shortAddress1)
            snapshot.deleteImportantPlace(kind: .Work)
            snapshot.updateOrInsertImportantPlace(kind: .Work, latitude: Static.latitude2, longitude: Static.longitude2,
                address: Static.address2, shortAddress: Static.shortAddress2)
            snapshot.endEditing()
            
            XCTAssert(snapshot.importantPlaces.count == 1)
            XCTAssert(snapshot.valid)
            
            guard let home = snapshot.importantPlaces[.Work] else {
                XCTFail()
                return
            }
            
            XCTAssert(home.kind == .Work)
            XCTAssertEqualWithAccuracy(home.created.timeIntervalSince1970, NSDate().timeIntervalSince1970, accuracy: 1.0)
            XCTAssertEqualWithAccuracy(home.modified.timeIntervalSince1970, NSDate().timeIntervalSince1970, accuracy: 1.0)
            XCTAssert(home.modified.timeIntervalSince1970 == home.created.timeIntervalSince1970)
            XCTAssert(home.latitude == Static.latitude2)
            XCTAssert(home.longitude == Static.longitude2)
            XCTAssert(home.address == Static.address2)
            XCTAssert(home.shortAddress == Static.shortAddress2)
        }
        
        waitForExpectations(timeout: 1.0, handler: nil)
    }
    
}
