//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

public final class LevelFilter: EventFilterProtocol {
    public enum MatchingRule {
        case higherThan
        case higherThanOrEqual
        case equal
        case lowerThanOrEqual
        case lowerThan
    }

    public var level: Level

    public var matchingRule: MatchingRule {
        didSet {
            self.updateMatchingFunc()
        }
    }

    public var matchResult: FilterResult
    public var mismatchResult: FilterResult

    public init(_ matchingRule: MatchingRule, level: Level) {
        self.level = level
        self.matchingRule = matchingRule
        self.matchResult = .accept
        self.mismatchResult = .neutral
        self.matchingFunc = { _, _ in false }

        self.updateMatchingFunc()
    }

    public func filter(_ event: Event) -> FilterResult {
        let result: FilterResult
        if self.matchingFunc(event.level, self.level) {
            result = self.matchResult
        }
        else {
            result = self.mismatchResult
        }
        return result
    }

    // MARK: - Private

    private var matchingFunc: (_ eventLevel: Level, _ filterLevel: Level) -> Bool

    private func updateMatchingFunc() {
        let result: (Level, Level) -> Bool
        switch self.matchingRule {
            case .higherThan:
                result = { $0 > $1 }
            case .higherThanOrEqual:
                result = { $0 >= $1 }
            case .equal:
                result = { $0 == $1 }
            case .lowerThanOrEqual:
                result = { $0 <= $1 }
            case .lowerThan:
                result = { $0 < $1 }
        }
        self.matchingFunc = result
    }
}
