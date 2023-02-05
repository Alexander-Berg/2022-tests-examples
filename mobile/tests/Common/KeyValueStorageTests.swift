//
//  KeyValueStorageTests.swift
//  YandexGeoToolboxTestApp
//
//  Created by Alexander Shchavrovskiy on 25.08.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest

class KeyValueStorageTests: XCTestCase {
    
    func testExtensionNSUserDefaults() {
        let def = UserDefaults.standard
        let storage: KeyValueStorage = UserDefaults.standard
        
        def.setValue(true, forKey: "first")
        def.synchronize()
        XCTAssertNotNil(storage["first"])
        XCTAssertEqual(storage["first"] as? Bool, def.value(forKey: "first") as? Bool)
        
        storage["second"] = false
        storage.sync()
        XCTAssertNotNil(storage["second"])
        XCTAssertEqual(storage["second"] as? Bool, def.value(forKey: "second") as? Bool)
    }
    
    func testScopedKeyValueStorage() {
        let scope = "scope"
        let anotherScope = "somAnotherScope"
        let key = "key"
        
        let scopedStorage = UserDefaults.standard.makeScoped(scope)
        let sameScopedStorage = UserDefaults.standard.makeScoped(scope)
        let anotherScopedStorage = UserDefaults.standard.makeScoped(anotherScope)
        let def = UserDefaults.standard
        
        scopedStorage[key] = true
        
        XCTAssertNotNil(scopedStorage[key])
        XCTAssertEqual(scopedStorage[key] as? Bool, sameScopedStorage[key] as? Bool)
        XCTAssertNil(anotherScopedStorage[key])
        XCTAssertNil(def.value(forKey: key))
        
    }
    
    func testThatValueNilWithNewKey() {
        let storage = UserDefaults.standard.makeScoped("KeyValueStorageTests")
        let key = "\(NSDate())"
        
        XCTAssertNil(storage[key])
    }
    
    func testDeleteFromStorage() {
        let storage = UserDefaults.standard.makeScoped("KeyValueStorageTests")
        let key = "someNewKey"
        
        let flag: Bool? = true
        
        storage[key] = flag
        XCTAssertNotNil(storage[key])
        
        storage[key] = nil
        XCTAssertNil(storage[key])
        XCTAssertNotNil(flag)
    }
    
    func testThatSavedNewValueForKey() {
        let storage = UserDefaults.standard.makeScoped("KeyValueStorageTests")
        let key = "someNewKey"
        
        storage[key] = true
        XCTAssertEqual(storage[key] as? Bool, true)
        
        storage[key] = false
        XCTAssertEqual(storage[key] as? Bool, false)
    }
    
    
}
