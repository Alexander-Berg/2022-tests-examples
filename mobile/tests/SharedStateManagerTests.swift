//
//  SharedStateManagerTests.swift
//  SharedStateManagerTests
//
//  Created by Dmitry Trimonov on 24.02.2021.
//

import XCTest
import YandexMapsUtils

class SharedStateManagerTests: XCTestCase {
    
    override func setUp() {
        super.setUp()
        
        sut = TestSharedStateManager<Int>()
        sut.stateChangedHandler = nil
    }

    func testWhenSetStateThenStateChangedHandlerIsCalled() throws {
        var wasCalled: Bool = false
        sut.stateChangedHandler = {
            wasCalled = true
        }
        _ = sut.setState(2)
        XCTAssert(wasCalled == true)
        XCTAssert(sut.currentState! == 2)
    }
    
    func testStackSemantics() {
        let firstKey = sut.setState(2)
        let secondKey = sut.setState(3)
        XCTAssert(sut.currentState! == 3)
        sut.removeState(forKey: secondKey)
        XCTAssert(sut.currentState! == 2)
    }
    
    func testWhenRemoveNotTopStateThenStateChangedHandlerIsNotCalledAntCurrentStateIsNotChanged() {
        var wasCalled: Bool = false
    
        let firstKey = sut.setState(2)
        let secondKey = sut.setState(100)
    
        sut.stateChangedHandler = {
            wasCalled = true
        }
        
        sut.removeState(forKey: firstKey)
        XCTAssert(wasCalled == false)
        XCTAssert(sut.currentState! == 100)
    }
    
    func testWhenRemoveTopStateThenStateChangedHandlerIsCalledAntCurrentStateIsChanged() {
        var wasCalled: Bool = false
        
        let firstKey = sut.setState(2)
        let secondKey = sut.setState(100)
        
        sut.stateChangedHandler = {
            wasCalled = true
        }
        
        sut.removeState(forKey: secondKey)
        XCTAssert(wasCalled == true)
        XCTAssert(sut.currentState! == 2)
    }
    
    private var sut: TestSharedStateManager<Int>!

}

class TestSharedStateManager<TResource>: BaseSharedStateManager<TResource> {
    var stateChangedHandler: (() -> Void)?
    
    override func onCurrentStateChanged() {
        super.onCurrentStateChanged()
        
        stateChangedHandler?()
    }
}
