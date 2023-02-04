//
//  TaskRetrierTests.swift
//  YRECoreUtils-Unit-Tests
//
//  Created by Leontyev Saveliy on 14.07.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRECoreUtils

final class TaskRetrierTests: XCTestCase {
    enum TestError: Error {
        case common
    }

    func testSuccessfulRetry() {
        let expectation = XCTestExpectation()

        var shouldFail = true

        let taskRetrier = TaskRetrier<Void, TestError>(
            maxAttemptCount: 2,
            strategy: .immediate,
            shouldRetry: { _ in true },
            retryIteration: { callback in
                if shouldFail {
                    shouldFail = false
                    callback(.failed(.common))
                }
                else {
                    callback(.succeeded(()))
                }
            },
            completion: { result in
                switch result {
                    case .cancelled, .failed:
                        XCTFail("Not expected result \(result)")
                    case .succeeded:
                        expectation.fulfill()
                }
            }
        )
        taskRetrier.run()

        XCTWaiter().wait(for: [expectation], timeout: 10)
    }

    func testRetryMaxCount() {
        let expectation = XCTestExpectation()

        var retryCounter = 0
        let retryMaxCount = 3

        let taskRetrier = TaskRetrier<Void, TestError>(
            maxAttemptCount: retryMaxCount,
            strategy: .immediate,
            shouldRetry: { _ in true },
            retryIteration: { callback in
                retryCounter += 1
                callback(.failed(.common))
            },
            completion: { _ in
                expectation.fulfill()
            }
        )
        taskRetrier.run()

        XCTWaiter().wait(for: [expectation], timeout: 10)
        XCTAssertEqual(retryCounter, retryMaxCount)
    }

    func testRetryCancelation() {
        let expectation = XCTestExpectation()

        let taskRetrier = TaskRetrier<Void, TestError>(
            maxAttemptCount: 1,
            strategy: .immediate,
            shouldRetry: { _ in true },
            retryIteration: { callback in
                callback(.cancelled)
            },
            completion: { result in
                switch result {
                    case .cancelled:
                        expectation.fulfill()
                    case .failed, .succeeded:
                        XCTFail("Not expected result \(result)")
                }
            }
        )
        taskRetrier.run()

        XCTWaiter().wait(for: [expectation], timeout: 10)
    }
}
