//
//  MortgageFormSMSConfirmationSteps.swift
//  UI Tests
//
//  Created by Timur Guliamov on 02.11.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import enum YREAccessibilityIdentifiers.MortgageFormAccessibilityIdentifiers

final class MortgageFormSMSConfirmationSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана \"СМС Подтверждение\"") { _ -> Void in
            self.screen.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие экрана \"СМС Подтверждение\"") { _ -> Void in
            self.screen.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isErrorUnderFieldPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие ошибки") { _ -> Void in
            let errorLabel = ElementsProvider.obtainElement(
                identifier: Identifiers.error,
                in: self.screen
            )
            errorLabel.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isErrorUnderFieldNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие ошибки") { _ -> Void in
            let errorLabel = ElementsProvider.obtainElement(
                identifier: Identifiers.error,
                in: self.screen
            )
            errorLabel.yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func makeScreenshot(suffix: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем экран \"СМС Подтверждение\" со снапшотом") { _ -> Void in
            self.screen.yreWaitAndCompareScreenshot(identifier: "mortgageFormSMSConfirmation" + suffix)
        }
        return self
    }

    @discardableResult
    func writeText(text: String) -> Self {
        XCTContext.runActivity(named: "Пишем \"\(text)\" в поле \"Код\"") { _ -> Void in
            let textEditView = ElementsProvider.obtainElement(
                identifier: Identifiers.textEditView,
                in: self.screen
            )

            textEditView
                .yreEnsureExistsWithTimeout()
                .yreTap()
                .yreTypeText(text)
        }
        return self
    }

    @discardableResult
    func tapOnView() -> Self {
        XCTContext.runActivity(named: "Тапаем по экрану") { _ -> Void in
            let title = ElementsProvider.obtainElement(
                identifier: Identifiers.title,
                in: self.screen
            )
            title.yreEnsureExists().yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnContinueButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Продолжить\"") { _ -> Void in
            let button = ElementsProvider.obtainButton(
                identifier: Identifiers.continueButton,
                in: self.screen
            )

            button
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func waitForRetryButtonEnable() -> Self {
        XCTContext.runActivity(named: "Проверяем, что кнопка \"Отправить повторно\" активна") { _ -> Void in
            let button = ElementsProvider.obtainButton(
                identifier: Identifiers.retryButton,
                in: self.screen
            )

            button.yreEnsureEnabledWithTimeout(timeout: Const.retryButtonTimeInterval)
        }
        return self
    }

    @discardableResult
    func isRetryButtonDisabled() -> Self {
        XCTContext.runActivity(named: "Проверяем, что кнопка \"Отправить повторно\" не активна") { _ -> Void in
            let button = ElementsProvider.obtainButton(
                identifier: Identifiers.retryButton,
                in: self.screen
            )

            button.yreEnsureNotEnabledWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnRetryButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Продолжить\"") { _ -> Void in
            let button = ElementsProvider.obtainButton(
                identifier: Identifiers.retryButton,
                in: self.screen
            )

            button
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

    private enum Const {
        // Retry button become enable in 30 seconds, but every hour in app is 7 years on agent
        // So let's set here 60 secods waiting timeout to make test less flacky
        // TODO: Make retry button became enable in 2-3 seconds in ui tests
        // https://st.yandex-team.ru/VSAPPS-8876
        static let retryButtonTimeInterval: TimeInterval = 60.0
    }

    private lazy var screen = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var topNotificationView = ElementsProvider.obtainElement(identifier: "top.notification.view")

    private typealias Identifiers = MortgageFormAccessibilityIdentifiers.SMSConfirmation
}
