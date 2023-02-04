//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

public final class DispositionFilter: EventFilterProtocol {
    public enum MatchingRule {
        case notContains
        case contains
    }

    public var dispositions: Set<Disposition>

    public var matchingRule: MatchingRule {
        didSet {
            self.updateMatchingFunc()
        }
    }

    public var matchResult: FilterResult
    public var mismatchResult: FilterResult

    public init(_ matchingRule: MatchingRule, dispositions: Set<Disposition>) {
        self.matchingRule = matchingRule
        self.dispositions = dispositions
        self.matchResult = .accept
        self.mismatchResult = .neutral
        self.matchingFunc = { _, _ in false }

        self.updateMatchingFunc()
    }

    public func filter(_ event: Event) -> FilterResult {
        let result: FilterResult
        if self.matchingFunc(event.disposition, self.dispositions) {
            result = self.matchResult
        }
        else {
            result = self.mismatchResult
        }
        return result
    }

    // MARK: - Private

    private var matchingFunc: (_ eventCategory: Disposition, _ filterCategories: Set<Disposition>) -> Bool

    private func updateMatchingFunc() {
        let result: (Disposition, Set<Disposition>) -> Bool
        switch self.matchingRule {
            case .notContains: result = Self.matchNotContains
            case .contains: result = Self.matchContains
        }
        self.matchingFunc = result
    }

    private static func matchNotContains(_ eventData: Disposition, _ filterData: Set<Disposition>) -> Bool {
        let result = !filterData.contains(eventData)
        return result
    }

    private static func matchContains(_ eventData: Disposition, _ filterData: Set<Disposition>) -> Bool {
        let result = filterData.contains(eventData)
        return result
    }
}
