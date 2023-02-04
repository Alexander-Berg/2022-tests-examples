//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

public final class SourceFilter: EventFilterProtocol {
    public enum MatchingRule {
        case notContains
        case contains
    }

    public var sources: Set<Source>

    public var matchingRule: MatchingRule {
        didSet {
            self.updateMatchingFunc()
        }
    }

    public var matchResult: FilterResult
    public var mismatchResult: FilterResult

    public init(_ matchingRule: MatchingRule, sources: Set<Source>) {
        self.matchingRule = matchingRule
        self.sources = sources
        self.matchResult = .accept
        self.mismatchResult = .neutral
        self.matchingFunc = { _, _ in false }

        self.updateMatchingFunc()
    }

    public func filter(_ event: Event) -> FilterResult {
        let result: FilterResult
        if self.matchingFunc(event.source, self.sources) {
            result = self.matchResult
        }
        else {
            result = self.mismatchResult
        }
        return result
    }

    // MARK: - Private

    private var matchingFunc: (_ eventCategory: Source, _ filterCategories: Set<Source>) -> Bool

    private func updateMatchingFunc() {
        let result: (Source, Set<Source>) -> Bool
        switch self.matchingRule {
            case .notContains: result = Self.matchNotContains
            case .contains: result = Self.matchContains
        }
        self.matchingFunc = result
    }

    private static func matchNotContains(_ eventData: Source, _ filterData: Set<Source>) -> Bool {
        let result = !filterData.contains(eventData)
        return result
    }

    private static func matchContains(_ eventData: Source, _ filterData: Set<Source>) -> Bool {
        let result = filterData.contains(eventData)
        return result
    }
}
