//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

public final class FileFilter: EventFilterProtocol {
    public var file: String

    public var matchResult: FilterResult
    public var mismatchResult: FilterResult

    public init(file: String) {
        self.file = file
        self.matchResult = .accept
        self.mismatchResult = .neutral
    }

    public func filter(_ event: Event) -> FilterResult {
        let filePath = event.file
        let result: FilterResult
        if filePath.contains(self.file) {
            result = self.matchResult
        }
        else {
            result = self.mismatchResult
        }
        return result
    }
}
