//
//  ThrottlerTest.swift
//  UtilsTests
//
//  Created by Aleksey Makhutin on 18.03.2022.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
@testable import Utils

import Combine

internal final class ThrottlerTest: XCTestCase {
    func testShouldFireInTime() {
        let throttler = Throttler(for: 0.1, queue: .main)
        let expectation = self.expectation(description: "wait for testing throttler")
        let date = Date()

        var lastEventDate = Date()
        throttler.throttle {
            lastEventDate = Date()
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(90)) {
            throttler.throttle {
                lastEventDate = Date()
            }
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(200)) {
            expectation.fulfill()
        }

        waitForExpectations(timeout: 1, handler: nil)

        let lastEventTimePast = lastEventDate.timeIntervalSince(date) * 1000
        XCTAssertGreaterThanOrEqual(lastEventTimePast, 100)
        XCTAssertLessThanOrEqual(lastEventTimePast, 120)
    }

    func testShouldSkipLastEvents() {
        let throttler = Throttler(for: 0.1, queue: .main, skipLastEvent: true)
        let expectation = self.expectation(description: "wait for testing throttler")

        var eventNumber = 0
        throttler.throttle {
            eventNumber = 1
        }

        throttler.throttle {
            eventNumber = 2
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(90)) {
            throttler.throttle {
                eventNumber = 3
            }
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(200)) {
            expectation.fulfill()
        }

        waitForExpectations(timeout: 1, handler: nil)

        XCTAssertEqual(eventNumber, 1)
    }

    func testShouldNotSkipLastEvents() {
        let throttler = Throttler(for: 0.1, queue: .main)
        let expectation = self.expectation(description: "wait for testing throttler")

        var eventNumber = 0
        throttler.throttle {
            eventNumber = 1
        }
        throttler.throttle {
            eventNumber = 2
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(90)) {
            throttler.throttle {
                eventNumber = 3
            }
        }

        DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(200)) {
            expectation.fulfill()
        }

        waitForExpectations(timeout: 1, handler: nil)

        XCTAssertEqual(eventNumber, 3)
    }
}
