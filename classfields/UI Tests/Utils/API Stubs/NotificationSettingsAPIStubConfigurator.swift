//
//  NotificationSettingsAPIStubConfigurator.swift
//  UI Tests
//
//  Created by Timur Guliamov on 30.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation

final class NotificationSettingsAPIStubConfigurator {
    static func setupUserNotifications(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.setupStub(
            remotePath: "/1.0/user/notifications",
            filename: "user-notifications-empty.debug"
        )
    }
}
