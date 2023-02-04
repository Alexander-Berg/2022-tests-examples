//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation

public final class AssertionHandler: AssertionHandlerProtocol {
    public init(
        eventHandler: EventHandlerProtocol
    ) {
        self.eventHandler = eventHandler
    }

    public var eventHandler: EventHandlerProtocol

    // MARK: - AssertionHandlerProtocol

    public func handleFailure(
        disposition: Disposition,
        level: Level,
        message: @autoclosure () -> String,
        file: StaticString,
        function: StaticString,
        line: UInt
    ) {
        let timestamp: Date = .init()
        let message = message()
        let file = file.withUTF8Buffer({ String(decoding: $0, as: UTF8.self) })
        let function = function.withUTF8Buffer({ String(decoding: $0, as: UTF8.self) })

        let event = Event(
            timestamp: timestamp,
            level: level,
            message: message,
            source: .swift,
            disposition: disposition,
            file: file,
            function: function,
            line: line
        )

        self.eventHandler.handle(event)
    }
}
