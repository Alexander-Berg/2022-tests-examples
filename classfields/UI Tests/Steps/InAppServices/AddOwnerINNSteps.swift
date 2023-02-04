//
//  AddOwnerINNSteps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 28.10.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import enum YREAccessibilityIdentifiers.InAppServicesAccessibilityIdentifiers

final class AddOwnerINNSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана ввода ИНН") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }
    
    
    @discardableResult
    func isScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие экрана ввода ИНН") { _ -> Void in
            self.screenView.yreEnsureNotExistsWithTimeout()
        }
        return self
    }
    
    @discardableResult
    func isErrorShown(with text: String) -> Self {
        XCTContext.runActivity(named: "Проверяем, что показали ошибку с текстом \(text)") { _ -> Void in
            self.errorText
                .yreEnsureVisible()
                .yreEnsureLabelStarts(with: text)
        }
        return self
    }
    
    @discardableResult
    func isErrorHidden() -> Self {
        XCTContext.runActivity(named: "Проверяем, что ошибка скрыта") { _ -> Void in
            self.errorText.yreEnsureNotVisible()
        }
        return self
    }
    
    @discardableResult
    func enterText(text: String) -> Self {
        XCTContext.runActivity(named: "Вводим текст '\(text)'") { _ -> Void in
            self.textField
                .yreEnsureExists()
                .yreTap()
                .yreClearText()
                .yreTypeText(text)
        }
        return self
    }
    
    @discardableResult
    func submitINN() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Добавить'") { _ -> Void in
            self.submitButton.yreTap()
        }
        return self
    }

    // MARK: - Private
    
    private typealias AccessibilityIdentifiers = InAppServicesAccessibilityIdentifiers.AddOwnerINNModule

    private lazy var textField = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.textField,
        in: self.screenView
    )
    
    private lazy var errorText = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.errorText,
        in: self.screenView
    )
    
    private lazy var submitButton = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.submitButton,
        type: .button,
        in: self.screenView
    )
    
    private lazy var screenView = ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.view)
}
