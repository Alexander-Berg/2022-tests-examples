import Foundation

/// Описание одной задачи на прогон тестов
public final class TestsBatch {
    public let id: String
    public let tests: [TestCase]
    private(set) public var executionInfo: ExecutionInfo?
    public let parallelize: Bool

    var alreadyExecuting: Bool { executionInfo != nil }

    var isCompleted: Bool { executionInfo?.completeTime != nil }

    init(tests: [TestCase], parallelize: Bool = true) {
        self.id = UUID().uuidString
        self.tests = tests
        self.parallelize = parallelize
    }

    func markAsExecuting(buildID: TeamcityBuildID) throws {
        if alreadyExecuting { throw Error.inconsistentExecution }

        executionInfo = ExecutionInfo(
            executor: buildID,
            dequeueTime: Date(),
            completeTime: nil
        )
    }

    func markAsCompleted() throws {
        if !alreadyExecuting { throw Error.inconsistentExecution }

        executionInfo?.completeTime = Date()
    }

    func resetExecutionInfo() {
        executionInfo = nil
    }

    enum Error: Swift.Error {
        case inconsistentExecution
    }
}

extension TestsBatch {
    public struct ExecutionInfo {
        public let executor: TeamcityBuildID
        public let dequeueTime: Date
        public var completeTime: Date?
    }
}
