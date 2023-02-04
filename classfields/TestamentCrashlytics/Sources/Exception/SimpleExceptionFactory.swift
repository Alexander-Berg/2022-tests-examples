//
//  Created by Alexey Aleshkov on 18.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Testament
import FirebaseCrashlytics

public final class SimpleExceptionFactory: ExceptionFactoryProtocol {
    public var name: String

    public init(name: String) {
        self.name = name
    }

    // MARK: - ExceptionFactoryProtocol

    public func makeException(_ event: Event) -> ExceptionModel {
        let exception = ExceptionModel(name: self.name, reason: event.message)
        exception.stackTrace = [
            StackFrame(symbol: event.function, file: event.file, line: Int(exactly: event.line) ?? 0),
        ]
        return exception
    }
}
