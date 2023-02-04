@testable import Mediation
import XCTest

final class EqualBatchesStrategyTests: XCTestCase {
    override func setUp() {
        super.setUp()
        continueAfterFailure = false
    }

    func test_emptyTests() {
        let batches = EqualBatchesStrategy(maxAvailableAgentsCount: 5, maxBatchSize: 20)
            .split(tests: [])

        XCTAssertEqual(batches.count, 0)
    }

    func test_allTestsInOneBatch() {
        let batches = EqualBatchesStrategy(maxAvailableAgentsCount: 5, maxBatchSize: 20)
            .split(tests: [TestCase.mock(), TestCase.mock(), TestCase.mock()])

        XCTAssertEqual(batches.count, 1)
        XCTAssertEqual(batches[0].tests.count, 3)
    }

    func test_multipleBatches_lessThanMax() {
        let batches = EqualBatchesStrategy(maxAvailableAgentsCount: 5, maxBatchSize: 20)
            .split(tests: Array(repeating: TestCase.mock(), count: 75))

        XCTAssertEqual(batches.count, 5)
        batches.forEach {
            XCTAssertEqual($0.tests.count, 15)
        }
    }

    func test_multipleBatches_moreThanMax() {
        let batches = EqualBatchesStrategy(maxAvailableAgentsCount: 5, maxBatchSize: 20)
            .split(tests: Array(repeating: TestCase.mock(), count: 750))

        XCTAssertEqual(batches.count, 38)

        batches.last.map {
            XCTAssertEqual($0.tests.count, 10)
        }
        batches.dropLast().forEach {
            XCTAssertEqual($0.tests.count, 20)
        }
    }
}
