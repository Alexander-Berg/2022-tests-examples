//
//  PromocodeBottomsheetSteps.swift
//  UITests
//
//  Created by Alexander Malnev on 3/25/21.
//

import XCTest
import Snapshots

final class PromocodeBottomsheetSteps: BaseSteps {
    func onPromocodeBottomsheetScreen() -> PromocodeBottomsheetScreen {
        return baseScreen.on(screen: PromocodeBottomsheetScreen.self)
    }

    @discardableResult
    func enterPromocodeText(_ text: String) -> Self {
        step("Вводим текст промокода \(text)") {
            app.typeText(text)
        }
    }

    @discardableResult
    func clearPromocodeText() -> Self {
        step("Очищаем поле ввода промокода") {
            onPromocodeBottomsheetScreen().clearButton.tap()
        }
    }

    @discardableResult
    func activatePromocode() -> Self {
        step("Жмем кнопку активации промокода") {
            onPromocodeBottomsheetScreen().activateButton.tap()
        }
    }

    @discardableResult
    func dismiss() -> Self {
        step("Закрываем ввод промокода") {
            onPromocodeBottomsheetScreen().dismissButton().tap()
        }
    }

    @discardableResult
    func validatePromocodeInputIs(_ value: String) -> Self {
        step("Проверяем что поле ввода содержит '\(value)'") {
            XCTAssert(onPromocodeBottomsheetScreen().textField.value as! String == value)
        }
    }

    @discardableResult
    func validatePromocodeActivationError(_ text: String) -> Self {
        step("Проверяем что виден текст ошибки '\(text)'") {
            app.staticTexts[text].firstMatch.shouldExist()
        }
    }
}
