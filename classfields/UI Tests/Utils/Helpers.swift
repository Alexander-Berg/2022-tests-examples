//
//  Helpers.swift
//  UI Tests
//
//  Created by Evgenii Novikov on 24.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest

func yreSleep(_ time: UInt32, message: String) {
    XCTContext.runActivity(named: message) { [time] _ -> Void in
        sleep(time)
    }
}
