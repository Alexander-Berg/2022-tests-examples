//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation

public final class InternalInconsistencyInterrupter: EventInterrupterProtocol {
    public init() {
    }

    // MARK: - EventInterrupterProtocol

    public func triggerInterrupt(_ event: Event) {
        let name: NSExceptionName
        switch event.disposition {
            case .parameter:
                name = .invalidArgumentException
            case .body, .result:
                name = .internalInconsistencyException
        }
        let exception = NSException(
            name: name,
            reason: event.message,
            userInfo: nil
        )
        exception.raise()
    }
}
