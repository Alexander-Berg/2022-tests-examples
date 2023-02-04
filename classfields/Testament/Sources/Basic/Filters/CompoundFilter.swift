//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

extension CompoundFilter {
    public static func not(_ subfilter: EventFilterProtocol) -> Self {
        return self.init(type: .not(subfilter))
    }

    public static func and(_ lhs: EventFilterProtocol, _ rhs: EventFilterProtocol) -> Self {
        return self.init(type: .and([lhs, rhs]))
    }

    public static func and(_ filters: [EventFilterProtocol]) -> Self {
        return self.init(type: .and(filters))
    }

    public static func or(_ lhs: EventFilterProtocol, _ rhs: EventFilterProtocol) -> Self {
        return self.init(type: .or([lhs, rhs]))
    }

    public static func or(_ filters: [EventFilterProtocol]) -> Self {
        return self.init(type: .or(filters))
    }
}

/// https://en.wikipedia.org/wiki/Three-valued_logic
public final class CompoundFilter: EventFilterProtocol {
    public enum LogicalType {
        case not(EventFilterProtocol)
        case and([EventFilterProtocol])
        case or([EventFilterProtocol]) // swiftlint:disable:this identifier_name
    }

    public init(type: LogicalType) {
        switch type {
            case .not(let filter):
                self.impl = CompoundNotFilter(filter)

            case .and(let filters):
                if filters.isEmpty {
                    self.impl = ConstantFilter(.neutral)
                }
                else if filters.count == 1 {
                    self.impl = filters[0]
                }
                else if filters.count == 2 {
                    self.impl = CompoundAndFilter(filters[0], filters[1])
                }
                else {
                    self.impl = CompoundCollectionAndFilter(filters)
                }

            case .or(let filters):
                if filters.isEmpty {
                    self.impl = ConstantFilter(.neutral)
                }
                else if filters.count == 1 {
                    self.impl = filters[0]
                }
                else if filters.count == 2 {
                    self.impl = CompoundOrFilter(filters[0], filters[1])
                }
                else {
                    self.impl = CompoundCollectionOrFilter(filters)
                }
        }
    }

    // MARK: - EventFilterProtocol

    public func filter(_ event: Event) -> FilterResult {
        let result = self.impl.filter(event)
        return result
    }

    let impl: EventFilterProtocol
}

/// Logical negation (NOT)
private final class CompoundNotFilter: EventFilterProtocol {
    init(_ filter: EventFilterProtocol) {
        self.filter = filter
    }

    let filter: EventFilterProtocol

    func filter(_ event: Event) -> FilterResult {
        let filterResult = self.filter.filter(event)
        let result = !filterResult
        return result
    }
}

/// Logical conjunction (AND)
private final class CompoundAndFilter: EventFilterProtocol {
    init(_ left: EventFilterProtocol, _ right: EventFilterProtocol) {
        self.leftFilter = left
        self.rightFilter = right
    }

    let leftFilter: EventFilterProtocol
    let rightFilter: EventFilterProtocol

    func filter(_ event: Event) -> FilterResult {
        let result = self.leftFilter.filter(event) && self.rightFilter.filter(event)
        return result
    }
}

/// Logical disjunction (OR)
private final class CompoundOrFilter: EventFilterProtocol {
    init(_ left: EventFilterProtocol, _ right: EventFilterProtocol) {
        self.leftFilter = left
        self.rightFilter = right
    }

    let leftFilter: EventFilterProtocol
    let rightFilter: EventFilterProtocol

    func filter(_ event: Event) -> FilterResult {
        let result = self.leftFilter.filter(event) || self.rightFilter.filter(event)
        return result
    }
}

private final class CompoundCollectionAndFilter: EventFilterProtocol {
    init(_ filters: [EventFilterProtocol]) {
        self.filters = filters
    }

    let filters: [EventFilterProtocol]

    func filter(_ event: Event) -> FilterResult {
        var result: FilterResult

        if let firstFilter = self.filters.first {
            result = firstFilter.filter(event)
            switch result {
                case .deny:
                    break

                case .neutral, .accept:
                    var iterator = self.filters.dropFirst().makeIterator()
                    while let restFilter = iterator.next() {
                        switch result {
                            case .deny:
                                break

                            case .neutral, .accept:
                                result = result && restFilter.filter(event)
                        }
                    }
            }
        }
        else {
            result = .neutral
        }

        return result
    }
}

private final class CompoundCollectionOrFilter: EventFilterProtocol {
    init(_ filters: [EventFilterProtocol]) {
        self.filters = filters
    }

    let filters: [EventFilterProtocol]

    func filter(_ event: Event) -> FilterResult {
        var result: FilterResult

        if let firstFilter = self.filters.first {
            result = firstFilter.filter(event)
            switch result {
                case .accept:
                    break

                case .neutral, .deny:
                    var iterator = self.filters.dropFirst().makeIterator()
                    while let restFilter = iterator.next() {
                        switch result {
                            case .accept:
                                break

                            case .neutral, .deny:
                                result = result || restFilter.filter(event)
                        }
                    }
            }
        }
        else {
            result = .neutral
        }

        return result
    }
}

extension FilterResult {
    static prefix func ! (_ value: FilterResult) -> FilterResult {
        let result: FilterResult

        switch value {
            case .accept:
                result = .deny
            case .neutral:
                result = .neutral
            case .deny:
                result = .accept
        }

        return result
    }

    static func && (lhs: FilterResult, rhs: @autoclosure () -> FilterResult) -> FilterResult {
        let result: FilterResult

        switch lhs {
            case .deny:
                result = .deny

            case .neutral, .accept:
                let rhs = rhs()
                switch (lhs, rhs) {
                    case (_, .deny),
                         (.deny, _):
                        result = .deny

                    case (.neutral, .neutral),
                         (.accept, .neutral),
                         (.neutral, .accept):
                        result = .neutral

                    case (.accept, .accept):
                        result = .accept
                }
        }

        return result
    }

    static func || (lhs: FilterResult, rhs: @autoclosure () -> FilterResult) -> FilterResult {
        let result: FilterResult

        switch lhs {
            case .accept:
                result = .accept

            case .neutral, .deny:
                let rhs = rhs()
                switch (lhs, rhs) {
                    case (_, .accept),
                         (.accept, _):
                        result = .accept

                    case (.neutral, .neutral),
                         (.deny, .neutral),
                         (.neutral, .deny):
                        result = .neutral

                    case (.deny, .deny):
                        result = .deny
                }
        }

        return result
    }
}
