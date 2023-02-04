//
//  RecentSearchesSteps.swift
//  UI Tests
//
//  Created by Alexey Salangin on 3/22/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YRETestsUtils
import YREAccessibilityIdentifiers

final class RecentSearchesSteps {
    init(recentSearchesCell: XCUIElement) {
        self.recentSearchesCell = recentSearchesCell
    }

    @discardableResult
    func isCellsCountEqual(to count: Int) -> Self {
        XCTContext.runActivity(named: "Проверяем, что количество сохранённых поисков равно \(count)") { _ -> Void in
            let cell = ElementsProvider.obtainElement(
                identifier: RecentSearchesAccessibilityIdentifiers.recentSearchCell(index: count - 1),
                type: .cell,
                in: self.recentSearchesCell
            )
            self.scrollToCellIfNeeded(cell, swipeLimits: 5)
            cell.yreEnsureExistsWithTimeout()

            let nextCell = ElementsProvider.obtainElement(
                identifier: RecentSearchesAccessibilityIdentifiers.recentSearchCell(index: count),
                type: .cell,
                in: self.recentSearchesCell
            )
            self.scrollToCellIfNeeded(nextCell)
            nextCell.yreEnsureNotExists()
        }

        return self
    }
    @discardableResult
    func tapOnSearchCell(index: Int) -> Self {
        XCTContext.runActivity(named: "Нажимаем на сохранённый поиск с индексом \(index)") { _ -> Void in
            let cell = ElementsProvider.obtainElement(
                identifier: RecentSearchesAccessibilityIdentifiers.recentSearchCell(index: index),
                type: .cell,
                in: self.recentSearchesCell
            )

            self.scrollToCellIfNeeded(cell)

            cell
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnSearchMoreButton(index: Int) -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку ... у сохранённого поиска с индексом \(index)") { _ -> Void in
            let cell = ElementsProvider.obtainElement(
                identifier: RecentSearchesAccessibilityIdentifiers.recentSearchCell(index: index),
                type: .cell,
                in: self.recentSearchesCell
            )

            self.scrollToCellIfNeeded(cell)

            cell
                .yreEnsureExistsWithTimeout()

            let moreButton = ElementsProvider.obtainButton(
                identifier: RecentSearchesAccessibilityIdentifiers.moreButton,
                in: cell
            )
            moreButton
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func makeScreenshot() -> Self {
        self.recentSearchesCell.yreWaitAndCompareScreenshot(identifier: "recentSearches")
        return self
    }

    @discardableResult
    func makeBottomSheetScreenshot() -> Self {
        let bottomSheet = ElementsProvider.obtainElement(identifier: RecentSearchesAccessibilityIdentifiers.bottomSheet)
        bottomSheet.yreWaitAndCompareScreenshot(identifier: "bottomSheet")
        return self
    }

    @discardableResult
    func tapOnBottomSheetApplyButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Показать\"") { _ -> Void in
            let button = ElementsProvider.obtainButton(
                identifier: RecentSearchesAccessibilityIdentifiers.bottomSheetApplyButton
            )

            button
                .yreEnsureExists()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    private let recentSearchesCell: XCUIElement

    private func scrollToCellIfNeeded(_ element: XCUIElement, swipeLimits: UInt = 2) {
        // Don't swipe if the cells count < 2, because then swipe will not occur and tap on cell will be triggered.
        if self.recentSearchesCell.cells.count > 1 {
            self.recentSearchesCell.scrollToElement(element: element, direction: .left, swipeLimits: swipeLimits)
            self.recentSearchesCell.scrollToElement(element: element, direction: .right, swipeLimits: swipeLimits)
        }
    }
}
