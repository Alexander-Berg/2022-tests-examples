//
//  ThrottlerTests.swift
//  YRECoreUtils-Unit-Tests
//
//  Created by Alexey Salangin on 26.09.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRECoreUtils

final class ThrottlerTests: XCTestCase {
    private let throttle = Throttler(minimumDelay: 0.1)

    func testThrottler() {
        let queue = DispatchQueue.main

        let times = [0.0, 0.05, 0.08, 0.30, 0.32, 0.35]

        var accepted = [Int]()
        var skipped = [Int]()

        let expectedAccepted = [0, 2, 3, 5]
        let expectedSkipped = [1, 4]

        let expectation = XCTestExpectation()

        times.enumerated().forEach { offset, time in
            func fulfill() {
                if offset == times.indices.last {
                    expectation.fulfill()
                }
            }

            queue.asyncAfter(deadline: .now() + time) {
                self.throttle {
                    accepted.append(offset)
                    fulfill()
                } onSkip: {
                    skipped.append(offset)
                    fulfill()
                }
            }
        }

        self.wait(for: [expectation], timeout: 0.5)

        XCTAssertEqual(accepted, expectedAccepted)
        XCTAssertEqual(skipped, expectedSkipped)
    }
}
