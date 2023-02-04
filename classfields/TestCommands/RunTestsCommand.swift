import Foundation
import ArgumentParser
import ProcessHelper
import Logging
import test_discover

private let logger = Logger(label: "RunTestsCommand")

struct RunTestsCommand: ConfigReadingCommand {
    static let configuration = CommandConfiguration(
        commandName: "run-tests",
        abstract: """
        Формирует список тестов для запуска и отправляет его в mediator service.
        """
    )

    @Flag(name: .customLong("unit"))
    var runUnitTests: Bool = false

    @Flag(name: .customLong("ui"))
    var runUITests: Bool = false

    @Flag(name: .customLong("changed"))
    var onlyChanged: Bool = false

    @Option
    var configPath: String = defaultConfigPath

    @Option
    var maxAgentCount = 1

    @Option
    var batchSize = 35

    @Option
    var mediatorServiceURL: String

    @Option
    var maxRetriesCount = 3

    @Option
    var initiatorBuildNumber: String

    @Option
    var initiatorBuildID: Int

    @Option
    var prID = ""

    func run(with config: Config) async throws {
        let tests = try getTests(config)

        logger.info("tests: \(tests)")

        if tests.isEmpty {
            logger.info("tests is empty")
            print("0")
            return
        }

        try await runTestsMediation(config, tests: tests)

        print(tests.count)
    }

    private func runTestsMediation(_ config: BuildUtilsConfig, tests: [String]) async throws {
        logger.info("runTestsMediation")

        guard let branch = config.branch else {
            throw MissingConfigParameterError(configProperty: "branch")
        }

        guard let buildID = config.teamcityBuildID else {
            throw MissingConfigParameterError(configProperty: "teamcityBuildID")
        }

        guard let url = URL(string: mediatorServiceURL) else {
            throw InvalidArgumentError(argumentName: "mediatorServiceURL", value: mediatorServiceURL)
        }

        logger.info("Request tests mediation: url \(mediatorServiceURL), build_id \(buildID), tests count \(tests.count)")

        let requestBody = StartMediationRequestBody(
            sourceBranch: branch,
            sourceBuildId: buildID,
            sourcePullRequestId: Int(prID),
            strategy: "autoRuV1",
            maxAgentsCount: maxAgentCount,
            maxBatchSize: batchSize,
            maxRetriesCount: maxRetriesCount,
            tests: tests,
            initiatorBuildNumber: initiatorBuildNumber,
            initiatorBuildId: initiatorBuildID,
            testsRunPoolConfigID: config.testsRunPoolConfigID,
            testsRunResultConfigID: config.testsRunResultConfigID
        )

        var request = URLRequest(url: url.appendingPathComponent("start"), timeoutInterval: 600)
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")

        request.httpMethod = "POST"
        request.httpBody = try JSONEncoder().encode(requestBody)

        let (responseData, _) = try await URLSession.shared.data(for: request)

        if let responseString = String(data: responseData, encoding: .utf8) {
            logger.info("Tests mediation response: \(responseString)")
        }

        let responseModel = try JSONDecoder().decode(StartMediationResponse.self, from: responseData)

        logger.info("Tests mediation request status: \(responseModel.status)")

        if responseModel.status != "MEDIATION_RUN_OK" {
            throw NotSuccessStatusError(status: responseModel.status)
        }
    }

    private func getTests(_ config: BuildUtilsConfig) throws -> [String] {
        var tests: [String] = []

        if runUnitTests {
            tests.append(
                contentsOf: TestDiscover.getTests(
                    in: config.projectRootURL.appendingPathComponent("Tests/Tests/Sources"),
                    suiteName: "Tests"
                )
            )

            let packageTargetsJson = try Process.run(
                "/usr/bin/swift",
                args: ["run", "TestTargetSearch"],
                currentDirectoryURL: config.projectRootURL.appendingPathComponent("PackageUtils")
            )

            let packageTargets = try JSONDecoder().decode([PackageTestTargets].self, from: Data(packageTargetsJson.utf8))

            for testTarget in packageTargets.flatMap(\.testTargets) {
                let targetTests = TestDiscover.getTests(in: testTarget.sources.map(URL.init(fileURLWithPath:)), suiteName: testTarget.name)
                tests.append(contentsOf: targetTests)
            }
        }

        if runUITests {
            var onlyInFiles: [URL]? = nil

            if onlyChanged {
                guard let branch = config.branch else {
                    throw MissingConfigParameterError(configProperty: "branch")
                }

                let baseBranch = "trunk"

                try Process.runShell("arc pull \(baseBranch)", currentDirectory: config.projectRootURL)
                try Process.runShell("arc fetch \(baseBranch)", currentDirectory: config.projectRootURL)

                let parentCommit = try Process.runShell("arc merge-base \(baseBranch) \(branch)", currentDirectory: config.projectRootURL)

                let tempDir = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString, isDirectory: true)

                defer {
                    try? FileManager.default.removeItem(at: tempDir)
                }

                try FileManager.default.createDirectory(at: tempDir, withIntermediateDirectories: false)
                try Process.runShell("arc init --bare", currentDirectory: tempDir)
                let uiTestsFullPath = "classifieds/mobile-autoru-client-ios/Tests/UITests"
                try Process.runShell(
                    "arc export \(parentCommit) \(uiTestsFullPath) --to .",
                    currentDirectory: tempDir
                )

                let uiTestsDir = config.projectRootURL.appendingPathComponent("Tests/UITests")

                if FileManager.default.fileExists(atPath: uiTestsDir.appendingPathComponent(".git").path) {
                    try FileManager.default.removeItem(at: uiTestsDir.appendingPathComponent(".git"))
                }

                try Process.runShell("git init", currentDirectory: uiTestsDir)
                try Process.runShell("git add -A", currentDirectory: uiTestsDir)
                try Process.runShell("git commit -m initial", currentDirectory: uiTestsDir)

                let tempUITestsDir = tempDir.appendingPathComponent(uiTestsFullPath)

                Thread.sleep(forTimeInterval: 1)

                try FileManager.default.moveItem(
                    at: uiTestsDir.appendingPathComponent(".git"),
                    to: tempUITestsDir.appendingPathComponent(".git")
                )

                let changedFiles = try Process.runShell("git diff --name-only", currentDirectory: tempUITestsDir)
                    .split(separator: "\n")

                onlyInFiles = changedFiles.map { file in
                    uiTestsDir.appendingPathComponent(String(file))
                }
            }

            tests.append(
                contentsOf: TestDiscover.getTests(
                    in: config.projectRootURL.appendingPathComponent("Tests/UITests/Sources"),
                    suiteName: "UITests",
                    onlyInFiles: onlyInFiles
                )
            )
        }

        tests.sort()

        logger.info("Found tests: \(tests.joined(separator: "\n\t"))\n")

        return tests
    }
}

private struct StartMediationRequestBody: Encodable {
    var sourceBranch: String
    var sourceBuildId: Int
    var sourcePullRequestId: Int?
    var strategy: String
    var maxAgentsCount: Int
    var maxBatchSize: Int
    var maxRetriesCount: Int
    var tests: [String]
    var initiatorBuildNumber: String
    var initiatorBuildId: Int
    var testsRunPoolConfigID: String
    var testsRunResultConfigID: String
}

private struct StartMediationResponse: Decodable {
    var status: String
}

private struct NotSuccessStatusError: Error {
    var status: String
}
