//
//  PaymentMethodsSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 24.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import enum YREAccessibilityIdentifiers.PaymentMethodsAccessibilityIdentifiers

final class PaymentMethodsSteps {
    @discardableResult
    func isPaymentMethodsScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана выбора метода оплаты") { _ -> Void in
            self.viewController.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func ensureTermsLabelNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие ссылки на условия оказания услуг") { _ -> Void in
            self.termsLabel
                .yreEnsureNotVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func pay() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Оплатить'") { _ -> Void in
            self.payButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapTermsLink() -> Self {
        XCTContext.runActivity(named: "Нажимаем на ссылку условий оказания услуг") { _ -> Void in
            self.termsLabel
                .yreEnsureExistsWithTimeout()
                .links["Условиями оказания услуг"]
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = PaymentMethodsAccessibilityIdentifiers

    private lazy var viewController = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var payButton = ElementsProvider.obtainButton(identifier: Identifiers.payButton)
    private lazy var termsLabel = ElementsProvider.obtainElement(identifier: Identifiers.termsLabel)
}
