//
//  Created by Alexey Aleshkov on 18.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Testament
import FirebaseCrashlytics

public final class NativeExceptionFactory: ExceptionFactoryProtocol {
    public init() {
    }

    // MARK: - ExceptionFactoryProtocol

    public func makeException(_ event: Event) -> ExceptionModel {
        let name: NSExceptionName
        switch event.disposition {
            case .parameter:
                name = .invalidArgumentException
            case .body, .result:
                name = .internalInconsistencyException
        }
        let exception = ExceptionModel(name: name.rawValue, reason: event.message)
        exception.stackTrace = [
            StackFrame(symbol: event.function, file: event.file, line: Int(exactly: event.line) ?? 0),
        ]
        return exception
    }
}
