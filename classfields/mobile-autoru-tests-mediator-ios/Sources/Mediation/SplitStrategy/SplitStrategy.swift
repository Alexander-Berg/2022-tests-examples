import Foundation

/// Описание того, как тесты будут разделяться на батчи
public protocol SplitStrategy {
    init(maxAvailableAgentsCount: Int, maxBatchSize: Int)

    func split(tests: [TestCase]) -> [TestsBatch]
}

/// Известные стратегии
public enum SplitStrategyType: String {
    case equal
    case autoRuV1

    public var type: SplitStrategy.Type {
        switch self {
        case .equal:
            return EqualBatchesStrategy.self
        case .autoRuV1:
            return AutoRuV1SplitStrategy.self
        }
    }
}
