import XCTest

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
