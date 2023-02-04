import Mediation
import Vapor

struct Batch: Content {
    let id: String
    let tests: [String]
    let parallelize: Bool
    let executionInfo: ExecutionInfo?

    struct ExecutionInfo: Content {
        let dequeueTime: Date
        let completeTime: Date?
        let executor: TeamcityBuildID
    }

    init(from batch: TestsBatch) {
        self.id = batch.id
        self.tests = batch.tests.map(\.name)
        self.parallelize = batch.parallelize
        self.executionInfo = batch.executionInfo.flatMap {
            ExecutionInfo(dequeueTime: $0.dequeueTime, completeTime: $0.completeTime, executor: $0.executor)
        }
    }
}
