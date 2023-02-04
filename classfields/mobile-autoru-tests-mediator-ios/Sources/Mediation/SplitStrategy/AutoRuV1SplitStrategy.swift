import Foundation

struct AutoRuV1SplitStrategy: SplitStrategy {
    private let maxAvailableAgentsCount: Int
    private let maxBatchSize: Int

    init(maxAvailableAgentsCount: Int, maxBatchSize: Int) {
        self.maxAvailableAgentsCount = maxAvailableAgentsCount
        self.maxBatchSize = maxBatchSize
    }

    func split(tests: [TestCase]) -> [TestsBatch] {
        var unitTests: [TestCase] = []
        var uiTests: [TestCase] = []

        for test in tests {
            if test.isUI {
                uiTests.append(test)
            } else {
                unitTests.append(test)
            }
        }

        let unitsBatches: [TestsBatch]
        if unitTests.isEmpty {
            unitsBatches = []
        } else {
            unitsBatches = [TestsBatch(tests: unitTests, parallelize: false)]
        }

        let equalStrategy = EqualBatchesStrategy(
            maxAvailableAgentsCount: maxAvailableAgentsCount,
            maxBatchSize: maxBatchSize
        )

        let uisBatches = equalStrategy.split(tests: uiTests)

        return unitsBatches + uisBatches
    }
}

private extension TestCase {
    var isUI: Bool { name.starts(with: "UITests/") }
}
