//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

public final class SwiftAssertionHandler: AssertionHandlerProtocol {
    public init() {
    }

    // MARK: - AssertionHandlerProtocol

    public func handleFailure(
        disposition: Disposition,
        level: Level,
        message: @autoclosure () -> String,
        file: StaticString,
        function: StaticString,
        line: UInt
    ) {
        switch level {
            case .assert:
                Swift.assert(false, message(), file: file, line: line)

            case .precondition:
                Swift.precondition(false, message(), file: file, line: line)

            case .fatal:
                Swift.fatalError(message(), file: file, line: line)
        }
    }
}
