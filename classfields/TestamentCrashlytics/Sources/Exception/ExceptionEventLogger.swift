//
//  Created by Alexey Aleshkov on 18.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Testament
import FirebaseCrashlytics

public final class ExceptionEventLogger<ExceptionFactory: ExceptionFactoryProtocol>: EventLoggerProtocol {
    public var exceptionFactory: ExceptionFactory

    public init(exceptionFactory: ExceptionFactory) {
        self.exceptionFactory = exceptionFactory
    }

    public func log(_ event: Event) {
        let exceptionModel = self.exceptionFactory.makeException(event)
        // @coreshock: Taking instance on each log is because it can be not configured during logger configuration.
        Crashlytics.crashlytics().record(exceptionModel: exceptionModel)
    }
}
