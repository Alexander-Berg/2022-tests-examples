//
//  SettingsSteps.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 21.07.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import enum YREAccessibilityIdentifiers.SettingsAccessibilityIdentifiers

final class SettingsSteps {
    private lazy var view = ElementsProvider.obtainElement(identifier: Identifiers.view)

    private lazy var settingsTable = ElementsProvider.obtainElement(
        identifier: Identifiers.table,
        type: .table
    )

    private lazy var phoneRedirectCell = ElementsProvider.obtainElement(
        identifier: Identifiers.phoneRedirectCell,
        type: .cell,
        in: self.settingsTable
    )

    private lazy var phoneRedirectSwitch = ElementsProvider.obtainElement(
        identifier: Identifiers.phoneRedirectCell + ".switch",
        type: .switch,
        in: self.phoneRedirectCell
    )

    private typealias Identifiers = SettingsAccessibilityIdentifiers
}

extension SettingsSteps {
    @discardableResult
    func screenIsPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана настроек") { _ -> Void in
            self.settingsTable.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func closeIfPresented() -> Self {
        XCTContext.runActivity(named: "Закрываем экран настроек (если он открыт)") { _ -> Void in
            if let closeButton = ElementsProvider.obtainBackButtonIfExists() {
                closeButton.yreTap()
            }
        }
        return self
    }

    @discardableResult
    func phoneRedirectIsExists() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие блока настроек подменного номера") { _ -> Void in
            self.phoneRedirectCell
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func phoneRedirectIsNotExists() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутвие блока настроек подменного номера") { _ -> Void in
            self.phoneRedirectCell
                .yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func phoneRedirectIsEnabled() -> Self {
        XCTContext.runActivity(named: "Проверяем, что 'Подменный номер' включен") { _ -> Void in
            self.phoneRedirectCell.yreEnsureExistsWithTimeout()
            let switchControlValue = self.phoneRedirectSwitch
                .yreEnsureExists()
                .isOn

            XCTAssertEqual(switchControlValue, true)
        }
        return self
    }

    @discardableResult
    func phoneRedirectIsDisabled() -> Self {
        XCTContext.runActivity(named: "Проверяем, что 'Подменный номер' отключен") { _ -> Void in
            self.phoneRedirectCell.yreEnsureExistsWithTimeout()
            let switchControlValue = self.phoneRedirectSwitch
                .yreEnsureExists()
                .isOn

            XCTAssertEqual(switchControlValue, false)
        }
        return self
    }

    @discardableResult
    func togglePhoneRedirect() -> Self {
        XCTContext.runActivity(named: "Переключаем настройку подменного номера") { _ -> Void in
            self.phoneRedirectCell
                .yreEnsureExistsWithTimeout()
            self.phoneRedirectSwitch
                .yreEnsureExists()
                .yreForceTap()
        }
        return self
    }
}

extension XCUIElement {
    var isOn: Bool? {
        return (self.value as? String).map { $0 == "1" }
    }
}
