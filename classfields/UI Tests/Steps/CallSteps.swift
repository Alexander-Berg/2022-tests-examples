//
//  CallSteps.swift
//  UI Tests
//
//  Created by Ibrakhim Nikishin on 3/12/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest

final class CallSteps {
    @discardableResult
    func waitForTopNotificationViewExistence() -> Self {
        XCTContext.runActivity(named: "Ожидаем появление верхней инфо-плашки") { _ -> Void in
            _ = self.topNotificationView.waitForExistence(timeout: Constants.timeout)
        }
        return self
    }

    private lazy var topNotificationView: XCUIElement = ElementsProvider.obtainElement(identifier: "top.notification.view")
}
