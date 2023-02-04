//
//  YaRentContractManagerCommentsSteps.swift
//  UI Tests
//
//  Created by Evgenii Novikov on 24.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.YaRentContractAccessibilityIdentifiers

final class YaRentContractManagerCommentsSteps {
    @discardableResult
    func ensurePresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие комментариев менеджера на экране") { _ -> Void in
            self.contentView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnBackButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку") { _ -> Void in
            self.closeButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = YaRentContractAccessibilityIdentifiers.ManagerComments

    private lazy var contentView = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var closeButton = ElementsProvider.obtainElement(identifier: Identifiers.closeButton)
}
