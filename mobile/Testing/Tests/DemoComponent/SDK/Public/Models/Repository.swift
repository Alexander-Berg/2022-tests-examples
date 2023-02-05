//  Created by Denis Malykh on 27.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import Foundation

public struct Repository {
    public let id: Int
    public let name: String
    public let fullName: String
    public let isPrivate: Bool
    public let url: URL
    public let description: String
}
