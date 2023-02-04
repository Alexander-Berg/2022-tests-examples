@testable import Mediation
import XCTest

final class StartTestRunTests: XCTestCase {
    override func setUp() {
        super.setUp()
        continueAfterFailure = false
    }

    func test_successRunBuilds() {
        let teamcity = MockTeamcityClient()
        let logger = MockLogger()

        let initiatorID = TeamcityBuildID.mock()
        let initiatorBuild = TeamcityBuildNumber.mock()

        let branchName = "branch_name"

        let expectation = XCTestExpectation()

        let state = MediationState(logger: logger, teamcity: teamcity)

        DispatchQueue.global().asyncAfter(deadline: .now() + 1.0) {
            print("tc => Finish build \(initiatorID)")
            teamcity.finishedBuilds.insert(initiatorID)
        }

        state.startTestRun(
            sourceBranch: branchName,
            sourceBuildID: TeamcityBuildID.mock(),
            sourcePullRequestID: nil,
            strategyType: MockStrategy.self,
            tests: Array(repeating: TestCase.mock(), count: 3),
            maxBatchSize: 20,
            maxAgentsCount: 5,
            maxRetriesCount: 1,
            initiatorBuildID: initiatorID,
            initiatorBuildNumber: initiatorBuild,
            testsRunPoolConfigID: "testsRunPool",
            testsRunResultConfigID: "testsRunResult"
        ) {
            defer { expectation.fulfill() }

            XCTAssertNotNil(state.runs.first)
            XCTAssertEqual(state.runs[0].sourceBranch, branchName)
            XCTAssertEqual(state.runs[0].maxRetriesCount, 1)
            XCTAssertEqual(state.runs[0].activeBuilds.count, 5)
            XCTAssertEqual(state.runs[0].testsRunResultConfigID, "testsRunResult")

            XCTAssertEqual(teamcity.buildsTriggered, 5)
        }

        XCTAssertNil(state.runs.first)

        wait(for: [expectation], timeout: 20.0)
    }
}
