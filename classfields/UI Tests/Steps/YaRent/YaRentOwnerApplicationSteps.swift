//
//  YaRentOwnerApplicationSteps.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 22.12.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import enum YREAccessibilityIdentifiers.YaRentOwnerApplicationAccessibilityIdentifiers

final class YaRentOwnerApplicationSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана заявки собственника") { _ -> Void in
            self.screen.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие экрана заявки собственника") { _ -> Void in
            self.screen.yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func tapOnCloseButton() -> Self {
        XCTContext.runActivity(named: "Закрываем экран заявки собственника") { _ -> Void in
            self.closeButton
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnSaveButton() -> Self {
        XCTContext.runActivity(named: #"Скроллим до кнопки "Сохранить""#) { _ -> Void in
            self.screen.scrollToElement(element: self.saveButton, direction: .up, swipeLimits: 10)
        }

        XCTContext.runActivity(named: #"Нажимаем на кнопку "Сохранить""#) { _ -> Void in
            self.saveButton
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnSubmitButton() -> Self {
        XCTContext.runActivity(named: #"Скроллим до кнопки "Отправить заявку""#) { _ -> Void in
            self.screen.scrollToElement(element: self.submitButton, direction: .up, swipeLimits: 10)
        }

        XCTContext.runActivity(named: #"Нажимаем на кнопку "Отправить заявку""#) { _ -> Void in
            self.submitButton
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    // MARK: - Private

    private typealias Identifiers = YaRentOwnerApplicationAccessibilityIdentifiers

    private lazy var screen: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.view
    )

    private lazy var closeButton: XCUIElement = ElementsProvider.obtainBackButton()

    private lazy var saveButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.saveButton,
        in: self.screen
    )

    private lazy var submitButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.submitButton,
        in: self.screen
    )
}
