//  Created by Denis Malykh on 23.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import XCTest

@testable import YxSwissKnife

class SkLazyTests: XCTestCase {

    func testValue() {
        let lazy = SkLazy<Int>(1)
        XCTAssertEqual(lazy.currentValue, 1)
        XCTAssertEqual(lazy.value, 1)
    }

    func testLoading() {
        var closureCalled = 0
        let lazy = SkLazy<Int> {
            closureCalled += 1
            return 1
        }
        XCTAssertNil(lazy.currentValue)
        XCTAssertEqual(lazy.value, 1)
        XCTAssertEqual(lazy.currentValue, 1)
        XCTAssertEqual(lazy.value, 1)
        XCTAssertEqual(closureCalled, 1)
    }
}
