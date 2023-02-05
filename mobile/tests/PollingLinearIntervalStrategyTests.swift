import XCTest
@testable import BeruCore

final class PollingLinearIntervalStrategyTests: XCTestCase {

    func test_shouldCalculateExpectedTimeInterval_onFirstPoll() {
        let expectedInterval: DispatchTimeInterval = .milliseconds(100)
        let nextIntervalStrategy = PollingLinearIntervalStrategy(start: 100, increment: 200)
        let numberOfRetries = 0

        let nextInterval = nextIntervalStrategy.getNextInterval(numberOfRetries)

        XCTAssertEqual(expectedInterval, nextInterval)
    }

    func test_shouldCalculateExpectedTimeInterval_onSecondPoll() {
        let expectedInterval: DispatchTimeInterval = .milliseconds(300)
        let nextIntervalStrategy = PollingLinearIntervalStrategy(start: 100, increment: 200)
        let numberOfRetries = 1

        let nextInterval = nextIntervalStrategy.getNextInterval(numberOfRetries)

        XCTAssertEqual(expectedInterval, nextInterval)
    }

    func test_shouldCalculateExpectedTimeInterval_onFifthPoll() {
        let expectedInterval: DispatchTimeInterval = .milliseconds(900)
        let nextIntervalStrategy = PollingLinearIntervalStrategy(start: 100, increment: 200)
        let numberOfRetries = 4

        let nextInterval = nextIntervalStrategy.getNextInterval(numberOfRetries)

        XCTAssertEqual(expectedInterval, nextInterval)
    }
}
