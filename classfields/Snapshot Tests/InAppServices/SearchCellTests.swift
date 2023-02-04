//
//  SearchCellTests.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 20.05.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest
@testable import YREInAppServicesModule
import YREModel
import YRELegacyFiltersCore
import YRESettings

final class SearchCellTests: XCTestCase {
    func testSearchCellLayoutWith0SavedSearchUpdates() {
        XCTContext.runActivity(named: "Сохранённые поиски без бейджа") { _ in
            let section = self.generator.makeSearchSection(
                latestSearch: YREMutableSearch(),
                savedSearchesUpdatesCount: 0,
                hasSavedSearches: true
            )
            self.assertLayout(section: section)
        }
    }

    func testSearchCellLayoutWithFrom1To9SavedSearchUpdates() {
        XCTContext.runActivity(named: "Сохранённые поиски с количеством 1-9") { _ in
            let section = self.generator.makeSearchSection(
                latestSearch: YREMutableSearch(),
                savedSearchesUpdatesCount: 1,
                hasSavedSearches: true
            )
            self.assertLayout(section: section)
        }
    }

    func testSearchCellLayoutWithFrom10To99SavedSearchUpdates() {
        XCTContext.runActivity(named: "Сохранённые поиски с количеством 10-99") { _ in
            let section = self.generator.makeSearchSection(
                latestSearch: YREMutableSearch(),
                savedSearchesUpdatesCount: 39,
                hasSavedSearches: true
            )
            self.assertLayout(section: section)
        }
    }

    func testSearchCellLayoutWith100PlusSavedSearchUpdates() {
        XCTContext.runActivity(named: "Сохранённые поиски с количеством 100+") { _ in
            let section = self.generator.makeSearchSection(
                latestSearch: YREMutableSearch(),
                savedSearchesUpdatesCount: 567,
                hasSavedSearches: true
            )
            self.assertLayout(section: section)
        }
    }

    private lazy var generator: InAppServiceSectionGenerator = {
        let presetFactory = SearchPresetViewModelFactory(
            mainFilterTransformerFactory: FilterTransformerFactory(),
            businessSettings: YREBusinessSettings()
        )
        let generator = InAppServiceSectionGenerator(presetFactory: presetFactory)
        return generator
    }()

    private func assertLayout(section: InAppServicesSection, function: String = #function) {
        guard case let .search(viewModel) = section.type else { return XCTFail("Incorrect section.") }
        let cell = self.cell(with: viewModel)
        self.assertSnapshot(cell, function: function)
    }

    private func cell(with viewModel: SearchCell.ViewModel) -> UIView {
        let frame = Self.frame(by: { SearchCell.size(width: $0, viewModel: viewModel).height })
        let cell = SearchCell(frame: frame)
        cell.configure(with: viewModel)
        return cell
    }
}
