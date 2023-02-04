//
//  MortgageFormSuccessSteps.swift
//  UI Tests
//
//  Created by Timur Guliamov on 02.11.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import enum YREAccessibilityIdentifiers.MortgageFormAccessibilityIdentifiers

final class MortgageFormSuccessSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана \"Успешное подтверждение\"") { _ -> Void in
            self.screen.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие экрана \"Успешное подтверждение\"") { _ -> Void in
            self.screen.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func makeScreenshot(suffix: String = #function) -> Self {
        XCTContext.runActivity(named: "Сравниваем экран \"Успешное подтверждение\" со снапшотом") { _ -> Void in
            self.screen.yreWaitAndCompareScreenshot(
                identifier: "mortgageFormSuccess" + suffix,
                ignoreEdges: XCUIApplication().yre_ignoredEdges()
            )
        }
        return self
    }

    @discardableResult
    func tapOnContinueButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Понятно\"") { _ -> Void in
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

    // MARK: - Private

    private typealias Identifiers = MortgageFormAccessibilityIdentifiers.Success

    private lazy var screen = ElementsProvider.obtainElement(identifier: Identifiers.view)
}
