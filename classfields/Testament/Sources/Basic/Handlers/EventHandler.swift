//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

public final class EventHandler<
    Filter: EventFilterProtocol,
    Logger: EventLoggerProtocol,
    Interrupter: EventInterrupterProtocol
>: EventHandlerProtocol {
    public init(
        filter: Filter,
        logger: Logger,
        interrupter: Interrupter
    ) {
        self.filter = filter
        self.logger = logger
        self.interrupter = interrupter
    }

    public var filter: Filter
    public var logger: Logger
    public var interrupter: Interrupter

    // MARK: - EventHandlerProtocol

    public func handle(_ event: Event) {
        switch self.filter.filter(event) {
            case .accept:
                self.logger.log(event)
                self.interrupter.triggerInterrupt(event)

            case .neutral, .deny:
                break
        }
    }
}
