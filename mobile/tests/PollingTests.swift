import PromiseKit
import XCTest
@testable import BeruCore

final class PollingTests: XCTestCase {

    func test_shouldReachExpectedValue() {
        let expectedValue = 3
        let counter = Counter()

        let exp = expectation(description: "polling")

        firstly { () -> Promise<Counter> in
            startPolling(
                task: self.incrementValue(in: counter),
                checkResult: { counter in
                    .success(expectedValue == counter.value ? .done : .retry)
                },
                options: PollingOptions(
                    nextIntervalStrategy: PollingFixedIntervalStrategy(.milliseconds(100)),
                    cancellationToken: PollingCancellationToken()
                )
            )
        }.catch { error in
            XCTFail("Test encountered unexpected error: \(error)")
        }.finally {
            exp.fulfill()
        }

        waitForExpectations(timeout: 3)

        XCTAssertEqual(expectedValue, counter.value)
    }

    func test_shouldCheckResultExpectedNumberOfTimes() {
        let expectedChecks = 3
        var checksCounter = 0

        let targetValue = 3
        let counter = Counter()

        let exp = expectation(description: "polling")

        firstly { () -> Promise<Counter> in
            startPolling(
                task: self.incrementValue(in: counter),
                checkResult: { counter in
                    checksCounter += 1
                    return .success(targetValue == counter.value ? .done : .retry)
                },
                options: PollingOptions(
                    nextIntervalStrategy: PollingFixedIntervalStrategy(.milliseconds(100)),
                    cancellationToken: PollingCancellationToken()
                )
            )
        }.catch { error in
            XCTFail("Test encountered unexpected error: \(error)")
        }.finally {
            exp.fulfill()
        }

        waitForExpectations(timeout: 3)

        XCTAssertEqual(expectedChecks, checksCounter)
    }

    func test_shouldCalculateExpectedTimeInterval_withLinearNextIntervalStrategy() {
        let expectedTimeInterval: DispatchTimeInterval = .milliseconds(400)

        let targetValue = 3
        let counter = Counter()

        let nextIntervalStrategy = PollingLinearIntervalStrategy(start: 100, increment: 100)

        let exp = expectation(description: "polling")

        firstly { () -> Promise<Counter> in
            startPolling(
                task: self.incrementValue(in: counter),
                checkResult: { counter in
                    .success(targetValue == counter.value ? .done : .retry)
                },
                options: PollingOptions(
                    nextIntervalStrategy: nextIntervalStrategy,
                    cancellationToken: PollingCancellationToken()
                )
            )
        }.catch { error in
            XCTFail("Test encountered unexpected error: \(error)")
        }.finally {
            exp.fulfill()
        }

        waitForExpectations(timeout: 3)

        XCTAssertEqual(
            expectedTimeInterval,
            nextIntervalStrategy.getNextInterval(counter.value)
        )
    }

    func test_shouldRecieveCancelledError_whenPollingCancelled() {
        let targetValue: Int = .max
        let counter = Counter()

        let expectedError = PollingError.cancelled
        var reachedError: Error?

        let cancellationToken = PollingCancellationToken()
        after(.milliseconds(300)).done { cancellationToken.cancel() }

        let exp = expectation(description: "polling")

        firstly { () -> Promise<Counter> in
            startPolling(
                task: self.incrementValue(in: counter),
                checkResult: { counter in
                    .success(targetValue == counter.value ? .done : .retry)
                },
                options: PollingOptions(
                    nextIntervalStrategy: PollingFixedIntervalStrategy(.milliseconds(100)),
                    cancellationToken: cancellationToken
                )
            )
        }.get { _ in
            XCTFail("Should be unreachable")
        }.catch { error in
            reachedError = error
        }.finally {
            exp.fulfill()
        }

        waitForExpectations(timeout: 3)

        XCTAssertEqual(
            expectedError,
            reachedError.map { $0 as? PollingError }
        )
    }

    func test_shouldRecieveMaximumRetriesCountError_whenRetriesProvided() {
        let retries = 10

        let targetValue: Int = .max
        let counter = Counter()

        let expectedError = PollingError.maximumRetriesCount
        var reachedError: Error?

        let exp = expectation(description: "polling")

        firstly { () -> Promise<Counter> in
            startPolling(
                task: self.incrementValue(in: counter),
                checkResult: { counter in
                    .success(targetValue == counter.value ? .done : .retry)
                },
                options: PollingOptions(
                    retries: retries,
                    nextIntervalStrategy: PollingFixedIntervalStrategy(.milliseconds(100)),
                    cancellationToken: PollingCancellationToken()
                )
            )
        }.get { _ in
            XCTFail("Should be unreachable")
        }.catch { error in
            reachedError = error
        }.finally {
            exp.fulfill()
        }

        waitForExpectations(timeout: 3)

        XCTAssertEqual(
            expectedError,
            reachedError.map { $0 as? PollingError }
        )
    }

    func test_shouldRecieveTimeoutError_whenTimeoutProvided() {
        let timeout: TimeInterval = 0.3

        let targetValue: Int = .max
        let counter = Counter()

        let expectedError = PollingError.timeout
        var reachedError: Error?

        let exp = expectation(description: "polling")

        firstly { () -> Promise<Counter> in
            startPolling(
                task: self.incrementValue(in: counter),
                checkResult: { counter in
                    .success(targetValue == counter.value ? .done : .retry)
                },
                options: PollingOptions(
                    nextIntervalStrategy: PollingFixedIntervalStrategy(.milliseconds(100)),
                    timeout: timeout,
                    cancellationToken: PollingCancellationToken()
                )
            )
        }.get { _ in
            XCTFail("Should be unreachable")
        }.catch { error in
            reachedError = error
        }.finally {
            exp.fulfill()
        }

        waitForExpectations(timeout: 3)

        XCTAssertEqual(
            expectedError,
            reachedError.map { $0 as? PollingError }
        )
    }

    func test_shouldRecieveTaskError_whenTaskThrows() {
        let expectedError = TaskError.something
        var reachedError: Error?

        let exp = expectation(description: "polling")

        firstly { () -> Promise<Void> in
            startPolling(
                task: self.throwError(),
                checkResult: { _ in
                    .success(.retry)
                },
                options: PollingOptions(
                    nextIntervalStrategy: PollingFixedIntervalStrategy(.milliseconds(100)),
                    cancellationToken: PollingCancellationToken()
                )
            )
        }.done {
            XCTFail("Can't be successfull")
        }.catch { error in
            reachedError = error
        }.finally {
            exp.fulfill()
        }

        waitForExpectations(timeout: 3)

        XCTAssertEqual(
            expectedError,
            reachedError.map { $0 as? TaskError }
        )
    }

    func test_shouldRecieveTaskError_whenCheckResultClosureReturnFailure() {
        let targetValue: Int = .max
        let counter = Counter()

        let expectedError = TaskError.something
        var reachedError: Error?

        let exp = expectation(description: "polling")

        firstly { () -> Promise<Counter> in
            startPolling(
                task: self.incrementValue(in: counter),
                checkResult: { counter in
                    targetValue == counter.value ? .success(.done) : .failure(TaskError.something)
                },
                options: PollingOptions(
                    nextIntervalStrategy: PollingFixedIntervalStrategy(.milliseconds(100)),
                    cancellationToken: PollingCancellationToken()
                )
            )
        }.get { _ in
            XCTFail("Should be unreachable")
        }.catch { error in
            reachedError = error
        }.finally {
            exp.fulfill()
        }

        waitForExpectations(timeout: 3)

        XCTAssertEqual(
            expectedError,
            reachedError.map { $0 as? TaskError }
        )
    }
}

// MARK: - Private

private extension PollingTests {

    func incrementValue(in counter: Counter) -> Promise<Counter> {
        firstly {
            after(.milliseconds(100))
        }.map { _ -> Counter in
            counter.value += 1
            return counter
        }
    }

    func throwError() -> Promise<Void> {
        .init(error: TaskError.something)
    }
}

// MARK: - Nested Types

private extension PollingTests {

    final class Counter {
        var value = 0
    }

    enum TaskError: Error {
        case something
    }
}
