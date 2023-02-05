//
//  Created by Timur Turaev on 17.02.2021.
//

import Foundation
import Utils

open class TestLogger: Logger {
    public var infos: [String] = .empty
    public var errors: [String] = .empty

    public init() {
    }

    public func info(_ message: String) {
        print("💬[Test] " + message)

        self.infos.append(message)
    }

    public func error(_ message: String) {
        print("❗️[Test] " + message)

        self.errors.append(message)
    }
}
