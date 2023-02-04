import Foundation
import ArgumentParser
import ProcessHelper
import Logging

private let logger = Logger(label: "MergeTestResultsCommand")

struct MergeTestResultsCommand: ConfigReadingCommand {
    static let configuration = CommandConfiguration(
        commandName: "merge-test-results",
        abstract: """
        Объединяет отчеты xcresult с нескольких агентов в один.
        """
    )

    @Option
    var configPath: String = defaultConfigPath

    @Option(name: .customLong("build-ids"))
    var buildIDs: String

    @Option
    var useHistoryData: Bool

    @Option
    var allurePath: String

    func run(with config: Config) async throws {
        guard let teamcityToken = config.teamcityToken else {
            throw MissingConfigParameterError(configProperty: "teamcityToken")
        }

        let buildIDs = self.buildIDs.split(separator: ",")

        guard !buildIDs.isEmpty else {
            throw InvalidArgumentError(argumentName: "buildIDs", value: buildIDs)
        }

        let artifactsDirectory = URL(fileURLWithPath: config.artifactsDirectory)

        let xcresultFiles = try await withThrowingTaskGroup(of: URL.self, body: { group -> [URL] in
            for buildID in buildIDs {
                group.addTask {
                    try await fetchXCResults(
                        for: buildID,
                        teamcityToken: teamcityToken,
                        artifactsDirectory: artifactsDirectory,
                        teamcityURL: config.teamcityURL
                    )
                }
            }

            return try await group.reduce(into: []) { array, url in
                array.append(url)
            }
        })

        let resultFile = artifactsDirectory.appendingPathComponent("summary.xcresult")
        try XCResultHelper.mergeXCResults(xcresultFiles, into: resultFile)

        let allureResultsDirectory = artifactsDirectory.appendingPathComponent("allure-results")

        try Process.run(
            URL(fileURLWithPath: config.projectRoot).appendingPathComponent("buildscript/utils/allure-xcresult").path,
            args: [
                "--input", resultFile.path,
                "--output", allureResultsDirectory.path,
                "--overwrite"
            ]
        )

        if useHistoryData {
            try await fetchAllureHistory(allureResultsDirectory.appendingPathComponent("history"))
        }

        let allureDir = URL(fileURLWithPath: config.allureReportDirectory)

        let allureBinary = try findAllureBinary(URL(fileURLWithPath: allurePath))

        try Process.run(allureBinary.path, args: [
            "generate",
            "-c",
            "-o", allureDir.path,
            allureResultsDirectory.path
        ])

        if useHistoryData {
            let allureHistoryZipURL = artifactsDirectory.appendingPathComponent("allure-history.zip")

            try Process.run("/usr/bin/zip", args: [
                "-j",
                "-r", allureHistoryZipURL.path,
                allureDir.appendingPathComponent("history").path
            ])

            try S3Helper.uploadFileToS3(
                allureHistoryZipURL,
                key: "autoru-mobile/ios-tests/allure-history.zip",
                config: config
            )
        }
    }

    private func findAllureBinary(_ baseURL: URL) throws -> URL {
        let dirName = baseURL.lastPathComponent

        guard let hyphenIndex = dirName.firstIndex(of: "-") else {
            throw UnableToFindAllureError()
        }

        let version = dirName[dirName.index(after: hyphenIndex)...]

        let expectedBinaryURL = baseURL.appendingPathComponent("allure-\(version)/bin/allure")

        guard FileManager.default.fileExists(atPath: expectedBinaryURL.path) else {
            throw UnableToFindAllureError()
        }

        return expectedBinaryURL
    }

    private func fetchAllureHistory(_ destination: URL) async throws {
        let (downloadedFileURL, _) = try await URLSession.shared.download(
            from: URL(string: "https://s3.mds.yandex.net/vertis-frontend/autoru-mobile/ios-tests/allure-history.zip")!
        )

        try Process.run("/usr/bin/unzip", args: [
            downloadedFileURL.path,
            "-d", destination.path
        ])
    }

    private func fetchXCResults(for buildID: Substring, teamcityToken: String, artifactsDirectory: URL, teamcityURL: URL) async throws -> URL {
        let url = teamcityURL.appendingPathComponent("repository/download/VerticalMobile_AutoRU_MobileAutoruClientIos_SpmRunTestsPool/\(buildID):id/result.xcresult.tar.gz")

        logger.info("Load artifacts for build \(buildID): url = \(url)")

        var request = URLRequest(url: url)
        request.addValue("Bearer \(teamcityToken)", forHTTPHeaderField: "Authorization")

        let (downloadedFileURL, _) = try await URLSession.shared.download(for: request)

        let xcresultURL = artifactsDirectory.appendingPathComponent("\(buildID).xcresult")
        try FileManager.default.createDirectory(at: xcresultURL, withIntermediateDirectories: false)

        try Process.run("/usr/bin/tar", args: ["-xf", downloadedFileURL.path, "-C", xcresultURL.path])
        return xcresultURL
    }
}

private struct UnableToFindAllureError: Error {
}
