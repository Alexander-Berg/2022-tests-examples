//
//  HouseUtilitiesPaymentPopupSteps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 02.02.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREAccessibilityIdentifiers

final class HouseUtilitiesBillDeclinedPopupSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие попапа 'Счёт отклонён'") { _ -> Void in
            self.viewController.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем закрытие попапа 'Счёт отклонён'") { _ -> Void in
            self.viewController.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    func tapContinueButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Продолжить'") { _ -> Void in
            self.continueButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    private typealias AccessibilityIdentifiers = HouseUtilitiesPaymentAccessibilityIdentifiers.PaymentPopup.Declined

    private lazy var viewController = ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.view)
    private lazy var continueButton = ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.button)
}
