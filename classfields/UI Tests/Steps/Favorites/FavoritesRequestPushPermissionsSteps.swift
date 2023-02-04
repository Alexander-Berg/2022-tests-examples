//
//  FavoritesRequestPushPermissionsSteps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 06.03.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.FavoritesAccessibilityIdentifiers

final class FavoritesRequestPushPermissionsSteps {
    @discardableResult
    func screenIsPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что экран \"\(self.screenTitle)\" показан") { _ -> Void in
            self.viewController.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func close() -> Self {
        XCTContext.runActivity(named: "Закрываем экран \"\(self.screenTitle)\"") { _ -> Void in
            let closeButton = ElementsProvider.obtainElement(identifier: "navigation.closeButton", in: self.viewController)
            closeButton
                .yreEnsureExists()
                .yreTap()
        }

        return self
    }

    @discardableResult
    func closeIfPresented() -> Self {
        XCTContext.runActivity(named: "Закрываем экран \"\(self.screenTitle)\", если он показан") { _ -> Void in
            guard self.viewController.yreWaitForExistence() else { return }

            let closeButton = ElementsProvider.obtainElement(identifier: "navigation.closeButton", in: self.viewController)
            closeButton
                .yreEnsureExists()
                .yreTap()
        }

        return self
    }

    private lazy var viewController = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.requestPushPermissionsViewIdentifier
    )
    private let screenTitle: String = "Разрешите пуш-уведомления"

    private typealias AccessibilityIdentifiers = FavoritesAccessibilityIdentifiers
}
