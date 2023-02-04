//
//  SpotlightSteps.swift
//  UI Tests
//
//  Created by Timur Guliamov on 03.12.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest

final class SpotlightSteps {
    @discardableResult
    func present() -> Self {
        XCTContext.runActivity(named: "Открытие Spotlight") { _ -> Void in
            XCUIDevice.shared.press(.home)
            self.springboard.yreEnsureExists().swipeDown()
            self.spotlight.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func typeText(_ text: String) -> Self {
        XCTContext.runActivity(named: "Пишем \"\(text)\" в поле ввода Spotlight") { _ -> Void in
            self.spotlight.yreTypeText(text)
        }
        return self
    }

    @discardableResult
    func tapOnCellWithTitle(_ title: String, description: String? = nil) -> Self {
        XCTContext.runActivity(named: "Тапаем по ячейке с тайтлом \"\(title)\", описанием \"\(description ?? "")\"") { _ -> Void in
            let cellLabel = [title, description].compactMap { $0 }.joined(separator: ", ")

            let predicate = NSPredicate { element, _ in
                guard let element = element as? XCUIElementSnapshot else {
                    return false
                }
                return element.label == cellLabel
                    || (element.label == title && (element.value as? String) == description)
            }

            let element = self.spotlight
                .cells
                .element(matching: predicate)

            element
                .yreEnsureExistsWithTimeout()
                .yreTap()

            XCTAssertTrue(XCUIApplication().wait(for: .runningForeground, timeout: 10.0))
        }
        return self
    }

    // MARK: - Private

    private lazy var spotlight = XCUIApplication(bundleIdentifier: "com.apple.Spotlight")
    private lazy var springboard = XCUIApplication(bundleIdentifier: "com.apple.springboard")
}
