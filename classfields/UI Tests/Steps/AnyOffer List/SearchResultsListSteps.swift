//
//  SearchResultsListSteps.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 18.01.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREAccessibilityIdentifiers

final class SearchResultsListSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что список открыт") { _ -> Void in
            self.screen.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isScreenNotPresented(timeout: TimeInterval = Constants.timeout) -> Self {
        XCTContext.runActivity(named: "Проверяем, что список не открыт") { _ -> Void in
            self.screen.yreEnsureNotExistsWithTimeout(timeout: timeout)
        }
        return self
    }

    @discardableResult
    func isScreenTitle(equals title: String) -> Self {
        XCTContext.runActivity(named: "Проверяем соответствие заголовка списка строке \"\(title)\"") { _ -> Void in
            CommonStepsProvider.isSearchResultsNavigationPanelWithTitle(title)
        }
        return self
    }

    @discardableResult
    func isSwitchToMapButtonTappable() -> Self {
        let buttonTitle = "Карта"
        XCTContext.runActivity(named: "Проверяем наличие кнопки \"\(buttonTitle)\"") { _ -> Void in
            self.mapSwitchButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabled()
                .yreEnsureHittable()
            XCTAssertEqual(self.mapSwitchButton.label, buttonTitle)
        }
        return self
    }

    @discardableResult
    func tapOnSwitchToMapButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Карта\"") { _ -> Void in
            self.mapSwitchButton.tap()
        }
        return self
    }

    @discardableResult
    func isSwitchToFiltersButtonTappable() -> Self {
        CommonSearchResultsSteps().isSwitchToFiltersButtonTappable()
        return self
    }

    @discardableResult
    func openFilters() -> FiltersSteps {
        CommonSearchResultsSteps().openFilters()
        return FiltersSteps()
    }

    @discardableResult
    func isLoadMoreButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие кнопки \"Показать ещё\"") { _ -> Void in
            let loadMoreButton = self.screen.buttons[self.loadMoreButtonID]
            loadMoreButton
                .yreEnsureExists()
                .yreEnsureEnabled()
                .yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func isLoadMoreButtonNotHittable() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие кнопки \"Показать ещё\"") { _ -> Void in
            let loadMoreButton = self.screen.buttons[self.loadMoreButtonID]
            XCTAssertTrue(loadMoreButton.exists == false || loadMoreButton.isHittable == false)
        }
        return self
    }

    @discardableResult
    func tapOnSubscribeButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Подписаться\"") { _ -> Void in
            self.subscribeButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }


    func withOfferList() -> OfferListSteps {
        return OfferListSteps.mainList()
    }

    func withSiteList() -> SiteListSteps {
        return SiteListSteps()
    }

    func withVillageList() -> VillageListSteps {
        return VillageListSteps()
    }

    func scrollToConcierge() -> Self {
        XCTContext.runActivity(named: "Ищем ячейку коньсержа") { _ -> Void in
            let cell = ElementsProvider.obtainElement(identifier: ConciergeAccessibilityIdentifiers.cell)
            cell.yreEnsureExistsWithTimeout()
            self.screen.scroll(to: cell)
        }
        return self
    }

    @discardableResult
    func tapOnConcierge() -> Self {
        XCTContext.runActivity(named: "Нажимаем на ячейку коньсержа") { _ -> Void in
            let conciergeCell = ElementsProvider.obtainElement(identifier: ConciergeAccessibilityIdentifiers.cell)
            conciergeCell
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    // MARK: Private

    private let mapSwitchButtonID = "search-results.controls.map-switch"
    private let loadMoreButtonID = "list.loadMoreButton"
    private let subscribeButtonID = "search-results.controls.subscribe-button"

    private lazy var screen: XCUIElement = ElementsProvider.obtainElement(
        identifier: AnyOfferListAccessibilityIdentifiers.mainList
    )
    private lazy var navigationContainer = ElementsProvider.obtainNavigationContainer()
    private lazy var mapSwitchButton = ElementsProvider.obtainElement(
        identifier: self.mapSwitchButtonID,
        type: .button,
        in: self.navigationContainer
    )
    private lazy var subscribeButton = ElementsProvider.obtainElement(
        identifier: self.subscribeButtonID,
        type: .button,
        in: self.navigationContainer
    )
}
