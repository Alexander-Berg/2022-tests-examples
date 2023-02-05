//
// Created by Artem Zoshchuk on 12/02/2020.
// Copyright (c) 2020 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public class DefaultErrorThrower: ErrorThrower {

    public static let instance = DefaultErrorThrower()

    public func fail(_ message: String) {
        XCTFail(message)
    }
}
