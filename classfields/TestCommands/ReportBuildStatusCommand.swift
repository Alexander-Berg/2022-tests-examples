import Foundation
import ArgumentParser
import Logging

private let logger = Logger(label: "ReportBuildStatusCommand")

struct ReportBuildStatusCommand: ConfigReadingCommand {
    static let configuration = CommandConfiguration(
        commandName: "report-build-status",
        abstract: """
        Уведомляет mediator service о завершении выполнения тестов и шлет в телеграм краткий отчет.
        """
    )

    @Option
    var configPath: String = defaultConfigPath

    @Option(name: .customLong("build-ids"))
    var buildIDs: String

    @Option
    var branch: String

    @Option
    var buildID: String

    func run(with config: Config) async throws {
        guard let teamcityToken = config.teamcityToken else {
            throw MissingConfigParameterError(configProperty: "teamcityToken")
        }

        do {
            try await run(with: config, teamcityToken: teamcityToken)
        } catch {
            let teamcityHelper = TeamcityHelper(teamcityURL: config.teamcityURL, token: teamcityToken)
            try await teamcityHelper.log(message: "##teamcity[buildProblem description='Tests result merge failed']", buildID: buildID)
            try await finishAgentlessBuild(config: config, teamcityHelper: teamcityHelper)
            throw error
        }
    }

    private func run(with config: Config, teamcityToken: String) async throws {
        guard let teamcityBuildID = config.teamcityBuildID else {
            throw MissingConfigParameterError(configProperty: "teamcityBuildID")
        }

        let testsFile = URL(fileURLWithPath: config.allureReportDirectory).appendingPathComponent("data/suites.json")

        guard FileManager.default.fileExists(atPath: testsFile.path) else {
            throw MissingFileError(path: testsFile.path)
        }

        let statistics = try getTestsStatistics(testsFile: testsFile)

        let buildIDs = try buildIDs.split(separator: ",").map(String.init).map { string -> Int in
            guard let int = Int(string) else {
                throw InvalidArgumentError(argumentName: "buildIDs", value: self.buildIDs)
            }

            return int
        }
        let buildDuration = try await getBuildDuration(teamcityToken: teamcityToken, buildIDs: buildIDs, teamcityURL: config.teamcityURL)

        let text = messageText(
            statistics: statistics,
            buildDuration: buildDuration,
            workersCount: buildIDs.count,
            buildNumber: config.buildNumber,
            buildID: teamcityBuildID,
            teamcityURL: config.teamcityURL
        )

        if let telegramToken = config.telegramToken, !config.disableNotifications {
            try await sendMessage(text, telegramToken: telegramToken)
        } else {
            logger.info("message for telegram: \(text)")
        }

        let teamcityHelper = TeamcityHelper(teamcityURL: config.teamcityURL, token: teamcityToken)

        try await logAgentlessBuild(statistics, teamcityBuildID: teamcityBuildID, teamcityHelper: teamcityHelper)
        try await finishAgentlessBuild(config: config, teamcityHelper: teamcityHelper)
    }

    private func logAgentlessBuild(_ statistics: TestsStatistics, teamcityBuildID: Int, teamcityHelper: TeamcityHelper) async throws {
        logger.trace("logAgentlessBuild")

        let messageForTeamcity: String

        if !statistics.failedTests.isEmpty {
            let description = "\(statistics.failedTests.count) tests failed, \(statistics.totalCount) passed. See build \(teamcityBuildID)"
            messageForTeamcity = "##teamcity[buildProblem description='\(description)']"
        } else {
            let description = "\(statistics.totalCount) tests passed"
            messageForTeamcity = "##teamcity[buildStatus status='SUCCESS' text='\(description)']"
        }

        try await teamcityHelper.log(message: messageForTeamcity, buildID: buildID)
    }

    private func finishAgentlessBuild(config: BuildUtilsConfig, teamcityHelper: TeamcityHelper) async throws {
        logger.trace("finishAgentlessBuild")

        try await teamcityHelper.finish(buildID: buildID)
    }

    private func getTestsStatistics(testsFile: URL) throws -> TestsStatistics {
        let fileData = try Data(contentsOf: testsFile)

        let timeline = try JSONDecoder().decode(AllureTimeline.self, from: fileData)

        var failedTests: [String] = []
        var totalCount = 0
        var duration: Double = 0

        for target in timeline.children {
            for suite in target.children {
                for test in suite.children {
                    if test.status == "failed" {

                        let isFlaky = test.flaky ?? false
                        let prefix = isFlaky ? "💣 ": ""

                        failedTests.append(prefix + [target.name, suite.name, test.name].joined(separator: "/"))
                    }

                    totalCount += 1
                    duration += test.time.duration
                }
            }
        }

        return TestsStatistics(
            failedTests: failedTests,
            totalCount: totalCount,
            duration: duration / 1000
        )
    }

    private func messageText(
        statistics: TestsStatistics,
        buildDuration: TimeInterval,
        workersCount: Int,
        buildNumber: String,
        buildID: Int,
        teamcityURL: URL
    ) -> String {
        let branchName: String
        if branch.contains("trunk") {
            branchName = "Ветка: trunk"
        } else if let range = branch.range(of: "(?<=users/robot-stark/autoru/ios/)\\d+", options: .regularExpression) {
            let prNumber = branch[range]
            branchName = "<a href=\"https://a.yandex-team.ru/review/\(prNumber)\">PR #\(prNumber)</a>"
        } else {
            branchName = "Ветка: \(branch)"
        }

        var report = """
        Прогон #\(buildNumber). \(branchName)
        Время: \(formatDuration(buildDuration)) (\(formatDuration(statistics.duration)), агентов: \(workersCount))
        Тестов упало: <b>\(statistics.failedTests.count)</b>, всего: \(statistics.totalCount)

        """

        if !statistics.failedTests.isEmpty {
            let firstTests = statistics.failedTests.prefix(10)
            let firstCountString: String

            if firstTests.count != statistics.failedTests.count {
                firstCountString = " (первые \(firstTests.count))"
            } else {
                firstCountString = ""
            }

            report += "<b>Упавшие тесты\(firstCountString):</b>\n"
            report += firstTests.joined(separator: "\n")
        }

        let url = teamcityURL.appendingPathComponent(
            "repository/download/VerticalMobile_AutoRU_MobileAutoruClientIos_SpmRunTestsResult/\(buildID):id/allure/index.html"
        )

        report += "\n<a href=\"\(url)\">Отчёт</a>"

        return report
    }

    private func sendMessage(_ text: String, telegramToken: String) async throws {
        var components = URLComponents(string: "https://api.telegram.org/bot\(telegramToken)/sendMessage")!

        var queryItems = components.queryItems ?? []
        queryItems.append(URLQueryItem(name: "text", value: text))
        queryItems.append(URLQueryItem(name: "chat_id", value: "-1001457641593"))
        queryItems.append(URLQueryItem(name: "parse_mode", value: "HTML"))
        queryItems.append(URLQueryItem(name: "disable_notification", value: "false"))

        components.queryItems = queryItems

        let url = components.url!

        let (responseData, _) = try await URLSession.shared.data(from: url)

        let responseString = String(data: responseData, encoding: .utf8) ?? ""

        logger.info("Post to telegram result: \(responseString)")
    }

    private func getBuildDuration(teamcityToken: String, buildIDs: [Int], teamcityURL: URL) async throws -> TimeInterval {
        let teamcityHelper = TeamcityHelper(teamcityURL: teamcityURL, token: teamcityToken)

        let buildInfos = try await withThrowingTaskGroup(of: (Int, BuildInfo).self) { group -> [Int: BuildInfo] in
            for buildID in buildIDs {
                group.addTask {
                    (buildID, try await teamcityHelper.buildInfo(teamcityBuildID: buildID))
                }
            }

            return try await group.reduce(into: [:], { dict, result in
                dict[result.0] = result.1
            })
        }

        let buildDuration: TimeInterval
        if let minStartDate = buildInfos.values.compactMap(\.startDate).min(),
           let maxFinishDate = buildInfos.values.compactMap(\.finishDate).max()
        {
            buildDuration = maxFinishDate.timeIntervalSinceReferenceDate - minStartDate.timeIntervalSinceReferenceDate
        } else {
            buildDuration = 0
        }

        return buildDuration
    }

    private func formatDuration(_ timeInterval: TimeInterval) -> String {
        let formatter = DateComponentsFormatter()
        formatter.calendar = Calendar(identifier: .gregorian)
        formatter.calendar?.locale = Locale(identifier: "ru-RU")
        formatter.unitsStyle = .short
        formatter.maximumUnitCount = 2
        return formatter.string(from: timeInterval) ?? ""
    }
}

struct TestsStatistics {
    var failedTests: [String]
    var totalCount: Int
    var duration: TimeInterval
}

private struct AllureTimeline: Decodable {
    struct Target: Decodable {
        var name: String
        var children: [Suite]
    }

    struct Suite: Decodable {
        var name: String
        var children: [Test]
    }

    struct Test: Decodable {
        var name: String
        var status: String
        var flaky: Bool?
        var time: Time
    }

    struct Time: Decodable {
        var duration: Double
    }

    var children: [Target]
}
