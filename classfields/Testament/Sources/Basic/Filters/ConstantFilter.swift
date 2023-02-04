//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

public class ConstantFilter: EventFilterProtocol {
    public var result: FilterResult

    public init(_ result: FilterResult = .neutral) {
        self.result = result
    }

    public func filter(_ event: Event) -> FilterResult {
        return self.result
    }
}
