//
//  PhonePickerSteps.swift
//  UITests
//
//  Created by Alexander Malnev on 7/29/20.
//

import XCTest
import Snapshots

class PhonePickerSteps: BaseSteps {
    func onPhonePickerScreen() -> PhonePickerScreen {
        return baseScreen.on(screen: PhonePickerScreen.self)
    }

    @discardableResult
    func tapPhone(_ phone: String) -> Self {
        Step("Тапаем в телефон \(phone) из списка") {
            onPhonePickerScreen().phone(phone).tap()
        }
        return self
    }

    @discardableResult
    func tapComplete() -> Self {
        Step("Завершаем выбор телефона") {
            onPhonePickerScreen().completeButton.tap()
        }
        return self
    }
}
