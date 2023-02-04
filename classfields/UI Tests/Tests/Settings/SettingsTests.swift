//
//  SettingsTests.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 21.07.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig

final class SettingsTests: BaseTestCase {
    func testNumberRedirectSettings() {
        let profileMenu = ProfileMenuSteps()
        let settings = SettingsSteps()

        SettingsAPIStubConfigurator.setupNaturalUser(using: self.dynamicStubs)

        self.relaunchApp(with: .profileTests)

        profileMenu
            .screenIsPresented()
            .tapOnSettings()

        settings
            .screenIsPresented()
            .phoneRedirectIsExists()
            .phoneRedirectIsEnabled()
            .togglePhoneRedirect()
            .phoneRedirectIsDisabled()

        let expectation = XCTestExpectation(description: "Обновление пользователя с верными параметрами")
        SettingsAPIStubConfigurator.setupSettingUpdate(
            using: self.dynamicStubs,
            with: expectation,
            onRequest: { params in
                let redirectPhones = params["redirectPhones"] as? Bool
                XCTAssertEqual(redirectPhones, false)
            }
        )

        settings
            .closeIfPresented()

        expectation
            .yreEnsureFullFilledWithTimeout()
    }

    func disabled_testNumberRedirectSettingsForAgentUser() {
        let profileMenu = ProfileMenuSteps()
        let settings = SettingsSteps()

        SettingsAPIStubConfigurator.setupLegalUser(using: self.dynamicStubs)

        self.relaunchApp(with: .profileTests)

        profileMenu
            .screenIsPresented()
            .tapOnSettings()

        settings
            .screenIsPresented()
            .phoneRedirectIsNotExists()
    }
}

private final class SettingsAPIStubConfigurator {
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

    static func setupSettingUpdate(using dynamicStubs: HTTPDynamicStubs,
                                   with expectation: XCTestExpectation? = nil,
                                   onRequest validate: @escaping ([String: Any]) -> Void) {
        dynamicStubs.register(
            method: .PATCH,
            path: "/1.0/user",
            middleware: MiddlewareBuilder
                .chainOf([
                    .callback({ request in
                        let data = Data(request.body)
                        if let json = try? JSONSerialization.jsonObject(with: data, options: []) as? [String: Any],
                           let userJson = json["user"] as? [String: Any] {
                            validate(userJson)
                            expectation?.fulfill()
                        }
                    }),
                    .requestTime(Constants.timeout),
                    .respondWith(.internalServerError()),
                ])
                .build()
        )
    }
}
