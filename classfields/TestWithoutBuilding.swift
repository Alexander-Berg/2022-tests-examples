//
//  TestWithoutBuilding.swift
//  AutoRuTestsRunner
//
//  Created by Vladislav Kiryukhin on 12.12.2021.
//

import Foundation
import XcrunWrapper
import SimulatorManager
import Logging

private let logger = Logger(label: "BuildRunner")

public enum BuildRunner {
    @discardableResult
    public static func testWithoutBuilding(
        appBundleID: String,
        xctestrunPath: String,
        resultBundlePath: String,
        destination: BuildDestination,
        parallelize: Bool = false,
        tests: [String],
        failedTestsOutputPath: String? = nil,
        output: ((String) -> Void)? = nil
    ) throws -> Artifacts {
        let simulatorManager = SimulatorManager()

        let device = try prepareSimulatorForTests(
            simulatorManager: simulatorManager,
            destination: destination,
            appBundleID: appBundleID
        )

        let options: [XcodebuildOptions] = [
            .xctestrun(xctestrunPath),
            .destination([.iOSSimulator(udid: device.udid)]),
            .parallelTestingEnabled(parallelize),
            .onlyTesting(tests),
            .resultBundlePath(resultBundlePath)
        ]

        let semaphore = DispatchSemaphore(value: 0)

        var failedTests: [String] = []
        var buildLogLines: [String] = []

        let stdout: (Data) throws -> Void = { data in
            let line = data.string.fromXcode

            let extractedTest = parallelize
                ? tryToExtractFailedTestParallelized(from: line, allTests: tests)
                : tryToExtractFailedTest(from: line)

            if let failedTest = extractedTest {
                failedTests.append(failedTest)

                try writeFailedTestToFileIfNeeded(failedTest, failedTestsOutputPath: failedTestsOutputPath)
            }

            buildLogLines.append(line)

            output?(line)
        }

        let stderr: (Data) throws -> Void = { data in
            let line = data.string.fromXcode

            buildLogLines.append(line)

            output?(line)
        }

        let xcrun = Xcrun(
            ["xcodebuild", XcodebuildAction.testWithoutBuilding.cliOption] + options.flatMap(\.cliOptions),
            options: .init(developerDir: nil, ignoreOutput: false)
        )

        let result = Xcrun.AsyncResult(stdout: stdout, stderr: stderr) { status in
            defer { semaphore.signal() }

            if status != EXIT_SUCCESS {
                logger.error("xcodebuild run failed, exit code = \(status)")
                logger.error("xcodebuild run failed, error = \(buildLogLines.last?.fromXcode ?? "<none>")")
            }
        }
        try xcrun.async(result)

        semaphore.wait()

        return Artifacts.test(
            .init(failedTests: failedTests, rawBuildLog: buildLogLines, resultBundlePath: resultBundlePath)
        )
    }

    private static func writeFailedTestToFileIfNeeded(
        _ testName: String,
        failedTestsOutputPath: String?
    ) throws {
        guard let filePath = failedTestsOutputPath else { return }

        let fileManager = FileManager()

        if !fileManager.fileExists(atPath: filePath) {
            fileManager.createFile(atPath: filePath, contents: nil, attributes: [:])
        }

        guard let fd = FileHandle(forWritingAtPath: filePath) else { return }

        try fd.seekToEnd()
        try fd.write(contentsOf: (testName + "\n").data(using: .utf8) ?? Data())
        try fd.close()
    }

    private static func tryToExtractFailedTest(from line: String) -> String? {
        guard let regexp = try? NSRegularExpression(
            pattern: #"\s*.+:\d+:\serror:\s[\+\-]\[(.*)\]\s:(?:\s'.*'\s\[FAILED\],)?\s(.*)"#
        ) else { return nil }

        let range = NSRange(location: 0, length: line.utf16.count)

        let matches = regexp.matches(in: line, options: [], range: range)
        guard let result = matches.map({ (line as NSString).substring(with: $0.range(at: 1))}).first else {
            return nil
        }

        let normalizedTestName = result
            .replacingOccurrences(of: " ", with: ".")
            .replacingOccurrences(of: ".", with: "/")

        return normalizedTestName
    }

    private static func tryToExtractFailedTestParallelized(from line: String, allTests: [String]) -> String? {
        guard let regexp = try? NSRegularExpression(pattern: ".*'(.*)\\(.*' failed on .*") else { return nil }

        let range = NSRange(location: 0, length: line.utf16.count)

        let matches = regexp.matches(in: line, options: [], range: range)
        guard let result = matches.map({ (line as NSString).substring(with: $0.range(at: 1))}).first else {
            return nil
        }

        let normalizedTestName = result.replacingOccurrences(of: ".", with: "/")

        return allTests.first(where: { $0.hasSuffix(normalizedTestName) })
    }

    private static func prepareSimulatorForTests(
        simulatorManager: SimulatorManager,
        destination: BuildDestination,
        appBundleID: String
    ) throws -> Device {
        logger.info("Create new simulator: \(destination.name) (\(destination.majorOSVersion))")

        let udid = try simulatorManager.getOrCreateDestination(
            DestinationParameters(
                platform: "iOS",
                majorVersion: destination.majorOSVersion,
                name: destination.name,
                simulatorType: destination.simulatorType
            )
        ).id

        try simulatorManager.boot(udid: udid)

        logger.info("Waiting for booting device \(udid)")
        Timer.scheduledTimer(withTimeInterval: 2.0, repeats: true) { timer in
            do {
                guard let device = try simulatorManager.device(udid: udid), device.state == .booted else { return }

                logger.info("Device \(udid) booted")
            } catch {
                logger.error("Error while boot simulator: \(error.localizedDescription)")
            }
        }

        guard let info = try simulatorManager.device(udid: udid), info.state == .booted else {
            throw TestWithoutBuildingError.unableToBootSimulator
        }

        try initiallySetupSimulator(simulatorManager: simulatorManager, device: info, appBundleID: appBundleID)

        return info
    }

    private static func initiallySetupSimulator(
        simulatorManager: SimulatorManager,
        device: Device,
        appBundleID: String
    ) throws {
        logger.info("Add privacy grants for \(appBundleID) in \(device.udid)")

        try simulatorManager.updatePrivacy(action: .grant, device: device.udid, service: .all, appBundleID: appBundleID)

        let disableContinuousPath: (String) -> Void = { dataPath in
            let fullPath = dataPath + "/Library/Preferences/com.apple.keyboard.ContinuousPath.plist"

            let plist = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>KeyboardContinuousPathEnabled</key>
                <false/>
            </dict>
            </plist>
            """

            logger.info("Disable keyboard continuous path, plist = \(fullPath)")

            FileManager().createFile(atPath: fullPath, contents: plist.data(using: .utf8), attributes: [:])
        }

        disableContinuousPath(device.dataPath)
    }

    enum TestWithoutBuildingError: Error {
        case unableToBootSimulator
        case unableToShutdownSimulator
    }
}
