//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

public final class CompositeFilter: EventFilterProtocol {
    public init(_ filters: [EventFilterProtocol] = .init()) {
        self.filters = filters
    }

    public var filters: [EventFilterProtocol]

    // MARK: - EventFilterProtocol

    public func filter(_ event: Event) -> FilterResult {
        for filter in self.filters {
            let filterResult = filter.filter(event)
            switch filterResult {
                case .accept, .deny:
                    return filterResult
                case .neutral:
                    continue
            }
        }
        return .neutral
    }
}
