//
//  Created by Alexey Aleshkov on 11.01.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

public protocol AssertionHandlerProtocol {
    func handleFailure(
        disposition: Disposition,
        level: Level,
        message: @autoclosure () -> String,
        file: StaticString,
        function: StaticString,
        line: UInt
    )
}
