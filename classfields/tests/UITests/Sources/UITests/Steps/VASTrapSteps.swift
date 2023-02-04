//
//  VASTrapSteps.swift
//  UITests
//
//  Created by Alexander Malnev on 4/19/21.
//

import XCTest
import Snapshots

final class VASTrapSteps: BaseSteps {
    func onScreen() -> VASTrapScreen {
        return baseScreen.on(screen: VASTrapScreen.self)
    }

    func tapPurchaseVASButton() -> PaymentOptionsSteps<VASTrapSteps> {
        Step("Нажимаем разместить") {
            onScreen().scrollTo(element: onScreen().purchaseVASButton)
            onScreen().purchaseVASButton.tap()
        }
        return PaymentOptionsSteps(context: context, source: self)
    }
}
