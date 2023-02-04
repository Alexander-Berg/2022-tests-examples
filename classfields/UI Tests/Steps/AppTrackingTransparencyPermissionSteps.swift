//
//  AppTrackingTransparencyPermissionSteps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 11.05.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest

final class AppTrackingTransparencyPermissionSteps {
    enum ActionType {
        case allow
        case forbid
    }

    @discardableResult
    func isPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что запрос на сбор персональных данных показан") { _ -> Void in
            self.alertView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что запрос на сбор персональных данных скрыт") { _ -> Void in
            self.alertView.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func action(type: ActionType) -> Self {
        switch type {
            case .allow:
                self.allowAction()
            case .forbid:
                self.forbidAction()
        }
        return self
    }

    private func allowAction() {
        XCTContext.runActivity(named: "Разрешаем сбор персональных данных") { _ -> Void in
            self.allowButton
                .yreEnsureExists()
                .yreEnsureHittable()
                .yreTap()
        }
    }

    private func forbidAction() {
        XCTContext.runActivity(named: "Запрещаем сбор персональных данных") { _ -> Void in
            self.forbidButton
                .yreEnsureExists()
                .yreEnsureHittable()
                .yreTap()
        }
    }

    private var allowButton: XCUIElement {
        return self.alertView
            .descendants(matching: .button)
            .element(boundBy: 1)
    }

    private var forbidButton: XCUIElement {
        return self.alertView
            .descendants(matching: .button)
            .element(boundBy: 0)
    }

    private var alertView: XCUIElement {
        let springBoard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
        let alertView = springBoard.descendants(matching: .alert).firstMatch
        return alertView
    }
}
