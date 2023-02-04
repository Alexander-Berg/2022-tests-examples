//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

public final class ClosureFilter: EventFilterProtocol {
    public var handler: (Event) -> FilterResult

    public init(_ handler: @escaping (Event) -> FilterResult) {
        self.handler = handler
    }

    public func filter(_ event: Event) -> FilterResult {
        let result = self.handler(event)
        return result
    }
}
