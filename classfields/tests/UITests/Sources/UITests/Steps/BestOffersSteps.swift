//
//  BestOffersSteps.swift
//  UITests
//
//  Created by Alexander Malnev on 7/20/20.
//

import Foundation
import XCTest
import Snapshots

class BestOffersSteps: BaseSteps {
    func onBestOffersScreen() -> BestOffersScreen {
        return baseScreen.on(screen: BestOffersScreen.self)
    }

    @discardableResult
    func tapTradeIn() -> Self {
        Step("Тапаем в свитч трейд-ина") {
            onBestOffersScreen().tradeInSwitcher().tap()
        }
        return self
    }

    @discardableResult
    func tapCredit() -> Self {
        Step("Тапаем в свитч кредита") {
            onBestOffersScreen().creditSwitcher().tap()
        }
        return self
    }

    @discardableResult
    func tapPhone() -> Self {
        Step("Тапаем в телефон") {
            onBestOffersScreen().phone().tap()
        }
        return self
    }

    @discardableResult
    func tapSendRequest() -> Self {
        Step("Тапаем в кнопку отправки заявки") {
            let button = onBestOffersScreen().requestButton()
            let coordinate = button.coordinate(withNormalizedOffset: CGVector(dx: 0.0, dy: 0.0))
            coordinate.tap()
        }
        return self
    }

    @discardableResult
    func tapDismiss() -> Self {
        Step("Закрываем модалку") {
            let button = onBestOffersScreen().dismissButton()
            button.tap()
        }
        return self
    }
}
