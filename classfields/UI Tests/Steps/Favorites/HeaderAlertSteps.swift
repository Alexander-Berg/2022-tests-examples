//
//  FavouritesHeaderSteps.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 13.07.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import YREAccessibilityIdentifiers

class HeaderAlertSteps {
    init(element: XCUIElement) {
        self.element = element
    }

    @discardableResult
    func isPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие уведомления над списком избранного") { _ -> Void in
            self.element.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isHittable() -> Self {
        XCTContext.runActivity(named: "Проверяем доступность нажатия на уведомление") { _ -> Void in
            self.element.yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func isActionButtonHittable() -> Self {
        XCTContext.runActivity(named: "Проверяем доступность нажатия на кнопку внутри уведомления") { _ -> Void in
            self.actionButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func ensureButtonLabelHasText(_ text: String? = nil) -> Self {
        let label = self.actionButton.label
        XCTAssertNotNil(label)
        XCTAssertNotEqual(label, "")
        if let text = text {
            XCTAssertEqual(text, label)
        }
        return self
    }

    @discardableResult
    func compareWithScreenshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом уведомления") { _ -> Void in
            let screenshot = self.element.yreWaitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
        }
        return self
    }

    private lazy var actionButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: FavoritesListAccessibilityIdentifiers.headerAlertAction,
        type: .button,
        in: self.element
    )

    private let element: XCUIElement
}
