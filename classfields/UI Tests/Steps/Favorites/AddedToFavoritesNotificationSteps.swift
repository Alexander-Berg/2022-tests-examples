//
//  AddedToFavoritesNotificationSteps.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 24.03.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.FavoritesAccessibilityIdentifiers

final class AddedToFavoritesNotificationSteps {
    @discardableResult
    func screenIsPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что экран \"\(self.screenTitle)\" показан") { _ -> Void in
            self.screen.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func close() -> Self {
        XCTContext.runActivity(named: "Закрываем экран \"\(self.screenTitle)\"") { _ -> Void in
            self.button
                .yreEnsureExists()
                .yreTap()
        }

        return self
    }

    @discardableResult
    func closeIfPresented() -> Self {
        XCTContext.runActivity(named: "Закрываем экран \"\(self.screenTitle)\", если он показан") { _ -> Void in
            guard self.screen.yreWaitForExistence() else { return }

            self.button
                .yreEnsureExists()
                .yreTap()
        }

        return self
    }

    private lazy var screen = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.AddedToFavoritesNotification.view
    )
    private lazy var button = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.AddedToFavoritesNotification.button,
        in: self.screen
    )
    private let screenTitle: String = "Мы сообщим если цена изменится"

    private typealias AccessibilityIdentifiers = FavoritesAccessibilityIdentifiers
}
