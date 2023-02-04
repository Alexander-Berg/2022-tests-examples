//
//  Created by Alexey Aleshkov on 18.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import Testament

@objc
public final class TTTAssertionHandler: NSObject, TTTAssertionHandlerProtocol {
    public init(
        eventHandler: EventHandlerProtocol
    ) {
        self.eventHandler = eventHandler
    }

    public var eventHandler: EventHandlerProtocol

    public func handleFailure(
        disposition handleDisposition: TTTDisposition,
        message: String,
        file handleFile: UnsafePointer<CChar>,
        function handleFunction: UnsafePointer<CChar>,
        line: UInt
    ) {
        let timestamp: Date = .init()
        let disposition: Disposition
        switch handleDisposition {
            case .body: disposition = .body
            case .parameter: disposition = .parameter
        }
        let file = String(cString: handleFile)
        let function = String(cString: handleFunction)

        let event = Event(
            timestamp: timestamp,
            level: .assert,
            message: message,
            source: .objectivec,
            disposition: disposition,
            file: file,
            function: function,
            line: line
        )

        self.eventHandler.handle(event)
    }
}
