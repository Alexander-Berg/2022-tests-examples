//
//  OfferPlanSnippetSteps.swift
//  UI Tests
//
//  Created by Fedor Solovev on 05.11.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import enum YREAccessibilityIdentifiers.OfferPlanListAccessibilityIdentifiers

final class OfferPlanSnippetSteps {
    init(element: XCUIElement) {
        self.element = element
    }

    @discardableResult
    func isPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие сниппета планировки") { _ -> Void in
            self.element.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tap() -> Self {
        XCTContext.runActivity(named: "Нажимаем на сниппет планировки") { _ -> Void in
            self.element.yreTap()
        }
        return self
    }

    @discardableResult
    func compareWithScreenshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом сниппета планировки") { _ -> Void in
            let screenshot = self.element.yreWaitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
        }
        return self
    }

    @discardableResult
    func isShowApartmentsButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем, что кнопка Смотреть N квартир нажимается") { _ -> Void in
            self.showApartmentsButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabled()
                .yreEnsureHittable()
        }
        return self
    }

    // MARK: - Private

    private typealias AccessibilityIdentifiers = OfferPlanListAccessibilityIdentifiers

    private let element: XCUIElement
    private lazy var showApartmentsButton = ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.showApartmentsButton)
}
