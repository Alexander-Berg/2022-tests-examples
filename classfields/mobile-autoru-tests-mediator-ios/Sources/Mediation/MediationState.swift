import Foundation
import Teamcity

public final class MediationState {
    private(set) var runs: [TestRun]

    private let logger: Logger

    private let teamcity: TeamcityClient
    private let stateQueue = DispatchQueue.global()

    public init(runs: [TestRun] = [], logger: Logger, teamcity: TeamcityClient) {
        self.logger = logger
        self.teamcity = teamcity
        self.runs = runs
    }

    private func getRun(for uuid: String) throws -> TestRun {
        guard let run = runs.first(where: { $0.id == uuid }) else { throw Error.noActiveRunsForInitiator }

        return run
    }

    public enum Error: Swift.Error {
        case noActiveRunsForInitiator
        case batchNotFound
        case uncompletedBatch
        case uncompletedBuild
    }
}

extension MediationState {
    /// Начать новый прогон тестов
    public func startTestRun(
        sourceBranch: String,
        sourceBuildID: TeamcityBuildID,
        sourcePullRequestID: Int?,
        strategyType: SplitStrategy.Type,
        tests: [TestCase],
        maxBatchSize: Int,
        maxAgentsCount: Int,
        maxRetriesCount: Int,
        initiatorBuildID: TeamcityBuildID,
        initiatorBuildNumber: TeamcityBuildNumber,
        testsRunPoolConfigID: BuildConfigurationID,
        testsRunResultConfigID: BuildConfigurationID,
        onCompleteRun: (() -> Void)? = nil
    ) {
        let strategy = strategyType.init(maxAvailableAgentsCount: maxAgentsCount, maxBatchSize: maxBatchSize)

        logger.info("Run new mediation for #\(initiatorBuildNumber): \(tests.count) tests on \(maxAgentsCount) agents")

        let testRun = TestRun(
            initiatorBuildID: initiatorBuildID,
            initiatorBuildNumber: initiatorBuildNumber,
            sourceBranch: sourceBranch,
            sourcePullRequest: sourcePullRequestID,
            sourceBuildID: sourceBuildID,
            tests: tests,
            strategy: strategy,
            maxRetriesCount: maxRetriesCount,
            testsRunResultConfigID: testsRunResultConfigID
        )

        logger.info("Waiting for finish build \(initiatorBuildID) (#\(initiatorBuildNumber))")

        let waitInitiatorBuild = DispatchGroup()
        waitInitiatorBuild.enter()

        teamcity.observeBuild(
            id: initiatorBuildID,
            interval: 10.0
        ) { [logger] result in
            if case .failure(let error) = result {
                logger.error("Error while request TeamCity: \(error)")
            }

            guard case .success(let build) = result else { return false }

            guard case .finished = build.state else { return false }

            defer { waitInitiatorBuild.leave() }

            logger.info("Build \(initiatorBuildID) (#\(initiatorBuildNumber)) finished, trigger builds in pool...")

            return true
        }

        waitInitiatorBuild.notify(queue: .global()) { [weak self] in
            guard let self = self else { return }

            defer { onCompleteRun?() }

            let startedBuilds = self.startTestRunPoolBuilds(
                testRunID: testRun.id,
                initiatorBuildNumber: initiatorBuildNumber,
                initiatorBuildID: initiatorBuildID,
                sourceBranch: sourceBranch,
                sourceBuildID: sourceBuildID,
                sourcePullRequestID: sourcePullRequestID,
                maxAgentsCount: maxAgentsCount,
                testsRunPoolConfigID: testsRunPoolConfigID
            )

            testRun.setActiveBuilds(startedBuilds)

            self.stateQueue.sync {
                self.runs.append(testRun)
            }
        }
    }

    private func startTestRunPoolBuilds(
        testRunID: String,
        initiatorBuildNumber: TeamcityBuildNumber,
        initiatorBuildID: TeamcityBuildID,
        sourceBranch: String,
        sourceBuildID: TeamcityBuildID,
        sourcePullRequestID: Int?,
        maxAgentsCount: Int,
        testsRunPoolConfigID: BuildConfigurationID
    ) -> [TeamcityBuildID] {
        let group = DispatchGroup()
        var testRunBuilds: [TeamcityBuildID] = []

        for _ in 0..<maxAgentsCount {
            let comment = "Run tests at #\(initiatorBuildNumber), source run #\(sourceBuildID)"

            group.enter()

            teamcity.triggerBuild(
                configID: testsRunPoolConfigID,
                branch: sourceBranch,
                parameters: [
                    .initiatorBuildID: .int(initiatorBuildID),
                    .initiatorBuildNumber: .string(initiatorBuildNumber),
                    .mediatorRunUUID: .string(testRunID),
                    .mediatorBuildUUID: .string(UUID().uuidString) // рандомный uuid, чтобы тимсити не скипал билды
                ],
                comment: comment
            ) { [logger] result in
                defer { group.leave() }

                if case .failure(let error) = result {
                    logger.error("Error while trigger TeamCity build: \(error)")
                }

                guard case .success(let poolBuild) = result else { return }

                logger.info("Run new build \(poolBuild.id) in pool for initiator #\(initiatorBuildNumber)")

                testRunBuilds.append(poolBuild.id)
            }

            group.wait()

            // Иногда тимсити не триггерит несколько билдов, а каждый раз возвращает один
            sleep(3)
        }

        return testRunBuilds
    }
}

extension MediationState {
    /// Получение следующего батча
    public func getNextBatch(testRunID: String, executorBuildID: TeamcityBuildID) throws -> TestsBatch? {
        logger.info("Request next batch for initiator \(testRunID)")

        return try stateQueue.sync {
            let run = try getRun(for: testRunID)

            guard let batch = run.batches.first(where: { !$0.alreadyExecuting }) else {
                logger.info("No more batches at run \(testRunID)")
                return nil
            }

            do {
                try batch.markAsExecuting(buildID: executorBuildID)
            } catch {
                return nil
            }

            logger.info("Return next batch, id \(batch.id)")

            return batch
        }
    }
}

extension MediationState {
    /// Пометить батч тестов исполненным
    public func completeBatch(testRunID: String, batchID: String) throws {
        logger.info("Complete batch \(batchID) for run \(testRunID)")

        return try stateQueue.sync {
            let run = try getRun(for: testRunID)

            guard let batch = run.batches.first(where: { $0.id == batchID }) else {
                throw Error.batchNotFound
            }

            do {
                try batch.markAsCompleted()
            } catch {
                logger.info("Unable to complete batch \(batchID), error: \(error)")
                throw Error.uncompletedBatch
            }

            logger.info("Batch \(batch.id) completed")
        }
    }
}

extension MediationState {
    /// Пометить билд тестов исполненным
    public func completeBuild(testRunID: String, executorBuildID: TeamcityBuildID) throws {
        logger.info("Complete build \(executorBuildID), initiator \(testRunID)")

        return try stateQueue.sync {
            let run = try getRun(for: testRunID)

            do {
                let wasLast = try run.markBuildAsCompleted(id: executorBuildID)

                logger.info("Build \(executorBuildID) completed\(wasLast ? ". It was the last build": "")")

                if wasLast {
                    DispatchQueue.global().async { [weak self] in
                        self?.waitFinishAndTriggerMergeResults(
                            sourceBranch: run.sourceBranch,
                            sourceBuildID: run.sourceBuildID,
                            sourcePullRequestID: run.sourcePullRequest,
                            builds: Array(run.completedBuildsWithAtLeastOneBatch),
                            testsRunResultConfigID: run.testsRunResultConfigID
                        )
                    }
                }
            } catch {
                logger.info("Unable to complete build \(executorBuildID), error: \(error)")
                throw Error.uncompletedBuild
            }
        }
    }

    private func waitFinishAndTriggerMergeResults(
        sourceBranch: String,
        sourceBuildID: TeamcityBuildID,
        sourcePullRequestID: Int?,
        builds: [TeamcityBuildID],
        testsRunResultConfigID: BuildConfigurationID
    ) {
        logger.info("Observe builds \(builds.map { String($0) }.joined(separator: ", ")) and wait for their finish...")

        var finishedBuilds: [TeamcityBuildID] = []
        let waitFinishedBuildsGroup = DispatchGroup()

        for build in builds {
            waitFinishedBuildsGroup.enter()

            teamcity.observeBuild(id: build, interval: 10) { [logger] result in
                if case .failure(let error) = result {
                    logger.error("Error while request TeamCity: \(error)")
                }

                guard case .success(let build) = result else { return false }
                guard build.state == .finished else { return false }

                defer { waitFinishedBuildsGroup.leave() }

                logger.info("Observed build \(build.id) (#\(build.number ?? "<none>")) has finished")

                finishedBuilds.append(build.id)

                return true
            }
        }

        waitFinishedBuildsGroup.wait()

        logger.info("All observed builds finished. Trigger result merge build")

        return stateQueue.sync {
            let triggerResultsBuildGroup = DispatchGroup()
            triggerResultsBuildGroup.enter()

            let comment = "Results for run #\(sourceBuildID)"

            self.teamcity.triggerBuild(
                configID: testsRunResultConfigID,
                branch: sourceBranch,
                parameters: [
                    .finishedBuildsIDs: .array(finishedBuilds.map { .int($0) }),
                    .sourceBranchName: .string(sourceBranch),
                    .sourceBuildID: .int(sourceBuildID),
                    .sourcePullRequestID: .string(sourcePullRequestID.flatMap { "\($0)" } ?? ""),
                ],
                comment: comment
            ) { [logger] result in
                // TODO: ошибка запроса
                guard case .success(let build) = result else { return }

                defer { triggerResultsBuildGroup.leave() }

                logger.info("Successfully triggered result merge build \(build.id)")
            }

            triggerResultsBuildGroup.wait()
        }
    }
}

extension MediationState {
    /// Вернуть тесты на дополнительный прогон
    public func retryTests(
        testRunID: String,
        tests: [TestCase]
    ) throws {
        logger.info("Retry \(tests.count) tests for initiator \(testRunID)")

        return try stateQueue.sync {
            let run = try getRun(for: testRunID)

            run.retry(tests: tests)
        }
    }
}
