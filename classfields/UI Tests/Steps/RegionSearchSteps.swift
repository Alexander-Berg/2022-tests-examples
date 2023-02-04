//
//  RegionSearchSteps.swift
//  UI Tests
//
//  Created by Timur Guliamov on 05.05.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest

final class RegionSearchSteps {
    @discardableResult
    func isScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие экрана выбора региона") { _ -> Void in
            ElementsProvider.obtainElement(identifier: Identifiers.regionSearch)
                .yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func changeRegionToFirst() -> Self {
        XCTContext.runActivity(named: "Выбираем первый регион из списка") { _ -> Void in
            let view = ElementsProvider.obtainElement(identifier: Identifiers.regionSearch)
                .yreEnsureExists()
            ElementsProvider.obtainElement(identifier: Identifiers.regionSearchTable, type: .table, in: view)
                .cells.element(boundBy: 1) // Second because first cell - current region.
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func changeRegion(rgid: String) -> Self {
        XCTContext.runActivity(named: "Выбираем регион с идентификатором \"\(rgid)\"") { _ -> Void in
            let view = ElementsProvider.obtainElement(identifier: Identifiers.regionSearch)
                .yreEnsureExists()
            ElementsProvider.obtainElement(identifier: Identifiers.regionCellPrefix + rgid, type: .cell, in: view)
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    // MARK: Private

    private enum Identifiers {
        static let regionSearch = "geoIntent.regionSearch"
        static let regionSearchTable = "geoIntent.regionSearch.list"
        static let regionCellPrefix = "geoIntent.regionsList.cell."
    }
}
