//
//  WalletSteps.swift
//  UITests
//
//  Created by Alexander Malnev on 4/9/21.
//

import XCTest
import Snapshots

class WalletSteps: BaseSteps {
    func onWalletScreen() -> WalletScreen {
        return baseScreen.on(screen: WalletScreen.self)
    }

    @discardableResult
    func tapSegment(_ segment: WalletScreen.Segment) -> Self {
        step("Тапаем в сегмент \"\(segment.description)\"") {
            onWalletScreen().segmentControl(at: segment).tap()
        }
    }

    @discardableResult
    func tapPromocodeEntry() -> Self {
        step("Тапаем в поле ввода промокода") {
            onWalletScreen().promocodeInput.tap()
        }
    }

    @discardableResult
    func tapActivatePromocode() -> Self {
        step("Тапаем в кнопку активации промокода") {
            onWalletScreen().promocodeActivateButton.tap()
        }
    }

    @discardableResult
    func enterPromocodeText(_ text: String) -> Self {
        step("Вводим текст промокода: \(text)") {
            app.typeText(text)
        }
    }

    @discardableResult
    func tapClearPromocode() -> Self {
        step("Очищем поле ввода промокода") {
            onWalletScreen().promocodeClearButton.tap()
        }
    }

    @discardableResult
    func validatePromocodeInputIs(_ value: String) -> Self {
        step("Проверяем что поле ввода содержит '\(value)'") {
            XCTAssert(onWalletScreen().promocodeTextField.value as! String == value)
        }
    }

    @discardableResult
    func validateHasPromocodeSnackbar(_ exist: Bool = true) -> Self {
        step("Проверяем, что снекбар активации промокода \(exist ? "есть" : "не виден")") {
            let hudText = app.staticTexts["Промокод активирован 👌"].firstMatch
            if exist {
                hudText.shouldExist()
            } else {
                hudText.shouldNotExist()
            }
        }
    }

    @discardableResult
    func validatePromocodeActivationError(_ text: String) -> Self {
        step("Проверяем что виден текст ошибки '\(text)'") {
            app.staticTexts[text].firstMatch.shouldExist()
        }
    }

    @discardableResult
    func validatePromocodeCellScreenshot(idPrefix: String) -> Self {
        step("Проверяем скриншот ячейки промокода с id начинающимся на \(idPrefix)") {
            let cell = onWalletScreen().promocodeCell(idPrefix: idPrefix)
            cell.shouldExist()
            validateSnapshot(of: cell, snapshotId: "promocode_cell")
        }
    }

    @discardableResult
    func tapRefundWalletButton() -> PaymentOptionsSteps<WalletSteps> {
        Step("Тапаем в кнопку \"Пополнить\"") {
            onWalletScreen().refundWalletButton.tap()
        }

        return PaymentOptionsSteps(context: context, source: self)
    }
}
