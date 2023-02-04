//
//  WrappedBrowserSteps.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 20.08.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.WrappedBrowserAccessibilityIdentifiers

final class WrappedBrowserSteps {
    @discardableResult
    func isEmbeddedBrowserPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие внутреннего браузера на экране") { _ -> Void in
            self.embeddedView.yreEnsureVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func isEmbeddedBrowserNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие внутреннего браузера на экране") { _ -> Void in
            self.embeddedView.yreEnsureNotVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func closeEmbeddedBrowser() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Закрыть' в браузере") { _ -> Void in
            self.embeddedCloseButton
                .yreEnsureHittableWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие браузера на экране") { _ -> Void in
            self.screenView.yreEnsureVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func isScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие браузера на экране") { _ -> Void in
            self.screenView.yreEnsureNotVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnCloseButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Закрыть' в браузере") { _ -> Void in
            self.closeButton
                .yreEnsureHittableWithTimeout()
                .yreTap()
        }
        return self
    }

    // MARK: - Private

    private lazy var screenView = ElementsProvider.obtainElement(identifier: "WrappedBrowser")
    private lazy var closeButton = self.topPanel.buttons["Done"]
    private lazy var topPanel: XCUIElement = {
        let key: String = "TopBrowserBar"

        return self.screenView.otherElements[key]
    }()

    private lazy var embeddedView = ElementsProvider.obtainElement(identifier: WrappedBrowserAccessibilityIdentifiers.view)
    private lazy var embeddedCloseButton = self.embeddedTopPanel.buttons["Done"]
    private lazy var embeddedTopPanel: XCUIElement = {
        let key: String = "TopBrowserBar"

        return self.embeddedView.otherElements[key]
    }()
}
