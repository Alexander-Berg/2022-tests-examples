//  Created by Denis Malykh on 26.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import XCTest

@testable import YxSwissKnife

class SkEventTests: XCTestCase {

    func testTriggers() {
        let trigger = SkEvent<Int>.create()
        let checker = SktClosureCallChecker(expectedValues: [1,2])
        let token = trigger.event.observe(checker.getClosure())
        trigger.notify(1)
        trigger.notify(2)
        checker.expect()
        token.dispose()
    }

    func testTransform() {
        let trigger = SkEvent<Int>.create()
        let otherEvent = trigger.event.map { "\($0)" }
        let checker = SktClosureCallChecker(expectedValues: ["1", "2"])
        let token = otherEvent.observe(checker.getClosure())
        trigger.notify(1)
        trigger.notify(2)
        checker.expect()
        token.dispose()
    }

    func testFiltering() {
        let trigger = SkEvent<Int>.create()
        let otherEvent = trigger.event.filter { $0 > 1 }
        let checker = SktClosureCallChecker(expectedValues: [2, 3])
        let token = otherEvent.observe(checker.getClosure())
        trigger.notify(1)
        trigger.notify(2)
        trigger.notify(3)
        checker.expect()
        token.dispose()
    }

    func testAdaptation() {
        let trigger = SkEvent<Int>.create()
        let otherEvent = trigger.event.adapt { $0 > 1 ? "\($0)" : nil }
        let checker = SktClosureCallChecker(expectedValues: ["2", "3"])
        let token = otherEvent.observe(checker.getClosure())
        trigger.notify(1)
        trigger.notify(2)
        trigger.notify(3)
        checker.expect()
        token.dispose()
    }
}
