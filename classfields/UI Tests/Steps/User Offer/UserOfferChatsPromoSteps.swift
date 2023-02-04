//
//  UserOfferChatsPromoSteps.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 14.07.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import enum YREDesignKit.PromoBannerAccessibilityIdentifiers

final class UserOfferChatsPromoSteps {
    init(element: XCUIElement) {
        self.element = element
    }

    @discardableResult
    func isPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие уведомления 'Включить сообщения'") { _ -> Void in
            self.element.yreEnsureVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func isDismissed() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие уведомления 'Включить сообщения'") { _ -> Void in
            self.element.yreEnsureNotVisible()
        }
        return self
    }

    @discardableResult
    func isHittable() -> Self {
        XCTContext.runActivity(named: "Проверяем доступность нажатия на уведомление") { _ -> Void in
            self.element.yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func tapOnView() -> Self {
        XCTContext.runActivity(named: "Нажатие на уведомление") { _ -> Void in
            self.element.yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnSettingsButton() -> Self {
        XCTContext.runActivity(named: "Нажатие на 'Настройки'") { _ -> Void in
            self.actionButton.yreTap()
        }
        return self
    }

    @discardableResult
    func toggle() -> Self {
        XCTContext.runActivity(named: "Переключаем настройку 'Включить сообщения'") { _ -> Void in
            self.switchControl.yreForceTap()
        }
        return self
    }

    @discardableResult
    func tapOnHide() -> Self {
        XCTContext.runActivity(named: "Нажатие на 'Скрыть'") { _ -> Void in
            self.hideButton.yreTap()
        }
        return self
    }

    private typealias Identifiers = PromoBannerAccessibilityIdentifiers

    private lazy var switchControl = ElementsProvider.obtainElement(identifier: Identifiers.switchControl)
    private lazy var actionButton = ElementsProvider.obtainElement(identifier: Identifiers.actionButton)
    private lazy var hideButton = ElementsProvider.obtainElement(identifier: Identifiers.hideButton)

    private let element: XCUIElement
}
