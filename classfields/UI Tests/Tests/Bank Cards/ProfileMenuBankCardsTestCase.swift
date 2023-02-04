//
//  Created by Alexey Aleshkov on 16/04/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig

final class ProfileMenuBankCardsTestCase: BaseTestCase {
    func testMenuItemVisibilityForAuthorizedNaturalPerson() {
        UserAPIStubConfigurator.setupNaturalUser(using: self.dynamicStubs)
        self.relaunchApp(with: .bankCardsUITests)

        let profileMenu = ProfileMenuSteps()
        profileMenu
            .screenIsPresented()
            .bankCardsCellIsPresented()
    }

    func testMenuItemVisibilityForAuthorizedLegalEntity() {
        UserAPIStubConfigurator.setupLegalUser(using: self.dynamicStubs)
        self.relaunchApp(with: .bankCardsUITests)

        let profileMenu = ProfileMenuSteps()
        profileMenu
            .screenIsPresented()
            .bankCardsCellIsDismissed()
    }

    func testMenuItemVisibilityForNonAuthorizedUser() {
        let config: ExternalAppConfiguration = .bankCardsUITests
        config.isAuthorized = false
        self.relaunchApp(with: config)

        let profileMenu = ProfileMenuSteps()
        profileMenu
            .screenIsPresented()
            .bankCardsCellIsDismissed()
    }

    // Disabled.
    // TODO @pavelcrane: Figure out how to handle auth state without direct usage of YandexAccountManager.
    func disabledTestMenuItemVisibilityForAuthorizedUser() {
        let profileMenu = ProfileMenuSteps()
        let accountManager = YandexAccountManagerSteps()
        let passwordDialog = SystemDialogs.makePasswordActivity(self)

        self.relaunchApp(with: .bankCardsUITests)

        profileMenu
            .screenIsPresented()

        profileMenu
            .tapOnLogoutButton()

        profileMenu.makeLogoutActivity()
            .submit()
            .run()

        // login with natural user

        UserAPIStubConfigurator.setupNaturalUser(using: self.dynamicStubs)

        profileMenu
            .tapOnLoginButton()

        passwordDialog
            .activate()

        accountManager.makeActivity()
            .fillForm(login: Constants.userLogin, password: Constants.userPassword)
            .run()

        passwordDialog
            .tapOnButton(.disallow)
            .deactivate()

        profileMenu
            .bankCardsCellIsPresented()

        profileMenu
            .tapOnLogoutButton()

        profileMenu.makeLogoutActivity()
            .submit()
            .run()

        profileMenu
            .bankCardsCellIsDismissed()

        // login with legal user

        UserAPIStubConfigurator.setupLegalUser(using: self.dynamicStubs)

        profileMenu
            .tapOnLoginButton()

        accountManager.makeActivity()
            .fillForm(login: Constants.userLogin, password: Constants.userPassword)
            .run()

        profileMenu
            .bankCardsCellIsDismissed()

        profileMenu
            .tapOnLogoutButton()

        profileMenu.makeLogoutActivity()
            .submit()
            .run()

        profileMenu
            .bankCardsCellIsDismissed()
    }

    // MARK: Private

    private enum Constants {
        static let userLogin = "realtytest"
        static let userPassword = "qwerty099"

        static let timeout: TimeInterval = UserAPIStubConfigurator.Constants.timeout + 1
    }
}

private final class UserAPIStubConfigurator {
    enum Constants {
        static let timeout: TimeInterval = 0.5
    }

    static func setupNaturalUser(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/1.0/user",
            filename: "profileMenu-user-natural.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupLegalUser(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/1.0/user",
            filename: "profileMenu-user-legal.debug",
            requestTime: Constants.timeout
        )
    }
}
