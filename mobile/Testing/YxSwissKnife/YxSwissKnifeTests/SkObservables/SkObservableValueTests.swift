//  Created by Denis Malykh on 27.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import XCTest

@testable import YxSwissKnife

class SkObservableValueTests: XCTestCase {

    func testObservation() {
        var val = 1
        let observable = SkObservableValue<Int>(
            getter: { val },
            setter: { val = $0 }
        )
        var oldVal = 0
        var newVal = 0
        let token = observable.observe { update in
            oldVal = update.old
            newVal = update.new
        }
        XCTAssertEqual(observable.value, 1)
        observable.value = 2
        XCTAssertEqual(oldVal, 1)
        XCTAssertEqual(newVal, 2)
        token.dispose()
    }

    func testTransformation() {
        var val = 1
        let observable = SkObservableValue<Int>(
            getter: { val },
            setter: { val = $0 }
        )
        let transformed = observable.map { "\($0)" }
        var oldVal = ""
        var newVal = ""
        let token = transformed.observe { update in
            oldVal = update.old
            newVal = update.new
        }
        XCTAssertEqual(transformed.value, "1")
        observable.value = 2
        XCTAssertEqual(oldVal, "1")
        XCTAssertEqual(newVal, "2")
        token.dispose()
    }

    func testDistinct() {
        var val = 1
        let observable = SkObservableValue<Int>(
            getter: { val },
            setter: { val = $0 }
        )
        let distinct = observable.distinct()
        var observed = [Int]()
        let token = distinct.observe { update in
            observed.append(update.new)
        }
        observable.value = 1
        observable.value = 1
        observable.value = 2
        observable.value = 2
        observable.value = 2
        XCTAssertEqual(observed, [2])
        token.dispose()
    }
}
