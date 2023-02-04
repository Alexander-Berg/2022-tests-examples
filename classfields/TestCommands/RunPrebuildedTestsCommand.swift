import Foundation
import ArgumentParser
import ProcessHelper
import Logging
import XcbeautifyLib
import BuildRunner
import SimulatorManager

private let logger = Logger(label: "RunPrebuildedTestsCommand")

struct RunPrebuildedTestsCommand: ConfigReadingCommand {
    static let configuration = CommandConfiguration(
        commandName: "run-prebuilded-tests",
        abstract: """
        Запускает наборы тестов, полученные от mediator service.
        """
    )

    @Option
    var configPath: String = defaultConfigPath

    @Option
    var mediatorServiceURL: String

    @Option
    var testRunID: String

    var simulatorManager: SimulatorManager {
        SimulatorManager()
    }

    func run(with config: Config) async throws {
        guard let serviceURL = URL(string: mediatorServiceURL) else {
            throw InvalidArgumentError(argumentName: "mediatorServiceURL", value: mediatorServiceURL)
        }

        guard let buildID = config.teamcityBuildID else {
            throw MissingConfigParameterError(configProperty: "teamcityBuildID")
        }

        var batchIndex = 0

        var xcresultURLs: [URL] = []

        if let existingSimulator = try? simulatorManager.getExistingDestination() {
            try simulatorManager.delete(udid: existingSimulator.id)
        }

        while true {
            let batch = try await getTestsBatch(serviceURL: serviceURL, buildID: buildID)

            guard let batch = batch, !batch.tests.isEmpty else {
                logger.info("No more batches planned. Finish execution")

                break
            }

            let urls = try await runBatch(batch, serviceURL: serviceURL, index: batchIndex, config: config)
            xcresultURLs.append(contentsOf: urls)

            batchIndex += 1
        }

        shutSimulatorsDown()

        if batchIndex > 0 {
            try mergeLocalTestsResults(
                baseURL: URL(fileURLWithPath: config.testResultsDirectory),
                xcresultURLs: xcresultURLs,
                config: config
            )
        }

        try await completeTestsBuild(serviceURL: serviceURL, buildID: buildID)
    }

    private func shutSimulatorsDown() {
        logger.info("Shutting simulators down")
        do {

            try simulatorManager.shutdownAll()

            let bootedDevices = try? simulatorManager.availableDevices()
                .flatMap { $0.value }
                .filter { $0.state == .booted }

            logger.info("booted devices: \((bootedDevices ?? []).map(\.udid).joined(separator: ", "))")
        } catch {
            logger.error("error during shutting simulators down: \(error)")
        }
    }

    private func runBatch(_ batch: TestBatch, serviceURL: URL, index: Int, config: BuildUtilsConfig) async throws -> [URL] {

        let prebuildDirectory = URL(fileURLWithPath: config.prebuildArtifactsDirectory)

        logger.info("Run new batch: \(batch.tests.count) tests")

        let testsByTargets = try groupTestsByTarget(batch.tests)

        var results: [URL] = []

        for target in testsByTargets.keys.sorted() {
            let tests = testsByTargets[target]!

            logger.info("running tests for target \(target), tests: \(tests)")

            let xctestrunFile = try findXCTestRunFile(target: target, in: prebuildDirectory)

            let resultURL = URL(fileURLWithPath: config.testResultsDirectory).appendingPathComponent("result_batch_\(index)_\(target)")

            results.append(resultURL.appendingPathExtension("xcresult"))

            let failedTestsURL = URL(fileURLWithPath: config.buildDirectory).appendingPathComponent("failed_tests_\(index)_\(target).txt")

            try startRunner(
                tests: tests,
                xctestrunPath: xctestrunFile.path,
                resultBundlePath: resultURL.path,
                parallelize: batch.parallelize,
                failedTestsOutput: failedTestsURL.path
            )

            if FileManager.default.fileExists(atPath: failedTestsURL.path) {
                let fileContent = try Data(contentsOf: failedTestsURL)
                let failedTests = String(data: fileContent, encoding: .utf8)?.split(separator: "\n").map(String.init) ?? []

                logger.info("Failed tests in batch \(index): \(failedTests.joined(separator: ", "))")

                try await sendTestsForRetry(serviceURL: serviceURL, failedTests: failedTests)
            }
        }

        try await completeTestsBatch(serviceURL: serviceURL, batchID: batch.id)

        return results
    }

    private func startRunner(
        tests: [String],
        xctestrunPath: String,
        resultBundlePath: String,
        parallelize: Bool,
        failedTestsOutput: String
    ) throws {
        let colored = ProcessInfo.processInfo.environment["COLORED"].flatMap { Bool($0) } ?? false

        let parser = XcbeautifyLib.Parser()

        let output: (String) -> Void = { line in
            if let parsedLine = parser.parse(line: line, colored: colored, additionalLines: { nil }) {
                logger.info("\(parsedLine)")
            }
        }

        try BuildRunner.testWithoutBuilding(
            appBundleID: "ru.AutoRu",
            xctestrunPath: xctestrunPath,
            resultBundlePath: resultBundlePath,
            destination: .init(.default),
            parallelize: parallelize,
            tests: tests,
            failedTestsOutputPath: failedTestsOutput,
            output: output
        )
    }

    private func groupTestsByTarget(_ tests: [String]) throws -> [String: [String]] {
        try tests.reduce(into: [:]) { dict, test in
            guard let index = test.firstIndex(of: "/") else {
                throw InvalidTestNameError(name: test)
            }

            let target = String(test[..<index])

            dict[target, default: []].append(test)
        }
    }

    private func sendTestsForRetry(serviceURL: URL, failedTests: [String]) async throws {
        logger.info("Retry failed tests: url \(serviceURL), test run \(testRunID), tests count \(failedTests.count)")

        let requestModel = RetryTestsMediationRequestBody(tests: failedTests)

        var request = URLRequest(url: serviceURL.appendingPathComponent("retry/\(testRunID)"), timeoutInterval: 600)
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpMethod = "POST"
        request.httpBody = try JSONEncoder().encode(requestModel)

        let (responseData, _) = try await URLSession.shared.data(for: request)

        if let responseString = String(data: responseData, encoding: .utf8) {
            logger.info("\(responseString)")
        }

        let responseModel = try JSONDecoder().decode(RetryTestsMediationResponse.self, from: responseData)

        logger.info("Tests mediation retry status: \(responseModel.status)")
    }

    private func completeTestsBatch(serviceURL: URL, batchID: String) async throws {
        logger.info("Complete tests batch: url \(serviceURL), test run \(testRunID), batch \(batchID)")

        let (responseData, _) = try await URLSession.shared.data(
            from: serviceURL.appendingPathComponent("complete/\(testRunID)/batch/\(batchID)")
        )

        if let responseString = String(data: responseData, encoding: .utf8) {
            logger.info("\(responseString)")
        }

        let responseModel = try JSONDecoder().decode(CompleteTestBatchResponse.self, from: responseData)

        logger.info("Complete tests batch status: \(responseModel.status)")
    }

    private func findXCTestRunFile(target: String, in directory: URL) throws -> URL {
        let contents = try FileManager.default.contentsOfDirectory(at: directory, includingPropertiesForKeys: nil)

        logger.info("contents of \(directory): \(contents)")

        let xctestrunFiles = contents.filter { $0.pathExtension == "xctestrun" }
        guard let autoruXCTestRunFile = xctestrunFiles.first(where: { $0.lastPathComponent.hasPrefix("\(target)_") }) else {
            throw XCTestRunNotFoundError()
        }

        return autoruXCTestRunFile
    }

    private func mergeLocalTestsResults(baseURL: URL, xcresultURLs: [URL], config: BuildUtilsConfig) throws {
        try XCResultHelper.mergeXCResults(
            xcresultURLs,
            into: baseURL.appendingPathComponent("merged.xcresult")
        )
    }

    private func completeTestsBuild(serviceURL: URL, buildID: Int) async throws {
        logger.info("Complete tests build: url \(serviceURL), test run \(testRunID), build \(buildID)")

        let (responseData, _) = try await URLSession.shared.data(
            from: serviceURL.appendingPathComponent("complete/\(testRunID)/build/\(buildID)")
        )

        if let responseString = String(data: responseData, encoding: .utf8) {
            logger.info("\(responseString)")
        }

        let responseModel = try JSONDecoder().decode(CompleteTestBuildResponse.self, from: responseData)

        logger.info("Complete tests build status: \(responseModel.status)")
    }

    private func getTestsBatch(serviceURL: URL, buildID: Int) async throws -> TestBatch? {
        logger.info("Request tests batch for execution: url \(serviceURL), test run \(testRunID)")

        var request = URLRequest(url: serviceURL.appendingPathComponent("next/\(testRunID)"), timeoutInterval: 600)
        request.addValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpMethod = "POST"
        let requestBody = NextBatchMediationRequestBody(build_id: buildID)
        request.httpBody = try JSONEncoder().encode(requestBody)

        let (responseData, _) = try await URLSession.shared.data(for: request)

        if let responseString = String(data: responseData, encoding: .utf8) {
            logger.info("\(responseString)")
        }

        let responseModel = try JSONDecoder().decode(NextBatchMediationResponse.self, from: responseData)

        logger.info("Tests mediation request status: \(responseModel.status)")

        return responseModel.batch
    }
}

private struct NextBatchMediationRequestBody: Encodable {
    var build_id: Int
}

private struct NextBatchMediationResponse: Decodable {
    var status: String
    var batch: TestBatch?
}

private struct TestBatch: Decodable {
    struct ExecutionInfo: Decodable {
        var dequeue_time: Double
        var executor: Int
    }

    var parallelize: Bool
    var execution_info: ExecutionInfo
    var id: String
    var tests: [String]
}

private struct XCTestRunNotFoundError: Error {
}

private struct RetryTestsMediationRequestBody: Encodable {
    var tests: [String]
}

private struct RetryTestsMediationResponse: Decodable {
    var status: String
}

private struct CompleteTestBatchResponse: Decodable {
    var status: String
}

private struct CompleteTestBuildResponse: Decodable {
    var status: String
}

struct InvalidTestNameError: Error {
    var name: String
}
