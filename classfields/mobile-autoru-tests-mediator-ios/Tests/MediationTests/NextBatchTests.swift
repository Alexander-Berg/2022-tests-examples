@testable import Mediation
import XCTest

final class NextBatchTests: XCTestCase {
    override func setUp() {
        super.setUp()
        continueAfterFailure = false
    }

    func test_noTestRun_throwException() {
        let state = MediationState(runs: [], logger: MockLogger(), teamcity: MockTeamcityClient())

        let expectation = XCTestExpectation()

        do {
            _ = try state.getNextBatch(
                testRunID: UUID().uuidString,
                executorBuildID: TeamcityBuildID.mock()
            )
        } catch MediationState.Error.noActiveRunsForInitiator {
            expectation.fulfill()
        } catch {
            XCTFail("Should be no-throw")
        }

        wait(for: [expectation], timeout: 1.0)
    }

    func test_noBatch_returnNil() {
        let initiator = TeamcityBuildID.mock()

        let runs: [TestRun] = [
            TestRun(
                initiatorBuildID: initiator,
                initiatorBuildNumber: TeamcityBuildNumber.mock(),
                sourceBranch: "branch",
                sourcePullRequest: nil,
                sourceBuildID: TeamcityBuildID.mock(),
                tests: [],
                strategy: MockStrategy(batches: []),
                maxRetriesCount: 0,
                testsRunResultConfigID: "testsRunResult"
            )
        ]

        let state = MediationState(runs: runs, logger: MockLogger(), teamcity: MockTeamcityClient())

        do {
            let batch = try state.getNextBatch(
                testRunID: runs[0].id,
                executorBuildID: TeamcityBuildID.mock()
            )

            XCTAssertNil(batch)
        } catch {
            XCTFail("Should be no-throw, but received: \(error)")
        }
    }

    func test_batchExtracting() {
        let batches = [
            TestsBatch(tests: [TestCase.mock()]),
            TestsBatch(tests: [TestCase.mock()])
        ]

        let runs: [TestRun] = [
            TestRun(
                initiatorBuildID: TeamcityBuildID.mock(),
                initiatorBuildNumber: TeamcityBuildNumber.mock(),
                sourceBranch: "branch",
                sourcePullRequest: nil,
                sourceBuildID: TeamcityBuildID.mock(),
                tests: [],
                strategy: MockStrategy(batches: batches),
                maxRetriesCount: 0,
                testsRunResultConfigID: "testsRunResult"
            )
        ]

        let state = MediationState(runs: runs, logger: MockLogger(), teamcity: MockTeamcityClient())

        let validateExecutionInfo: (TestsBatch?, TeamcityBuildID) -> () = { batch, executor in
            XCTAssertNotNil(batch!.executionInfo)
            XCTAssertLessThan(
                Date().timeIntervalSince1970 - batch!.executionInfo!.dequeueTime.timeIntervalSince1970,
                5
            )
            XCTAssertNil(batch!.executionInfo!.completeTime)
            XCTAssertEqual(batch!.executionInfo!.executor, executor)
        }

        do {
            let executor1 = TeamcityBuildID.mock()
            let batch1 = try state.getNextBatch(
                testRunID: runs[0].id,
                executorBuildID: executor1
            )

            let executor2 = TeamcityBuildID.mock()
            let batch2 = try state.getNextBatch(
                testRunID: runs[0].id,
                executorBuildID: executor2
            )

            let executor3 = TeamcityBuildID.mock()
            let batch3 = try state.getNextBatch(
                testRunID: runs[0].id,
                executorBuildID: executor3
            )

            XCTAssertEqual(batch1?.id, batches[0].id)
            validateExecutionInfo(batch1, executor1)

            XCTAssertEqual(batch2?.id, batches[1].id)
            validateExecutionInfo(batch2, executor2)

            XCTAssertNil(batch3)
        } catch {
            XCTFail("Should be no-throw, but received: \(error)")
        }
    }
}
