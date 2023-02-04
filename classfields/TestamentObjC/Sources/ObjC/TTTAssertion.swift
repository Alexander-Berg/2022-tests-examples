//
//  Created by Alexey Aleshkov on 16.12.2020.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import Testament

@objc
public final class TTTAssertion: NSObject {
    @objc
    public static func assert(
        disposition: TTTDisposition,
        message: String,
        file: UnsafePointer<CChar>,
        function: UnsafePointer<CChar>,
        line: Int = #line
    ) {
        self.assertionHandler?.handleFailure(
            disposition: disposition,
            message: message,
            file: file,
            function: function,
            line: UInt(exactly: line) ?? 0
        )
    }

    public static var assertionHandler: TTTAssertionHandlerProtocol?
}
