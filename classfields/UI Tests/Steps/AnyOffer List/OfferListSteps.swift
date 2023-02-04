//
//  OfferListSteps.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 18.01.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import class YREAccessibilityIdentifiers.OfferSnippetCellAccessibilityIdentifiers
import class YREAccessibilityIdentifiers.AnyOfferListAccessibilityIdentifiers

final class OfferListSteps {
    init(screen: XCUIElement, customTimeout: TimeInterval? = nil) {
        self.screen = screen
        self.customTimeout = customTimeout ?? Constants.timeout
    }

    convenience init(screenID: String, customTimeout: TimeInterval? = nil) {
        let element = ElementsProvider.obtainElement(identifier: screenID)
        self.init(screen: element, customTimeout: customTimeout)
    }

    static func mainList() -> Self {
        return self.init(screenID: AnyOfferListAccessibilityIdentifiers.mainList)
    }

    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что список открыт") { _ -> Void in
            self.screen.yreEnsureExistsWithTimeout()
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
            cell.yreEnsureExistsWithTimeout(timeout: self.customTimeout)

            return OfferSnippetSteps(element: cell)
        }
    }

    func filterPromoBanner(kind: FilterPromoKind) -> FilterPromoBannerSteps {
        return FilterPromoBannerSteps(kind: kind, in: self.screen)
    }

    // MARK: Private

    private lazy var listStepsProvider = AnyOfferListStepsProvider(
        container: self.screen,
        cellID: OfferSnippetCellAccessibilityIdentifiers.cell
    )
    private let screen: XCUIElement
    private let customTimeout: TimeInterval
}
