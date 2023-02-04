//
//  SiteSnippetSteps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 15.06.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import class YREAccessibilityIdentifiers.SiteSnippetCellAccessibilityIdentifiers
import YRETestsUtils

final class SiteSnippetSteps {
    init(element: XCUIElement) {
        self.element = element
        self.actionsProvider = AnySnippetStepsProvider(cell: element)
    }

    @discardableResult
    func isPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие сниппета ЖК") { _ -> Void in
            self.element.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func callButtonLabelStarts(with prefix: String) -> Self {
        XCTContext.runActivity(named: "Проверяем, что название кнопки звонка имеет префикс \"\(prefix)\"") { _ -> Void in
            self.actionsProvider.callButton(self.callButton, labelStartsWith: prefix)
        }
        return self
    }

    @discardableResult
    func isCallButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем доступность кнопки \"Позвонить\"") { _ -> Void in
            self.actionsProvider.isCallButtonTappable(self.callButton)
        }
        return self
    }

    @discardableResult
    func tap() -> Self {
        XCTContext.runActivity(named: "Нажимаем на сниппет ЖК") { _ -> Void in
            self.element.yreTap()
        }
        return self
    }

    @discardableResult
    func compareWithScreenshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом сниппета ЖК") { _ -> Void in
            let screenshot = self.element.yreWaitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
        }
        return self
    }

    @discardableResult
    func tapOnFavoriteButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Добавить в избранное' на сниппете ЖК") { _ -> Void in
            self.favoriteButton.tap()
        }
        return self
    }

    // MARK: - Private

    private let element: XCUIElement
    private let actionsProvider: AnySnippetStepsProvider

    private lazy var callButton = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.callButtonIdentifier,
        type: .button,
        in: self.element
    )

    private lazy var favoriteButton = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.favoriteButtonIdentifier,
        type: .button,
        in: self.element
    )

    private typealias AccessibilityIdentifiers = SiteSnippetCellAccessibilityIdentifiers
}

extension SiteSnippetSteps: CallButtonHandler {
    @discardableResult
    func tapOnCallButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Позвонить' на сниппете ЖК") { _ -> Void in
            self.callButton.tap()
        }
        return self
    }
}
