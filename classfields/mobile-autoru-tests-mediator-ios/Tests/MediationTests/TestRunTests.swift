@testable import Mediation
import XCTest

final class TestRunTests: XCTestCase {
    override func setUp() {
        super.setUp()
        continueAfterFailure = false
    }

    func test_setActiveBuilds() {
        let testRun = Self.makeTestRun()

        let build = TeamcityBuildID.mock()

        XCTAssertFalse(testRun.activeBuilds.contains(build))

        testRun.setActiveBuilds([build])

        XCTAssertTrue(testRun.activeBuilds.contains(build))
    }

    func test_completeBuild() {
        let testRun = Self.makeTestRun()

        let build = TeamcityBuildID.mock()

        XCTAssertFalse(testRun.activeBuilds.contains(build))

        testRun.setActiveBuilds([build])
        XCTAssertNoThrow(try testRun.markBuildAsCompleted(id: build))

        XCTAssertFalse(testRun.activeBuilds.contains(build))
        XCTAssertTrue(testRun.completedBuilds.contains(build))
    }

    func test_completeBuild_unknownBuild() {
        let testRun = Self.makeTestRun()

        let build = TeamcityBuildID.mock()

        XCTAssertThrowsError(try testRun.markBuildAsCompleted(id: build))

        XCTAssertFalse(testRun.activeBuilds.contains(build))
        XCTAssertFalse(testRun.completedBuilds.contains(build))
    }

    func test_retryTests() {
        let testRun = Self.makeTestRun()

        let build = TeamcityBuildID.mock()

        XCTAssertEqual(testRun.batches.count, 2)

        XCTAssertNoThrow(try testRun.batches[0].markAsExecuting(buildID: build))
        XCTAssertNoThrow(try testRun.batches[0].markAsCompleted())

        XCTAssertNoThrow(try testRun.batches[1].markAsExecuting(buildID: build))

        // Первый рейтрай – берем в работу
        testRun.retry(tests: testRun.batches[1].tests)

        XCTAssertEqual(testRun.batches.count, 3)
        XCTAssertTrue(testRun.batches[0].isCompleted)
        XCTAssertFalse(testRun.batches[1].isCompleted)
        XCTAssertFalse(testRun.batches[2].isCompleted)

        XCTAssertEqual(testRun.batches[2].tests, testRun.batches[1].tests)

        XCTAssertNoThrow(try testRun.batches[1].markAsCompleted())

        // Второй рейтрай – уже всё
        XCTAssertNoThrow(try testRun.batches[2].markAsExecuting(buildID: build))

        testRun.retry(tests: testRun.batches[2].tests)

        XCTAssertEqual(testRun.batches.count, 3)
    }

    private static func makeTestRun() -> TestRun {
        let tests = [TestCase.mock(), TestCase.mock()]

        let strategy = EqualBatchesStrategy(maxAvailableAgentsCount: 5, maxBatchSize: 1)

        let testRun = TestRun(
            initiatorBuildID: TeamcityBuildID.mock(),
            initiatorBuildNumber: TeamcityBuildNumber.mock(),
            sourceBranch: "branch",
            sourcePullRequest: nil,
            sourceBuildID: TeamcityBuildID.mock(),
            tests: tests,
            strategy: strategy,
            maxRetriesCount: 1,
            testsRunResultConfigID: "testsRunResult"
        )

        return testRun
    }
}
