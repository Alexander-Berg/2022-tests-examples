//  Created by Denis Malykh on 27.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import Foundation

public protocol DemoSDK {
    func makeRepositorySession(repo: RepositoryDescription) -> RepositorySession
}

public func makeDemoSDK(
    logger: YxLogger,
    dispatcher: SkDispatcher
) -> DemoSDK {
    return DemoSDKImpl(logger: logger, dispatcher: dispatcher)
}
