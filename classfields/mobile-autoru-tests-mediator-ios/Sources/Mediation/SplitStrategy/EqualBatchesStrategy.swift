import Foundation

struct EqualBatchesStrategy: SplitStrategy {
    private let maxAvailableAgentsCount: Int
    private let maxBatchSize: Int

    init(maxAvailableAgentsCount: Int, maxBatchSize: Int) {
        self.maxAvailableAgentsCount = maxAvailableAgentsCount
        self.maxBatchSize = maxBatchSize
    }

    func split(tests: [TestCase]) -> [TestsBatch] {
        if tests.isEmpty {
            return []
        }

        if tests.count <= maxBatchSize {
            return [TestsBatch(tests: tests)]
        }

        let tests = tests.shuffled()

        let batchSize = min(maxBatchSize, max(1, Int(Double(tests.count / maxAvailableAgentsCount).rounded(.up))))

        let batches = stride(from: 0, to: tests.count, by: batchSize).map {
            Array(tests[$0..<min($0 + batchSize, tests.count)])
        }
        return batches.filter { !$0.isEmpty }.map { TestsBatch(tests: $0) }
    }
}
