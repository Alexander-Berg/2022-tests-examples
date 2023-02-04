//
//  MortgageFormSteps.swift
//  UI Tests
//
//  Created by Timur Guliamov on 02.11.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import enum YREAccessibilityIdentifiers.MortgageFormAccessibilityIdentifiers

final class MortgageFormSteps {
    enum FormField {
        case name
        case surname
        case patronymic
        case email
        case phoneNumber
    }

    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана \"Нативная форма ипотечной заявки\"") { _ -> Void in
            self.screen.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие экрана \"Нативная форма ипотечной заявки\"") { _ -> Void in
            self.screen.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func makeScreenshot(suffix: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем экран \"Нативная форма ипотечной заявки\" со снапшотом") { _ -> Void in
            self.screen.yreWaitAndCompareScreenshot(identifier: "mortgageForm" + suffix)
        }
        return self
    }

    @discardableResult
    func tapOnView() -> Self {
        XCTContext.runActivity(named: "Тапаем по экрану") { _ -> Void in
            self.scrollView
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnAgreementSwitch() -> Self {
        XCTContext.runActivity(named: "Тапаем по свитчу \"Даю согласие на...\"") { _ -> Void in
            let agreementSwitch = ElementsProvider.obtainElement(
                identifier: Identifiers.agreementSwitch,
                in: self.screen
            )

            agreementSwitch.yreTap()
        }
        return self
    }

    @discardableResult
    func writeTextIn(_ field: FormField, text: String) -> Self {
        XCTContext.runActivity(named: "Пишем \"\(text)\" в поле \"\(Self.nameOfField(field))\"") { _ -> Void in
            let textField = self.textFieldElement(field)

            textField
                .yreEnsureExists()
                .yreTap()
                .yreTypeText(text)
        }
        return self
    }

    @discardableResult
    func isTextIn(_ field: FormField, equalTo text: String) -> Self {
        XCTContext.runActivity(named: "Проверяем, что текст в поле: \"\(Self.nameOfField(field))\" равен \"\(text)\"") { _ -> Void in
            let textField = self.textFieldElement(field)
            XCTAssertEqual(textField.value as? String, text)
        }
        return self
    }

    @discardableResult
    func isErrorUnderFieldPresented(_ field: FormField) -> Self {
        XCTContext.runActivity(named: "Проверяем наличие ошибки в поле \(Self.nameOfField(field))") { _ -> Void in
            let errorLabel = self.errorLabelElement(field)
            errorLabel.yreEnsureExists()
        }
        return self
    }

    @discardableResult
    func isErrorUnderFieldNotPresented(_ field: FormField) -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие ошибки в поле \(Self.nameOfField(field))") { _ -> Void in
            let errorLabel = self.errorLabelElement(field)
            errorLabel.yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func tapOnSendButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Отправить заявку\" ") { _ -> Void in
            let sendButton = ElementsProvider.obtainElement(
                identifier: Identifiers.sendButton,
                in: self.screen
            )

            sendButton
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func waitForTopNotificationViewExistence() -> Self {
        XCTContext.runActivity(named: "Ожидаем появление верхней инфо-плашки") { _ -> Void in
            _ = self.topNotificationView.waitForExistence(timeout: Constants.timeout)
        }
        return self
    }

    // MARK: - Private

    private typealias Identifiers = MortgageFormAccessibilityIdentifiers.Form

    private lazy var screen = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var scrollView = ElementsProvider.obtainElement(identifier: Identifiers.scrollView, in: self.screen)
    private lazy var topNotificationView = ElementsProvider.obtainElement(identifier: "top.notification.view")

    private func textFieldElement(_ field: FormField) -> XCUIElement {
        let identifier: String
        switch field {
            case .name: identifier = Identifiers.nameTextEditView
            case .surname: identifier = Identifiers.surnameTextEditView
            case .patronymic: identifier = Identifiers.patronymicTextEditView
            case .email: identifier = Identifiers.emailTextEditView
            case .phoneNumber: identifier = Identifiers.phoneNumberTextEditView
        }

        return ElementsProvider.obtainElement(
            identifier: identifier,
            in: self.screen
        )
    }

    private func errorLabelElement(_ field: FormField) -> XCUIElement {
        let identifier: String
        switch field {
            case .name: identifier = Identifiers.nameError
            case .surname: identifier = Identifiers.surnameError
            case .patronymic: identifier = Identifiers.patronymicError
            case .email: identifier = Identifiers.emailError
            case .phoneNumber: identifier = Identifiers.phoneNumberError
        }

        return ElementsProvider.obtainElement(
            identifier: identifier,
            in: self.screen
        )
    }

    private static func nameOfField(_ field: FormField) -> String {
        switch field {
            case .name: return "Имя"
            case .surname: return "Фамилия"
            case .patronymic: return "Отчество"
            case .email: return "Электронная почта"
            case .phoneNumber: return "Номер телефона"
        }
    }
}
