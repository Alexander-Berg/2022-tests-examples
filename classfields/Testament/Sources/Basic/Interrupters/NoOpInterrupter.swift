//
//  Created by Alexey Aleshkov on 18.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

public final class NoOpInterrupter: EventInterrupterProtocol {
    public static let sharedInstance: NoOpInterrupter = .init()

    // MARK: - EventInterrupterProtocol

    public func triggerInterrupt(_ event: Event) {
    }

    // MARK: - Private

    private init() {
    }
}
