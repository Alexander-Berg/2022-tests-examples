//
//  CancelAfterTest.swift
//  UtilsTests
//
//  Created by Aleksey Makhutin on 17.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
@testable import Utils

internal final class CancelAfterTest: XCTestCase {
    func testShouldFireBeforeUntilTimeIsLeft() {
        var isPerformed = false
        let block = CancelAfter(deadline: .now() + .milliseconds(300)) {
            isPerformed = true
        }
        let expectation = self.expectation(description: "wait for testing cancelAfter")

        DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(150)) {
            block()
            expectation.fulfill()
        }

        self.wait(for: [expectation], timeout: 0.5)

        XCTAssertTrue(isPerformed)
    }

    func testShouldNotFireAfterUntilTimeIsLeft() {
        var isPerformed = false
        let block = CancelAfter(deadline: .now() + .milliseconds(150)) {
            isPerformed = true
        }
        let expectation = self.expectation(description: "wait for testing cancelAfter")

        DispatchQueue.main.asyncAfter(deadline: .now() + .milliseconds(300)) {
            block()
            expectation.fulfill()
        }

        self.wait(for: [expectation], timeout: 0.5)

        XCTAssertFalse(isPerformed)
    }
}
