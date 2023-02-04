@testable import Mediation
import XCTest

final class CompleteBatchTests: XCTestCase {
    override func setUp() {
        super.setUp()
        continueAfterFailure = false
    }

    func test_noTestRun_throwException() {
        let state = MediationState(runs: [], logger: MockLogger(), teamcity: MockTeamcityClient())

        let expectation = XCTestExpectation()

        do {
            try state.completeBatch(
                testRunID: UUID().uuidString,
                batchID: UUID().uuidString
            )
        } catch MediationState.Error.noActiveRunsForInitiator {
            expectation.fulfill()
        } catch {
            XCTFail("Should be no-throw")
        }

        wait(for: [expectation], timeout: 1.0)
    }

    func test_noBatch_throwException() {
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

        let expectation = XCTestExpectation()

        do {
            try state.completeBatch(
                testRunID: runs[0].id,
                batchID: UUID().uuidString
            )
        } catch MediationState.Error.batchNotFound {
            expectation.fulfill()
        } catch {
            XCTFail("Should be no-throw")
        }

        wait(for: [expectation], timeout: 1.0)
    }

    func test_completeBatch() {
        let initiator = TeamcityBuildID.mock()

        let batch = TestsBatch(tests: [])

        let runs: [TestRun] = [
            TestRun(
                initiatorBuildID: initiator,
                initiatorBuildNumber: TeamcityBuildNumber.mock(),
                sourceBranch: "branch",
                sourcePullRequest: nil,
                sourceBuildID: TeamcityBuildID.mock(),
                tests: [],
                strategy: MockStrategy(batches: [batch]),
                maxRetriesCount: 0,
                testsRunResultConfigID: "testsRunResult"
            )
        ]

        let state = MediationState(runs: runs, logger: MockLogger(), teamcity: MockTeamcityClient())

        XCTAssertNoThrow(
            try state.runs[0].batches
                .first(where: { $0.id == batch.id })!
                .markAsExecuting(buildID: TeamcityBuildID.mock())
        )

        do {
            try state.completeBatch(
                testRunID: runs[0].id,
                batchID: batch.id
            )

            let batch = try XCTUnwrap(state.runs[0].batches.first(where: { $0.id == batch.id }))

            let executionInfo = try XCTUnwrap(batch.executionInfo)

            XCTAssertNotNil(executionInfo.dequeueTime)
            XCTAssertNotNil(executionInfo.completeTime)
        } catch {
            XCTFail("Should be no-throw, but error \(error)")
        }
    }
}
