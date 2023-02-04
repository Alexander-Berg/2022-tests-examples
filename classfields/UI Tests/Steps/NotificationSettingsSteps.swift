//
//  NotificationSettingsSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 19.03.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.NotificationSettingsAccessibilityIdentifiers

final class NotificationSettingsSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана настроек уведомлений") { _ -> Void in
            self.view.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func enablePushNotificationsIfNeeded() -> Self {
        XCTContext.runActivity(named: "Включаем пуши, если нужно") { _ -> Void in
            if self.allowNotificationsView.yreWaitForExistence() {
                self.allowNotificationsButton
                    .yreEnsureExists()
                    .yreTap()
            }
        }
        return self
    }

    @discardableResult
    func tapBackButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку Назад") { _ -> Void in
            ElementsProvider.obtainBackButton()
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnSavedSearchesCell() -> Self {
        XCTContext.runActivity(named: "Нажимаем на ячейку 'Новые объявления в подписках'") { _ -> Void in
            self.tapOnCell(section: 1, row: 0)
        }
        return self
    }

    private typealias Identifiers = NotificationSettingsAccessibilityIdentifiers

    private lazy var view = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var allowNotificationsView = ElementsProvider.obtainElement(
        identifier: Identifiers.allowNotificationsView,
        in: self.view
    )
    private lazy var allowNotificationsButton = ElementsProvider.obtainElement(
        identifier: Identifiers.allowNotificationsButton,
        in: self.allowNotificationsView
    )

    private func tapOnCell(section: Int, row: Int) {
        let identifier = Identifiers.cell(section: section, row: row)
        ElementsProvider.obtainElement(identifier: identifier)
            .yreEnsureExistsWithTimeout()
            .yreTap()
    }
}
