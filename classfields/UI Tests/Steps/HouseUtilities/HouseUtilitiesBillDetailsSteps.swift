//
//  HouseUtilitiesBillDetailsSteps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 26.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import enum YREAccessibilityIdentifiers.HouseUtilitiesPaymentAccessibilityIdentifiers
import XCTest

final class HouseUtilitiesBillDetailsSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана 'Счёт за коммуналку'") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isContentPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что содержимое экрана отображается") { _ -> Void in
            self.contentView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isLoadingFinished() -> Self {
        XCTContext.runActivity(named: "Проверяем, что загрузка контента завершилась") { _ -> Void in
            self.loadingView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapPayButton() -> PaymentMethodsSteps {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Оплатить'") { _ -> Void in
            self.payButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return PaymentMethodsSteps()
    }

    @discardableResult
    func tapDeclineButton() -> TextPickerSteps {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Отклонить'") { _ -> Void in
            self.declineButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return TextPickerSteps(name: "Отклонение счёта")
    }

    private typealias Identifiers = HouseUtilitiesPaymentAccessibilityIdentifiers.BillDetails

    private lazy var screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var contentView = ElementsProvider.obtainElement(identifier: Identifiers.contentView)
    private lazy var loadingView = ElementsProvider.obtainElement(identifier: Identifiers.loadingView)
    private lazy var payButton = ElementsProvider.obtainButton(
        identifier: Identifiers.payButton,
        in: self.screenView
    )
    private lazy var declineButton = ElementsProvider.obtainButton(
        identifier: Identifiers.declineButton,
        in: self.screenView
    )
}
