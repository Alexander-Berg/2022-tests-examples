//
//  InAppServiceSectionGeneratorTests.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 20.05.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
@testable import YREInAppServicesModule
import XCTest
import YRESettings
import YRELegacyFiltersCore
import YREModel
import YREAccessibilityIdentifiers
import YREDesignKit

final class InAppServiceSectionGeneratorTests: XCTestCase {
    func testSearchSection() {
        let presetFactory = SearchPresetViewModelFactory(
            mainFilterTransformerFactory: FilterTransformerFactory(),
            businessSettings: YREBusinessSettings()
        )
        
        let generator = InAppServiceSectionGenerator(presetFactory: presetFactory)

        func checkOnlyPresetsSection(_ section: InAppServicesSection) {
            if case let .search(presets) = section.type {
                let identifiers = presets.map(\.accessibilityIdentifier)
                let expectedIdentifiers = [
                    FiltersPresetsAccessibilityIdentifiers.presetCell(type: .rentTwoRoomApartment),
                    FiltersPresetsAccessibilityIdentifiers.presetCell(type: .buyApartmentInSite),
                    FiltersPresetsAccessibilityIdentifiers.presetCell(type: .buySingleRoomApartment),
                ]

                XCTAssertEqual(identifiers, expectedIdentifiers)
            }
            else {
                XCTFail("Некорректный тип секции.")
            }
        }

        XCTContext.runActivity(named: "Только пресеты и сохранённые поиски") { _ in
            // Don't show saved searches without recent searches
            // https://st.yandex-team.ru/VSAPPS-9905#624c5b4fd955364cc085657f
            let section = generator.makeSearchSection(latestSearch: nil, savedSearchesUpdatesCount: 5, hasSavedSearches: true)
            checkOnlyPresetsSection(section)
        }

        XCTContext.runActivity(named: "Только пресеты и недавние поиски") { _ in
            let section = generator.makeSearchSection(
                latestSearch: YREMutableSearch(),
                savedSearchesUpdatesCount: 0,
                hasSavedSearches: false
            )

            if case let .search(presets) = section.type {
                let identifiers = presets.map(\.accessibilityIdentifier)
                let expectedIdentifiers = [
                    InAppServicesAccessibilityIdentifiers.Search.recentSearch,
                    FiltersPresetsAccessibilityIdentifiers.presetCell(type: .rentTwoRoomApartment),
                    FiltersPresetsAccessibilityIdentifiers.presetCell(type: .buyApartmentInSite),
                    FiltersPresetsAccessibilityIdentifiers.presetCell(type: .buySingleRoomApartment),
                ]

                XCTAssertEqual(identifiers, expectedIdentifiers)
            }
            else {
                XCTFail("Некорректный тип секции.")
            }
        }

        XCTContext.runActivity(named: "Только пресеты") { _ in
            let section = generator.makeSearchSection(latestSearch: nil, savedSearchesUpdatesCount: 0, hasSavedSearches: false)
            checkOnlyPresetsSection(section)
        }

        XCTContext.runActivity(named: "Пресеты, сохранённые поиски и недавние поиски") { _ in
            let section = generator.makeSearchSection(
                latestSearch: YREMutableSearch(),
                savedSearchesUpdatesCount: 5,
                hasSavedSearches: true
            )

            if case let .search(presets) = section.type {
                let identifiers = presets.map(\.accessibilityIdentifier)
                let expectedIdentifiers = [
                    InAppServicesAccessibilityIdentifiers.Search.recentSearch,
                    InAppServicesAccessibilityIdentifiers.Search.savedSearch,
                    FiltersPresetsAccessibilityIdentifiers.presetCell(type: .rentTwoRoomApartment),
                    FiltersPresetsAccessibilityIdentifiers.presetCell(type: .buyApartmentInSite),
                    FiltersPresetsAccessibilityIdentifiers.presetCell(type: .buySingleRoomApartment),
                ]

                XCTAssertEqual(identifiers, expectedIdentifiers)

                let savedSearchPreset = presets[1]
                let badge = savedSearchPreset.badge?.configuration
                let expectedBadge = BadgeView.Configuration(text: "5", icon: nil)

                XCTAssertEqual(badge, expectedBadge)
            }
            else {
                XCTFail("Некорректный тип секции.")
            }
        }
    }
}
