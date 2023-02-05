import PromiseKit
import XCTest

enum TestingError: Error {
    case expectationTimeout
}

class AsyncResult<T> {

    // MARK: - Properties

    var result: T? {
        didSet {
            expectation.fulfill()
        }
    }

    private let expectation: XCTestExpectation
    private let testCase: XCTestCase

    // MARK: - Lifecycle

    init(_ testCase: XCTestCase) {
        self.testCase = testCase
        expectation = testCase.expectation(description: "ExpecationOf\(T.self)")
    }

    // MARK: - Public

    func wait(timeout: TimeInterval) {
        testCase.wait(for: [expectation], timeout: timeout)
    }
}

extension Promise {

    @discardableResult
    func expect(in testCase: XCTestCase, timeout: TimeInterval = 0.3) -> Swift.Result<T, Error> {
        var result: Swift.Result<T, Error> = .failure(TestingError.expectationTimeout)
        let expecation = testCase.expectation(description: "ExpecationOf\(T.self)")

        done {
            result = .success($0)
        }
        .catch {
            result = .failure($0)
        }
        .finally {
            expecation.fulfill()
        }

        testCase.wait(for: [expecation], timeout: timeout)

        return result
    }
}
