//
//  FiltersDeveloperPickerSteps.swift
//  UI Tests
//
//  Created by Aleksey Gotyanov on 10/23/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREAccessibilityIdentifiers

final class FiltersSuggestsListPickerSteps {
    
    @discardableResult
    func enter(developerName: String) -> Self {
        XCTContext.runActivity(named: "Вводим название застройщика в строку поиска") { _ in
            let textField = ElementsProvider.obtainElement(
                identifier: AccessibilityIdentifiers.searchTextFieldIdentifier,
                type: .textField,
                in: self.filtersDeveloperPickerVC
            )

            textField
                .yreEnsureExists()
                .yreTap()
                .yreTypeText(developerName)
        }

        return self
    }

    @discardableResult
    func tapOnSearchResult(_ name: String) -> Self {
        XCTContext.runActivity(named: "Тапаем на найденного застройщика") { _ in
            let cell = ElementsProvider.obtainElement(
                identifier: name,
                type: .staticText,
                in: self.filtersDeveloperPickerVC
            )

            cell.yreEnsureExistsWithTimeout()
                .yreTap()
        }

        return self
    }

    @discardableResult
    func clearSelectedValue() -> Self {
        XCTContext.runActivity(named: "Удаляем выбранного застройщика") { _ in
            let clearButton = ElementsProvider.obtainButton(identifier: AccessibilityIdentifiers.removeButtonIdentifier)

            clearButton
                .yreEnsureExists()
                .yreTap()
        }

        return self
    }

    // MARK: Private

    private typealias AccessibilityIdentifiers = FiltersSuggestsListPickerAccessibilityIdentifiers

    private let filtersDeveloperPickerVC = ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.moduleIdentifier)
}
