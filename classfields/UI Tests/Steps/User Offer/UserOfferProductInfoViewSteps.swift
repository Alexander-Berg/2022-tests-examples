//
//  UserOfferProductInfoViewSteps.swift
//  UI Tests
//
//  Created by Erik Burygin on 15.03.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation

import XCTest
import enum YREAccessibilityIdentifiers.UserOfferProductInfoAccessibilityIdentifiers
import YRETestsUtils

final class UserOfferProductInfoViewSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что экрана с информацией о продукте показана") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func compareViewWithScreenshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем скриншот экрана с информацией о продукте с эталоном") { _ -> Void in
            let screenshot = screenView.yreWaitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
        }
        return self
    }

    @discardableResult
    func tapActivateButton() -> PaymentMethodsSteps {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Активировать'") { _ -> Void in
            self.activateButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .tap()
        }
        return PaymentMethodsSteps()
    }
 
    @discardableResult
    func tapOnCloseButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку Закрыть") { _ -> Void in
            let navigationContainer = ElementsProvider.obtainNavigationContainer()
            let closeButton = ElementsProvider.obtainBackButton(in: navigationContainer)
            closeButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }
    
    private typealias Identifiers = UserOfferProductInfoAccessibilityIdentifiers

    private lazy var screenView: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.viewIdentifier)
    private lazy var activateButton: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.activateButton)
}
