//  Created by Denis Malykh on 27.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import XCTest

@testable import YxSwissKnife

class SkMutableValueTests: XCTestCase {

    func testMutableValue() {
        var val = 1
        let value = SkMutableValue(
            getter: { val },
            setter: { val = $0 }
        )
        var observed = 0
        let token = value.observable.observe { update in
            observed = update.new
        }
        XCTAssertEqual(value.value, 1)
        XCTAssertEqual(value.readonly.value, 1)
        value.value = 2
        XCTAssertEqual(val, 2)
        XCTAssertEqual(observed, 2)
        token.dispose()
    }
}
