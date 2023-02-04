//
//  YaRentContractFinishPopupSteps.swift
//  UI Tests
//
//  Created by Evgenii Novikov on 09.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.YaRentContractAccessibilityIdentifiers

final class YaRentContractFinishPopupSteps {
    @discardableResult
    func ensureFinishPopupPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие финальной модалки на экране") { _ -> Void in
            self.view.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapFinishPopupButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку в модалке") { _ -> Void in
            self.button
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = YaRentContractAccessibilityIdentifiers

    private lazy var view = ElementsProvider.obtainElement(identifier: Identifiers.finishPopup)
    private lazy var button = ElementsProvider.obtainElement(identifier: Identifiers.finishPopupButton)
}
