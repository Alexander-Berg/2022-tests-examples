//
//  TopNotificationToastViewSteps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 07.02.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREAccessibilityIdentifiers

final class TopNotificationToastViewSteps {
    @discardableResult
    func waitForTopNotificationViewExistence() -> Self {
        XCTContext.runActivity(named: "Ожидаем появление верхней инфо-плашки") { _ -> Void in
            self.topNotificationView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    private typealias AccessibilityIdentifiers = TopNotificationToastViewAccessibilityIdentifiers

    private var topNotificationView: XCUIElement {
        return ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.view)
    }
}
