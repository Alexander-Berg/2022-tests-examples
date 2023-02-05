//  Created by Denis Malykh on 27.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import Foundation

public protocol RepositorySession {
    var description: RepositoryDescription { get }
    var info: SkObservableValue<Repository?> { get }

    @discardableResult
    func acquireInfo() -> SkResultOperation<Repository, Error>
}
