//
//  SafariSteps.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 29.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest

final class SafariSteps {
    @discardableResult
    func isOpened() -> Self {
        XCTContext.runActivity(named: "Проверяем, что Сафари открыт") { _ -> Void in
            XCTAssertTrue(self.safari.wait(for: .runningForeground, timeout: 10),
                          "Safari browser not in foreground state")
        }
        return self
    }

    private lazy var safari = XCUIApplication(bundleIdentifier: "com.apple.mobilesafari")
}
