//
//  PaymentOptionsSteps.swift
//  UITests
//
//  Created by Alexander Malnev on 4/13/21.
//

import XCTest
import Snapshots

final class PaymentOptionsSteps<SourceSteps>: ModalSteps<SourceSteps, PaymentOptionsScreen> {
    @discardableResult
    func tapPromocodeOption() -> PromocodeBottomsheetSteps {
        Step("Нажимаем кнопку промокода") {
            onModalScreen().enterPromocodeCell.tap()
        }
        return PromocodeBottomsheetSteps(context: context)
    }

    @discardableResult
    func validatePromocodeOptionAvailable(_ available: Bool = true) -> Self {
        step("Проверяем что опция \"Введите промокод\" \(available ? "доступна" : "не доступна")") {
            if available {
                onModalScreen().enterPromocodeCell.shouldExist()
            } else {
                onModalScreen().enterPromocodeCell.shouldNotExist()
            }
        }
    }

    @discardableResult
    func validatePaymentScreenExists() -> Self {
        step("Виден экран оплаты") {
            onModalScreen().paymentOptionsScreen.shouldExist()
        }
    }

    @discardableResult
    func tapOnPurchaseButton() -> Self {
        step("Нажимаем на кнопку оплаты") {
            onModalScreen().purchaseButton.shouldExist().tap()
        }
    }

    @discardableResult
    func checkHasActivityHud(_ text: String) -> Self {
        step("Проверяем наличие ActivityHUD с заданным текстом - \"\(text)\"") {
            let activityHud = onModalScreen().activityHUD
            activityHud.staticTexts[text].shouldExist()
        }
    }
}
