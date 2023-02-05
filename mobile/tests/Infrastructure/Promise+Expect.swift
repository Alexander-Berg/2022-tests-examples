import PromiseKit
import XCTest

enum TestingError: Error {
    case expectationTimeout
}

extension Promise {

    @discardableResult
    func expect(in testCase: XCTestCase, timeout: TimeInterval = 0.3) -> Swift.Result<T, Error> {
        var result: Swift.Result<T, Error> = .failure(TestingError.expectationTimeout)
        let expectation = testCase.expectation(description: "ExpecationOf\(T.self)")

        done {
            result = .success($0)
        }
        .catch {
            result = .failure($0)
        }
        .finally {
            expectation.fulfill()
        }

        testCase.wait(for: [expectation], timeout: timeout)

        return result
    }
}
