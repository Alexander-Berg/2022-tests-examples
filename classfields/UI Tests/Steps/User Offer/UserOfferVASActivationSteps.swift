//
//  UserOfferVASActivationSteps.swift
//  UI Tests
//
//  Created by Erik Burygin on 20.05.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.UserOfferVASActivationAccessibilityIdentifiers
import YRETestsUtils

final class UserOfferVASActivationSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что ботомшет с активацией автопродления показан") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapAcceptButton() -> UserOffersListSteps {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Активировать'") { _ -> Void in
            self.acceptButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .tap()
        }
        return UserOffersListSteps()
    }

    private typealias Identifiers = UserOfferVASActivationAccessibilityIdentifiers

    private lazy var screenView: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var acceptButton: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.acceptButton)
}
