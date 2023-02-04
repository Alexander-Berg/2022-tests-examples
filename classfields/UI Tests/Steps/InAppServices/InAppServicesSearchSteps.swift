//
//  InAppServicesSearchSteps.swift
//  UI Tests
//
//  Created by Alexey Salangin on 12.04.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import YREAccessibilityIdentifiers

final class InAppServicesSearchSteps {
    @discardableResult
    func isBlockPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие поискового блока") { _ -> Void in
            self.searchSection
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnRecentSearchPreset() -> Self {
        XCTContext.runActivity(named: #"Нажимаем на пресет "Предыдущий поиск""#) { _ -> Void in
            self.recentSearch
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnSavedSearchPreset() -> SavedSearchesListSteps {
        XCTContext.runActivity(named: #"Нажимаем на пресет "Сохранённые поиски""#) { _ -> Void in
            self.savedSearch
                .yreTap()
        }
        return SavedSearchesListSteps()
    }

    @discardableResult
    func tapOnRecentSearchBottomSheetApplyButton() -> CommonSearchResultsSteps {
        XCTContext.runActivity(named: #"Нажимаем на кнопку "Показать""#) { _ -> Void in
            let button = ElementsProvider.obtainButton(
                identifier: RecentSearchesAccessibilityIdentifiers.bottomSheetApplyButton
            )

            button
                .yreEnsureExists()
                .yreEnsureHittable()
                .yreTap()
        }
        return CommonSearchResultsSteps()
    }

    @discardableResult
    func tapOnRentPreset() -> CommonSearchResultsSteps {
        XCTContext.runActivity(named: #"Нажимаем на пресет "Аренда 2-комнатной квартиры""#) { _ -> Void in
            let cell = ElementsProvider.obtainElement(
                identifier: FiltersPresetsAccessibilityIdentifiers.presetCell(
                    type: .rentTwoRoomApartment
                ),
                in: self.searchSection
            )
            cell.yreTap()
        }
        return CommonSearchResultsSteps()
    }

    @discardableResult
    func ensureSavedSearchCellExists(_ isExists: Bool) -> Self {
        XCTContext.runActivity(named: "Проверяем, что ячейка \"Сохранённые поиски\" \(isExists ? "" : "не ")существует") { _ -> Void in
            let cell = ElementsProvider.obtainElement(identifier: Identifiers.Search.savedSearch)

            if isExists {
                cell
                    .yreEnsureExists()
            }
            else {
                cell
                    .yreEnsureNotExists()
            }
        }
        return self
    }

    @discardableResult
    func savedSearchesCountIsEqualTo(_ count: String) -> Self {
        XCTContext.runActivity(named: "Проверяем, что текст на бейдже у пресета \"Сохранённые поиски\" равен \(count)") { _ -> Void in
            let cell = ElementsProvider.obtainElement(identifier: Identifiers.Search.savedSearch)

            let badge = ElementsProvider.obtainElement(identifier: Identifiers.Search.savedSearchBadge, in: cell)
            XCTAssertEqual(badge.label, count)
        }
        return self
    }

    @discardableResult
    func ensureRecentSearchCellExists(_ isExists: Bool) -> Self {
        XCTContext.runActivity(named: "Проверяем, что ячейка \"Последний поиск\" \(isExists ? "" : "не ")существует") { _ -> Void in
            let cell = ElementsProvider.obtainElement(identifier: Identifiers.Search.recentSearch)

            if isExists {
                cell
                    .yreEnsureExists()
            }
            else {
                cell
                    .yreEnsureNotExists()
            }
        }
        return self
    }

    @discardableResult
    func openFilters() -> FiltersSteps {
        XCTContext.runActivity(named: #"Нажимаем на кнопку "Начать поиск""#) { _ -> Void in
            let button = ElementsProvider.obtainElement(identifier: Identifiers.Search.startSearch)
            button.yreTap()
        }
        return FiltersSteps()
    }

    // MARK: - Private

    private typealias Identifiers = InAppServicesAccessibilityIdentifiers

    private lazy var searchSection = ElementsProvider.obtainElement(identifier: Identifiers.Search.section)

    private lazy var recentSearch = ElementsProvider.obtainElement(
        identifier: Identifiers.Search.recentSearch,
        in: self.searchSection
    )

    private lazy var savedSearch = ElementsProvider.obtainElement(
        identifier: Identifiers.Search.savedSearch,
        in: self.searchSection
    )
}
