//
//  HouseUtilitiesDeclinedBillSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 21.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import enum YREAccessibilityIdentifiers.HouseUtilitiesOwnerBillFormAccessibilityIdentifiers
import XCTest

final class HouseUtilitiesDeclinedBillSteps {
    @discardableResult
    func ensureScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем показ экран отклоненного счёта") { _ -> Void in
            self.view.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapRebillButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку `отправить заново` на экране отклоненного счёта") { _ -> Void in
            self.rebillButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = HouseUtilitiesOwnerBillFormAccessibilityIdentifiers.Declined

    private lazy var view = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var rebillButton = ElementsProvider.obtainElement(
        identifier: Identifiers.rebillButton,
        in: self.view
    )
}
