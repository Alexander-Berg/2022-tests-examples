//
//  ValueSetterTests.swift
//  YandexGeoToolboxTestApp
//
//  Created by Alexander Shchavrovskiy on 30.08.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest

class ValueSetterTests: XCTestCase {
    
    class SimpleClass: NSObject {
        var mutableValue: String
        init(val: String) {
            self.mutableValue = val
        }
        convenience override init() {
            self.init(val: "")
        }
    }
    
    func testThatValueSetterSet() {
        let obj = SimpleClass()
        
        let setter = ValueSetter<String>{[unowned obj] val in obj.mutableValue = val }
        
        let value = "NewValue"
        setter.set(value)
        XCTAssertEqual(obj.mutableValue, value)
    }
    
    func testLastSettedValue() {
        let obj = SimpleClass()
        
        let setter = ValueSetter<String>{[unowned obj] val in obj.mutableValue = val }
        
        XCTAssertNil(setter.lastSettedValue)
        
        let value = "NewValue"
        setter.set(value)
        XCTAssertEqual(value, setter.lastSettedValue)
        
        let secondValue = "OneMoreValue"
        setter.lastSettedValue = secondValue
        XCTAssertEqual(setter.lastSettedValue, secondValue)
        XCTAssertEqual(obj.mutableValue, value)
    }
    
    func testInOutValueWithDifferentGetterSetter() {
        let obj = SimpleClass()
        let secondValue = "anotherValue"
        let anotherObj = SimpleClass(val: secondValue)
        
        let inOutValue = InOutValue<String>(
            get: { [unowned anotherObj] in
                return anotherObj.mutableValue},
            set: { [unowned obj] newValue in
                obj.mutableValue = newValue
        })
        let value = "value"
        inOutValue.value = value
        
        XCTAssertNotEqual(obj.mutableValue, anotherObj.mutableValue)
        XCTAssertEqual(value, obj.mutableValue)
    }
    
    func testKVStorageInOutValue() {
        let storage = UserDefaults.standard.makeScoped("testValueSetter")
        
        let key = "someKey"
        let inOutValue = storage.stringInOutValue(key)
        
        let val = "someValue"
        inOutValue.value = val
        
        XCTAssertEqual(storage[key] as? String, val)
    }
}
