//  Created by Denis Malykh on 20.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import XCTest

@testable import YxSwissKnife

class SkValueTests: XCTestCase {

    func testGetter() {
        let clz = Class()
        let valueC = SkValue<Int> {
            clz.a
        }
        XCTAssertEqual(valueC.value, 0)
        clz.a = 1
        XCTAssertEqual(valueC.value, 1)

        var str = Struct()
        let valueS = SkValue<Int> {
            str.a
        }
        XCTAssertEqual(valueS.value, 0)
        str.a = 1
        XCTAssertEqual(valueS.value, 1)
    }

    func testKeyPath() {
        let clz = Class()
        let valueC = SkValue<Int>(clz, keyPath: \.a)
        XCTAssertEqual(valueC.value, 0)
        clz.a = 1
        XCTAssertEqual(valueC.value, 1)

        var str = Struct()
        let valueS = SkValue<Int>(str, keyPath: \.a)
        XCTAssertEqual(valueS.value, 0)
        str.a = 1
        XCTAssertEqual(valueS.value, 0)
    }

    func testConst() {
        var val = 1
        let value = SkValue<Int>.const(val)
        XCTAssertEqual(value.value, 1)
        val = 2
        XCTAssertEqual(value.value, 1)
    }

    func testMap() {
        let clz = Class()
        let valueC = SkValue<Int> { clz.a }
        let valueC2 = valueC.map { "\($0)" }
        XCTAssertEqual(valueC2.value, "0")
        clz.a = 1
        XCTAssertEqual(valueC2.value, "1")

        var str = Struct()
        let valueS = SkValue<Int> { str.a }
        let valueS2 = valueS.map { "\($0)" }
        XCTAssertEqual(valueS2.value, "0")
        str.a = 1
        XCTAssertEqual(valueS2.value, "1")
    }

}

private class Class {
    var a: Int = 0
}

private struct Struct {
    var a: Int = 0
}
