//
//  Created by Alexey Aleshkov on 18.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Testament

public final class SimpleErrorFactory: ErrorFactoryProtocol {
    public var domain: String
    public var code: Int

    public init(domain: String, code: Int) {
        self.domain = domain
        self.code = code
    }

    // MARK: - ErrorFactoryProtocol

    public func makeError(_ event: Event) -> NSError {
        let userInfo: [String: Any]?
        if !event.message.isEmpty {
            userInfo = [
                NSLocalizedFailureReasonErrorKey: event.message,
            ]
        }
        else {
            userInfo = nil
        }

        let error = NSError(
            domain: self.domain,
            code: self.code,
            userInfo: userInfo
        )

        return error
    }
}
