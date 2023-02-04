//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Testament
import FirebaseCrashlytics

public final class ErrorEventLogger<ErrorFactory: ErrorFactoryProtocol>: EventLoggerProtocol {
    public var errorFactory: ErrorFactory

    public init(errorFactory: ErrorFactory) {
        self.crashlyticsInstance = Crashlytics.crashlytics()
        self.errorFactory = errorFactory
    }

    public func log(_ event: Event) {
        let error = self.errorFactory.makeError(event)
        self.crashlyticsInstance.record(error: error)
    }

    private let crashlyticsInstance: Crashlytics
}
