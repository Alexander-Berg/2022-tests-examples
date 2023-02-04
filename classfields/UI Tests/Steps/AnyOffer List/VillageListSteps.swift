//
//  VillageListSteps.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 18.01.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import class YREAccessibilityIdentifiers.VillageSnippetCellAccessibilityIdentifiers
import class YREAccessibilityIdentifiers.AnyOfferListAccessibilityIdentifiers

final class VillageListSteps {
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
        XCTContext.runActivity(named: "Проверяем, что список КП не пуст") { _ -> Void in
            self.listStepsProvider.isListNonEmpty()
        }
        return self
    }

    func cell(withIndex index: Int) -> VillageSnippetSteps {
        return XCTContext.runActivity(named: "Получаем элемент списка под индексом \(index)") { _ -> VillageSnippetSteps in
            let cell = self.listStepsProvider.сell(withIndex: index)
            cell.yreEnsureExistsWithTimeout(timeout: self.customTimeout)

            return VillageSnippetSteps(element: cell)
        }
    }

    // MARK: Private

    private lazy var listStepsProvider = AnyOfferListStepsProvider(
        container: self.screen,
        cellID: VillageSnippetCellAccessibilityIdentifiers.cellIdentifier
    )

    private let screen: XCUIElement
    private let customTimeout: TimeInterval
}
