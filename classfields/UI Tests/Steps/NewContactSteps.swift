//
//  NewContactSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 26.07.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.NewContactAccessibilityIdentifiers

final class NewContactSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана добавления номера телефона") { _ -> Void in
            self.screen.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func typeText(_ text: String) -> Self {
        XCTContext.runActivity(named: "Вводим текст \(text)") { _ -> Void in
            self.textField
                .yreEnsureExists()
                .yreTap()
                .yreTypeText(text)
        }
        return self
    }

    @discardableResult
    func tapSendButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку отправить") { _ -> Void in
            self.sendButton
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = NewContactAccessibilityIdentifiers

    private lazy var screen: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var textField: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.textField,
        in: self.screen
    )
    private lazy var sendButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.sendButton,
        in: self.screen
    )
}
