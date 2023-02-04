//
//  SiteOfferByPlanSnippetSteps.swift
//  UI Tests
//
//  Created by Fedor Solovev on 06.11.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import class YREAccessibilityIdentifiers.SiteOfferListByPlanAccessibilityIdentifiers

final class SiteOfferByPlanSnippetSteps {
    init(element: XCUIElement) {
        self.element = element
    }

    @discardableResult
    func isPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие сниппета оффера по планировке") { _ -> Void in
            self.element.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tap() -> Self {
        XCTContext.runActivity(named: "Нажимаем на сниппет оффера по планировке") { _ -> Void in
            self.element.yreTap()
        }
        return self
    }

    @discardableResult
    func isFavoritesButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие кнопки добавить в избранное") { _ -> Void in
            self.favoritesButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabled()
                .yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func tapFavoritesButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку добавить в избранное") { _ -> Void in
            self.favoritesButton.tap()
        }
        return self
    }

    @discardableResult
    func compareWithScreenshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом сниппета оффера по планировке") { _ -> Void in
            let screenshot = self.element.yreWaitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
        }
        return self
    }

    // MARK: - Private

    private typealias AccessibilityIdentifiers = SiteOfferListByPlanAccessibilityIdentifiers

    private lazy var favoritesButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.favoritesButtonIdentifier,
        type: .button,
        in: self.element
    )

    private lazy var callButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.callButtonIdentifier,
        type: .button,
        in: self.element
    )

    private let element: XCUIElement
}

extension SiteOfferByPlanSnippetSteps: CallButtonHandler {
    @discardableResult
    func tapOnCallButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Позвонить\"") { _ -> Void in
            self.callButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }
}
