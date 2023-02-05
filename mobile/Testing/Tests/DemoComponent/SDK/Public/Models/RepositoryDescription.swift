//  Created by Denis Malykh on 27.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import Foundation

public struct RepositoryDescription {
    public let owner: String
    public let name: String

    public init(owner: String, name: String) {
        self.owner = owner
        self.name = name
    }
}
