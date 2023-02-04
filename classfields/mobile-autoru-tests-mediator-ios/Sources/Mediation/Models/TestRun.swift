import Foundation

public typealias TeamcityBuildID = Int
public typealias TeamcityBuildNumber = String
public typealias BuildConfigurationID = String

/// Описания одного прогона тестов
public final class TestRun {
    let id: String
    let initiatorBuildID: TeamcityBuildID
    let initiatorBuildNumber: TeamcityBuildNumber
    let sourceBranch: String
    let sourcePullRequest: Int?
    let sourceBuildID: TeamcityBuildID

    let testsRunResultConfigID: BuildConfigurationID

    private(set) var batches: [TestsBatch]
    let strategy: SplitStrategy
    let maxRetriesCount: Int

    private var resultsMergeBuildID: TeamcityBuildID?

    private(set) var activeBuilds: Set<TeamcityBuildID>
    private(set) var completedBuilds: Set<TeamcityBuildID>
    private(set) var failedBuilds: Set<TeamcityBuildID>

    private var retriesCount: [TestCase: Int]

    var completedBuildsWithAtLeastOneBatch: Set<TeamcityBuildID> {
        let batchesExecutors = Set(batches.compactMap(\.executionInfo?.executor))
        return completedBuilds.intersection(batchesExecutors)
    }

    init(
        initiatorBuildID: TeamcityBuildID,
        initiatorBuildNumber: TeamcityBuildNumber,
        sourceBranch: String,
        sourcePullRequest: Int?,
        sourceBuildID: TeamcityBuildID,
        tests: [TestCase],
        strategy: SplitStrategy,
        maxRetriesCount: Int,
        testsRunResultConfigID: BuildConfigurationID
    ) {
        self.id = UUID().uuidString
        self.initiatorBuildID = initiatorBuildID
        self.initiatorBuildNumber = initiatorBuildNumber
        self.sourceBranch = sourceBranch
        self.sourcePullRequest = sourcePullRequest
        self.sourceBuildID = sourceBuildID
        self.batches = strategy.split(tests: tests)
        self.strategy = strategy
        self.maxRetriesCount = maxRetriesCount

        self.activeBuilds = .init()
        self.completedBuilds = .init()
        self.failedBuilds = .init()

        self.retriesCount = [:]
        for test in tests {
            self.retriesCount[test] = 0
        }

        self.testsRunResultConfigID = testsRunResultConfigID
    }

    func setActiveBuilds(_ builds: [TeamcityBuildID]) {
        activeBuilds = Set(builds)
    }

    func markBuildAsCompleted(id: TeamcityBuildID) throws -> Bool {
        guard activeBuilds.contains(id) else { throw Error.inconsistentExecution }

        activeBuilds.remove(id)
        completedBuilds.insert(id)

        if activeBuilds.isEmpty {
            return true
        }

        return false
    }

    func setResultsMerge(buildID: TeamcityBuildID) {
        resultsMergeBuildID = buildID
    }

    func retry(tests: [TestCase]) {
        var filteredTests: [TestCase] = []

        for test in tests {
            retriesCount[test, default: 0] += 1

            if retriesCount[test, default: 0] <= maxRetriesCount {
                filteredTests.append(test)
            }
        }

        var currentBatches: [TestsBatch] = []
        var waitingTests: [TestCase] = []

        for batch in batches {
            if batch.alreadyExecuting {
                currentBatches.append(batch)
            } else {
                waitingTests.append(contentsOf: batch.tests)
            }
        }

        waitingTests.append(contentsOf: filteredTests)

        let newBatches = strategy.split(tests: waitingTests)
        batches = currentBatches + newBatches
    }

    enum Error: Swift.Error {
        case inconsistentExecution
    }
}
