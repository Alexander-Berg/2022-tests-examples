//
//  DebouncerTest.swift
//  UtilsTests
//
//  Created by Aleksey Makhutin on 23.06.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
@testable import Utils

internal final class DebouncerTest: XCTestCase {
    func testShouldFireOnlyOnce() {
        var counter = 0
        var repeatCount = 5
        let delay: TimeInterval = 0.1
        let debouncer = Debouncer(queue: .main, delay: delay)

        let expectation = self.expectation(description: "wait for testing debounce")

        let timer = Timer.scheduledTimer(withTimeInterval: delay / 2, repeats: true) { timer in
            if repeatCount == 0 {
                DispatchQueue.main.asyncAfter(deadline: .now() + (delay * 2)) {
                    expectation.fulfill()
                }
                timer.invalidate()
                return
            }

            repeatCount -= 1
            debouncer.debounce {
                counter += 1
            }
        }
        timer.fire()

        waitForExpectations(timeout: Double(repeatCount) * (delay / 2) + 1, handler: nil)

        XCTAssertEqual(counter, 1, "Debounce should fire once")
    }

    func testDebounceShouldMultiplyFire() {
        var counter = 0
        var repeatCount = 5
        let testRepeatCount = repeatCount

        let delay: TimeInterval = 0.05
        let debouncer = Debouncer(queue: .main, delay: delay)

        let expectation = self.expectation(description: "wait for testing debounce")

        let timer = Timer.scheduledTimer(withTimeInterval: delay + 0.1, repeats: true) { timer in
            if repeatCount == 0 {
                DispatchQueue.main.asyncAfter(deadline: .now() + delay + 0.1) {
                    expectation.fulfill()
                }
                timer.invalidate()
                return
            }

            repeatCount -= 1
            debouncer.debounce {
                counter += 1
            }
        }
        timer.fire()

        waitForExpectations(timeout: Double(repeatCount) * delay + 1, handler: nil)

        XCTAssertEqual(counter, testRepeatCount, "Debounce should fire repeatCount times")
    }
}
