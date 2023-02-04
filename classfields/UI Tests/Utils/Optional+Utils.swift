//
//  Optional+Utils.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 25.04.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest

extension Optional {
    func yreForced() -> Wrapped {
        guard let someValue = self else {
            XCTFail("Unexpected nil")
            fatalError("No way")
        }
        return someValue
    }
}
