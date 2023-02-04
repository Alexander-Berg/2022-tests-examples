//
//  ProfileMenuSteps.swift
//  UITests
//
//  Created by Alexey Salangin on 1/30/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import enum YREAccessibilityIdentifiers.ProfileMenuAccessibilityIdentifiers

final class ProfileMenuSteps {
    lazy var profileMenuTable = ElementsProvider.obtainElement(
        identifier: ProfileMenuIdentifiers.table,
        type: .table
    )

    lazy var accountCell = ElementsProvider.obtainElement(
        identifier: ProfileMenuIdentifiers.accountCell,
        type: .cell,
        in: self.profileMenuTable
    )

    lazy var loginButton = ElementsProvider.obtainElement(
        identifier: ProfileMenuIdentifiers.loginButton,
        type: .button,
        in: self.accountCell
    )

    lazy var logoutButton = ElementsProvider.obtainElement(
        identifier: ProfileMenuIdentifiers.logoutButton,
        type: .button,
        in: self.accountCell
    )

    lazy var bankCardsCell = ElementsProvider.obtainElement(
        identifier: ProfileMenuIdentifiers.bankCardsCell,
        type: .cell,
        in: self.profileMenuTable
    )

    lazy var techSupportCell = ElementsProvider.obtainElement(
        identifier: ProfileMenuIdentifiers.supportCell,
        type: .cell,
        in: self.profileMenuTable
    )

    lazy var notificationSettingsCell = ElementsProvider.obtainElement(
        identifier: ProfileMenuIdentifiers.notificationSettingsCell,
        type: .cell,
        in: self.profileMenuTable
    )

    lazy var settingsCell = ElementsProvider.obtainElement(
        identifier: ProfileMenuIdentifiers.settingsCell,
        type: .cell,
        in: self.profileMenuTable
    )

    // MARK: Private

    private typealias ProfileMenuIdentifiers = ProfileMenuAccessibilityIdentifiers
}

extension ProfileMenuSteps {
    @discardableResult
    func screenIsPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана 'Профиль'") { _ -> Void in
            self.profileMenuTable.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnLoginButton() -> Self {
        self.loginButton.yreEnsureExistsWithTimeout().yreTap()
        return self
    }

    @discardableResult
    func tapOnLogoutButton() -> Self {
        self.logoutButton.yreEnsureExistsWithTimeout().yreTap()
        return self
    }

    @discardableResult
    func bankCardsCellIsPresented() -> Self {
        self.bankCardsCell.yreEnsureExistsWithTimeout()
        return self
    }

    @discardableResult
    func bankCardsCellIsDismissed() -> Self {
        self.bankCardsCell.yreEnsureNotExistsWithTimeout()
        return self
    }

    @discardableResult
    func tapOnBankCardsCell() -> Self {
        self.bankCardsCell.yreEnsureExists().yreTap()
        return self
    }

    @discardableResult
    func tapOnTechSupportChatCell() -> Self {
        XCTContext.runActivity(named: "Открываем экран чата тех поддержки") { _ -> Void in
            self.techSupportCell
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnNotificationSettings() -> Self {
        XCTContext.runActivity(named: "Открываем экран настроек уведомлений") { _ -> Void in
            self.notificationSettingsCell
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnSettings() -> Self {
        XCTContext.runActivity(named: "Открываем экран настроек") { _ -> Void in
            self.settingsCell
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }
}

extension ProfileMenuSteps {
    func makeLogoutActivity() -> ProfileMenuStepsLogoutActivity {
        return .init(self)
    }
}

final class ProfileMenuStepsLogoutActivity {
    init(_ steps: ProfileMenuSteps) {
        self.steps = steps
    }

    @discardableResult
    func submit() -> Self {
        self.isSuccess = true

        return self
    }

    @discardableResult
    func cancel() -> Self {
        self.isSuccess = false

        return self
    }

    @discardableResult
    func run() -> Self {
        let isSuccess = YREUnwrap(self.isSuccess)

        XCTContext.runActivity(named: "LogoutAlert Story", block: { _ -> Void in
            if isSuccess {
                self.performSubmit()
            }
            else {
                self.performCancel()
            }
        })

        return self
    }

    // MARK: - Private

    // @coreshock: Keep in sync with JumboStrings.rootProvider.settings.logout<...>
    private enum AlertIdentifiers {
        static let viewTitleLabel = "Предупреждение"
        static let submitButtonLabel = "Выйти"
        static let cancelButtonLabel = "Отмена"
    }

    private let steps: ProfileMenuSteps

    private var isSuccess: Bool?

    private func performSubmit() {
        let alert = XCUIApplication()
            .sheets[AlertIdentifiers.viewTitleLabel]
        alert
            .yreEnsureExistsWithTimeout()
            .buttons[AlertIdentifiers.submitButtonLabel]
            .yreTap()
    }

    private func performCancel() {
        let alert = XCUIApplication()
            .sheets[AlertIdentifiers.viewTitleLabel]
        alert
            .yreEnsureExistsWithTimeout()
            .buttons[AlertIdentifiers.cancelButtonLabel]
            .yreTap()
    }
}
