//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation

public struct Event {
    // when
    public let timestamp: Date

    // what
    public let level: Level
    public let message: String

    // where
    public let source: Source
    public let disposition: Disposition
    public let file: String
    public let function: String
    public let line: UInt

    public init(
        timestamp: Date,
        level: Level,
        message: String,
        source: Source,
        disposition: Disposition,
        file: String,
        function: String,
        line: UInt
    ) {
        self.timestamp = timestamp
        self.level = level
        self.message = message
        self.source = source
        self.disposition = disposition
        self.file = file
        self.function = function
        self.line = line
    }
}
