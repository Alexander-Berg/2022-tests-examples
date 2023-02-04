//
//  SiteListSteps.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 18.01.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import class YREAccessibilityIdentifiers.SiteSnippetCellAccessibilityIdentifiers
import class YREAccessibilityIdentifiers.AnyOfferListAccessibilityIdentifiers

final class SiteListSteps {
    convenience init(
        screenID: String = AnyOfferListAccessibilityIdentifiers.mainList,
        customTimeout: TimeInterval? = nil
    ) {
        let screen = ElementsProvider.obtainElement(identifier: screenID)
        self.init(screen: screen, customTimeout: customTimeout)
    }

    init(screen: XCUIElement, customTimeout: TimeInterval? = nil) {
        self.screen = screen
        self.customTimeout = customTimeout ?? Constants.timeout
    }

    @discardableResult
    func isListNonEmpty() -> Self {
        XCTContext.runActivity(named: "Проверяем, что список ЖК не пуст") { _ -> Void in
            self.listStepsProvider.isListNonEmpty()
        }
        return self
    }

    @discardableResult
    func isListEmpty() -> Self {
        XCTContext.runActivity(named: "Проверяем, что список ЖК пуст") { _ -> Void in
            self.listStepsProvider.isListEmpty()
        }
        return self
    }

    func cell(withIndex index: Int) -> SiteSnippetSteps {
        return XCTContext.runActivity(named: "Получаем элемент списка под индексом \(index)") { _ -> SiteSnippetSteps in
            let cell = self.listStepsProvider.сell(withIndex: index)
            cell.yreEnsureExistsWithTimeout(timeout: self.customTimeout)

            return SiteSnippetSteps(element: cell)
        }
    }

    // MARK: Private
    
    private lazy var listStepsProvider = AnyOfferListStepsProvider(
        container: self.screen,
        cellID: SiteSnippetCellAccessibilityIdentifiers.cellIdentifier
    )

    private let screen: XCUIElement
    private let customTimeout: TimeInterval
}
