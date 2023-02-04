//
//  TinkoffPaymentsSteps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 13.12.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import enum YREAccessibilityIdentifiers.TinkoffPaymentsAccessibilityIdentifiers

final class TinkoffPaymentsSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана оплаты 'Tinkoff'") { _ -> Void in
            self.view
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func payWithSuccess() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку с успешной оплатой") { _ -> Void in
            self.successButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func payWithError() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку с неудачной оплатой") { _ -> Void in
            self.errorButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = TinkoffPaymentsAccessibilityIdentifiers

    private lazy var view = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var successButton = ElementsProvider.obtainButton(identifier: Identifiers.successButton)
    private lazy var errorButton = ElementsProvider.obtainButton(identifier: Identifiers.errorButton)
}
