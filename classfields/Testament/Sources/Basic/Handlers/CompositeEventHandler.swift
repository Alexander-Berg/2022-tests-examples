//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

public final class CompositeEventHandler: EventHandlerProtocol {
    public init(_ handlers: [EventHandlerProtocol] = .init()) {
        self.handlers = handlers
    }

    public var handlers: [EventHandlerProtocol]

    // MARK: - EventHandlerProtocol

    public func handle(_ event: Event) {
        for logger in self.handlers {
            logger.handle(event)
        }
    }
}
