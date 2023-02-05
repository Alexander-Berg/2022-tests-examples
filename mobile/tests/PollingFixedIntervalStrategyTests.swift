import XCTest
@testable import BeruCore

final class PollingFixedIntervalStrategyTests: XCTestCase {

    func test_shouldCalculateExpectedTimeInterval_onFirstPoll() {
        let expectedInterval: DispatchTimeInterval = .milliseconds(100)
        let nextIntervalStrategy = PollingFixedIntervalStrategy(.milliseconds(100))
        let numberOfRetries = 0

        let nextInterval = nextIntervalStrategy.getNextInterval(numberOfRetries)

        XCTAssertEqual(expectedInterval, nextInterval)
    }

    func test_shouldCalculateExpectedTimeInterval_onSecondPoll() {
        let expectedInterval: DispatchTimeInterval = .milliseconds(100)
        let nextIntervalStrategy = PollingFixedIntervalStrategy(.milliseconds(100))
        let numberOfRetries = 1

        let nextInterval = nextIntervalStrategy.getNextInterval(numberOfRetries)

        XCTAssertEqual(expectedInterval, nextInterval)
    }

    func test_shouldCalculateExpectedTimeInterval_onFifthPoll() {
        let expectedInterval: DispatchTimeInterval = .milliseconds(100)
        let nextIntervalStrategy = PollingFixedIntervalStrategy(.milliseconds(100))
        let numberOfRetries = 4

        let nextInterval = nextIntervalStrategy.getNextInterval(numberOfRetries)

        XCTAssertEqual(expectedInterval, nextInterval)
    }
}
