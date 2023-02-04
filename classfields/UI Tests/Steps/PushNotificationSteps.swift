//
//  PushNotificationSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 19.03.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest

final class PushNotificationSteps {
    @discardableResult
    func openCurrentPush() -> Self {
        XCTContext.runActivity(named: "Нажимаем на пуш") { _ -> Void in
            self.notificationView
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func isPushNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что пуша нет") { _ -> Void in
            // wait a sec for possible push presentation
            sleep(1)
            self.notificationView.yreEnsureNotExists()
        }
        return self
    }

    private lazy var notificationView: XCUIElement = {
        let springBoard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        return springBoard.otherElements.descendants(matching: .any)["NotificationShortLookView"]
    }()
}
