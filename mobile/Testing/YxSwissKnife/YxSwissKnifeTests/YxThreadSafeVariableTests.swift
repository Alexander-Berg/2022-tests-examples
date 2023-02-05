//
//  YxThreadSafeVariableTests.swift
//  YxSwissKnifeTests
//
//  Created by Denis Malykh on 23.08.2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

import Foundation
import XCTest

@testable import YxSwissKnife

final class YxThreadSafeVariableTests : XCTestCase {

    func testInitialization() {
        let disp = YxFakeDispatcher()
        let variable = YxThreadSafeVariable<Int>(inner: 10, queue: disp)
        XCTAssertEqual(variable.value, 10)
    }

    func testConcurrentRead() {
        let disp = YxFakeDispatcher()
        let variable = YxThreadSafeVariable<Int>(inner: 10, queue: disp)
        XCTAssertEqual(variable.value, 10)
        XCTAssertEqual(variable.value, 10)
    }

    // NOTE: with executeFirst we simulate barrier write
    func testConcurrentWrite() {
        let disp = YxFakeDispatcher()
        let variable = YxThreadSafeVariable<Int>(inner: 10, queue: disp)
        XCTAssertEqual(variable.value, 10)

        variable.value = 20
        XCTAssertEqual(variable.value, 10)
        disp.executeAsync()
        XCTAssertEqual(variable.value, 20)

        variable.value = 30
        variable.value = 40
        XCTAssertEqual(variable.value, 20)
        disp.executeFirst()
        XCTAssertEqual(variable.value, 30)
        disp.executeFirst()
        XCTAssertEqual(variable.value, 40)
    }

}
