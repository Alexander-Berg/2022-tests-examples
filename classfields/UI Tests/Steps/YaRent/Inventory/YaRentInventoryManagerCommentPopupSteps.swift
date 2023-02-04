//
//  YaRentInventoryManagerCommentPopupSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 09.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.YaRentInventoryAccessibilityIdentifiers

final class YaRentInventoryManagerCommentPopupSteps {
    @discardableResult
    func ensureScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие попапа комментария менеджера") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func dismissScreen() -> Self {
        XCTContext.runActivity(named: "Закрываем попап тапом") { _ -> Void in
            XCUIApplication().yreTap()
        }
        return self
    }

    private typealias Identifiers = YaRentInventoryAccessibilityIdentifiers.ManagerCommentPopup

    private lazy var screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)
}
