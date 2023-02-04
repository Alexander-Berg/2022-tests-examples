//
//  CommonSearchResultsSteps.swift
//  UITests
//
//  Created by Alexey Salangin on 1/30/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class CommonSearchResultsSteps {
    @discardableResult
    func openFiltersIfNeeded() -> FiltersSteps {
        return XCTContext.runActivity(named: "Проверяем наличие экрана фильтров") { _ -> FiltersSteps in
            let viewController = ElementsProvider.obtainElement(identifier: Identifiers.filtersViewController)
            if viewController.yreWaitForExistence() == false {
                return self.openFilters()
            }
            else {
                return FiltersSteps()
            }
        }
    }

    @discardableResult
    func openFilters() -> FiltersSteps {
        return XCTContext.runActivity(named: "Открываем экран фильтров") { _ -> FiltersSteps in
            let navigationContainer = ElementsProvider.obtainNavigationContainer()

            let filtersButton = ElementsProvider.obtainElement(identifier: Identifiers.filtersButton, in: navigationContainer)
            filtersButton
                .yreEnsureExistsWithTimeout()
                .tap()
            
            return FiltersSteps()
        }
    }

    @discardableResult
    func filterButtonHasBadge() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие бейджа на кнопке \"Фильтры\"") { _ in
            let navigationContainer = ElementsProvider.obtainNavigationContainer()
            let filtersButton = ElementsProvider.obtainElement(identifier: Identifiers.filtersButton, in: navigationContainer)
            filtersButton.yreEnsureExistsWithTimeout()
            let badge = ElementsProvider.obtainElement(identifier: Identifiers.filtersButtonBadge, in: navigationContainer)
            badge.yreEnsureExistsWithTimeout()
        }
        return self
    }

    func isSwitchToFiltersButtonTappable() {
        XCTContext.runActivity(named: "Проверяем наличие кнопки \"Фильтры\"") { _ in
            let navigationContainer = ElementsProvider.obtainNavigationContainer()
            let titleView = ElementsProvider.obtainElement(identifier: "Search Geo Params Info", in: navigationContainer)

            guard let filtersButton = ElementsProvider.obtainFiltersButtonIfExists(in: titleView) else {
                XCTFail("Couldn't find filter button")
                return
            }
            filtersButton.yreEnsureEnabledWithTimeout()
        }
    }

    // MARK: Private
    private enum Identifiers {
        static let filtersButton = "search-results.controls.filters-button"
        static let filtersButtonBadge = "search-results.controls.filters-button.badge"
        static let filtersViewController = "YREMainFilterViewController"
    }
}
