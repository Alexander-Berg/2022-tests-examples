//
//  NotifierTests.swift
//  YandexGeoToolboxTestApp
//
//  Created by Alexander Shchavrovskiy on 29.08.16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import XCTest

class NotifierTests: XCTestCase {
    
    class SimpleClass: NSObject {
        let id: String
        init(id: String) {
            self.id = id
        }
        convenience override init() {
            self.init(id: "")
        }
    }
    
//    func testNotifierCalls() {
//        let not = Notifier<Int>()
//        
//        let intArray = [Int](1...5)
//        
//        intArray.forEach { not.addListener($0) }
//        
//        var testIntArray: [Int] = []
//        not.notify { testIntArray.append($0) }
//        
//        XCTAssertEqual(testIntArray, intArray)
//    }
//    
//    func testRemovingListeners() {
//        let not = Notifier<Int>()
//        
//        let intArray = [Int](1...5)
//        intArray.forEach{ not.addListener($0) }
//        not.removeListener(intArray[intArray.count - 1])
//        
//        let shortIntArray = [Int](1...4)
//        
//        var testIntArray: [Int] = []
//        not.notify{ testIntArray.append($0) }
//        
//        XCTAssertEqual(testIntArray, shortIntArray)
//        
//    }
    
    func testThatListenerCantBeAddedSeveralTimes() {
        let not = Notifier<SimpleClass>()
        
        let obj = SimpleClass()
        let array = [obj,obj]
        array.forEach{ not.addListener($0) }
        
        var testArray: [SimpleClass] = []
        not.notify{ testArray.append($0) }
        
        XCTAssertEqual(testArray, [obj])
    }
    
    func testThatNotifier() {
        let not = Notifier<SimpleClass>()
        var obj: SimpleClass? = SimpleClass()
        
        not.addListener(obj!)
        
        var testString: String? = nil
        
        obj = nil
        not.notify{ testString = $0.id }
        
        XCTAssertNil(testString)
        
    }
    
}
