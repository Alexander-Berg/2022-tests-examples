//
//  UserOfferActivationSteps.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 21.04.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class UserOfferActivationSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что экран активации отображается") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnCloseButton() -> Self {
        XCTContext.runActivity(named: "Закрываем экран активации") { _ -> Void in
            self.closeButton.yreTap()
        }
        return self
    }

    // MARK: Private

    private struct Identifiers {
        static let screenView = "userOffers.activation"
    }

    private lazy var screenView: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.screenView)
    private lazy var closeButton: XCUIElement = ElementsProvider.obtainBackButton()
}
