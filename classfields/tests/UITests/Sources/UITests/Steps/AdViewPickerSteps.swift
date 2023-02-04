//
//  AdViewPickerSteps.swift
//  UITests
//
//  Created by Alexander Malnev on 4/20/21.
//

import XCTest
import Snapshots

final class AdViewPickerSteps: BaseSteps {
    func onScreen() -> AdViewPickerScreen {
        return baseScreen.on(screen: AdViewPickerScreen.self)
    }

    func tapActivateStandard() -> PaymentOptionsSteps<AdViewPickerSteps> {
        Step("Нажимаем активацию объявления в промо") {
            onScreen().scrollTo(element: onScreen().standardActivationButton)
            onScreen().standardActivationButton.tap()
        }
        return PaymentOptionsSteps(context: context, source: self)
    }
}
