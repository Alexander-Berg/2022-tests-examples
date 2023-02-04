@testable import Mediation

final class MockStrategy: SplitStrategy {
    private let batches: [TestsBatch]

    init(maxAvailableAgentsCount: Int, maxBatchSize: Int) {
        self.batches = [TestsBatch(tests: [])]
    }

    init(batches: [TestsBatch]) {
        self.batches = batches
    }

    func split(tests: [TestCase]) -> [TestsBatch] { batches }
}
