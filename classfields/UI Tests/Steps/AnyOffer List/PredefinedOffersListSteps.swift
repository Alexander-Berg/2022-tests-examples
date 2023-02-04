//
//  PredefinedOffersListSteps.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 21.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.PredefinedOfferListAccessibilityIdentifiers
import class YREAccessibilityIdentifiers.OfferSnippetCellAccessibilityIdentifiers

final class PredefinedOffersListSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что список открыт") { _ -> Void in
            self.screen.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что список не открыт") { _ -> Void in
            self.screen.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isListNonEmpty() -> Self {
        XCTContext.runActivity(named: "Проверяем, что список офферов не пуст") { _ -> Void in
            self.listStepsProvider.isListNonEmpty()
        }
        return self
    }

    @discardableResult
    func isListEmpty() -> Self {
        XCTContext.runActivity(named: "Проверяем, что список офферов пуст") { _ -> Void in
            self.listStepsProvider.isListEmpty()
        }
        return self
    }

    func cell(withIndex index: Int) -> OfferSnippetSteps {
        return XCTContext.runActivity(named: "Получаем элемент списка под индексом \(index)") { _ -> OfferSnippetSteps in
            let cell = self.listStepsProvider.сell(withIndex: index)
            cell.yreEnsureExistsWithTimeout()

            return OfferSnippetSteps(element: cell)
        }
    }

    @discardableResult
    func tapOnCloseButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку закрытия") { _ -> Void in
            self.closeButton.yreEnsureExists().yreTap()
        }
        return self
    }

    // MARK: Private

    private lazy var listStepsProvider = AnyOfferListStepsProvider(
        container: self.screen,
        cellID: OfferSnippetCellAccessibilityIdentifiers.cell
    )

    private lazy var screen: XCUIElement = ElementsProvider.obtainElement(
        identifier: PredefinedOfferListAccessibilityIdentifiers.view
    )

    private lazy var closeButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: PredefinedOfferListAccessibilityIdentifiers.closeButton
    )
}
