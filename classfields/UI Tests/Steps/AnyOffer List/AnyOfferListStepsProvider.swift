//
//  AnyOfferListStepsProvider.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 18.01.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

/// Convenient methods to assert the most common cases for a list of any offers (YREOffer, YRESite, YREVillage).
/// Not intended to be used directly from tests.
final class AnyOfferListStepsProvider {
    let cellsQuery: XCUIElementQuery

    init(container: XCUIElement, cellID: String) {
        self.cellsQuery = container.cells.matching(identifier: cellID)
    }

    func isListNonEmpty() {
        let firstCell = self.cellsQuery.element(boundBy: 0)
        firstCell.yreEnsureExistsWithTimeout()
    }

    func isListEmpty() {
        let firstCell = self.cellsQuery.element(boundBy: 0)
        firstCell.yreEnsureNotExistsWithTimeout()
    }

    func сell(withIndex index: Int) -> XCUIElement {
        return self.cellsQuery.element(boundBy: index)
    }
}
