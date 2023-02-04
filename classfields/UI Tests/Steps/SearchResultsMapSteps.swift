//
//  SearchResultsMapSteps.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 18.01.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class SearchResultsMapSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        return XCTContext.runActivity(named: "Проверяем наличие экрана с картой") { _ -> Self in
            self.mapContentView.yreEnsureExistsWithTimeout()
            return self
        }
    }

    @discardableResult
    func hasGeoIntentWithTitle(_ title: String?) -> Self {
        CommonStepsProvider.isSearchResultsNavigationPanelWithTitle(title)
        return self
    }

    @discardableResult
    func tapOnHeatmapLegendButton() -> Self {
        let legendButton = self.mapContentView.buttons[self.heatmapLegendButtonID]
        legendButton
            .yreEnsureExistsWithTimeout()
            .tap()

        return self
    }

    @discardableResult
    func tapOnSubscribeButton() -> Self {
        XCTContext.runActivity(named: #"Нажимаем на кнопку "Подписаться""#) { _ -> Self in
            self.subscribeButton
                .yreEnsureExistsWithTimeout()
                .yreTap()

            return self
        }
    }

    @discardableResult
    func tapOnSwitchToListButton() -> Self {
        self.mapSwitchButton
            .yreEnsureExistsWithTimeout()
            .yreTap()

        return self
    }

    @discardableResult
    func tapOnHeatmapsSwitchingButton() -> Self {
        let mapControlsView = ElementsProvider.obtainElement(identifier: self.mapControlsViewID)
        mapControlsView.yreEnsureExistsWithTimeout()
        let heatmapsButton = ElementsProvider.obtainElement(identifier: self.heatmapsSwitchingButtonID, in: mapControlsView)
        heatmapsButton
            .yreEnsureExistsWithTimeout()
            .tap()
        return self
    }

    @discardableResult
    func showHeatmap(withTitle title: String) -> Self {
        let app = XCUIApplication()
        app.tables.staticTexts[title].tap()
        return self
    }

    @discardableResult
    func isHeatmapDescriptionWithTitle(_ title: String) -> Self {
        let app = XCUIApplication()

        let heatmapDescriptionSnippet = app.otherElements[self.heatmapDescriptionSnippetViewID]
        heatmapDescriptionSnippet.yreEnsureExistsWithTimeout()

        let snippetTitleLabel = heatmapDescriptionSnippet.staticTexts.firstMatch
        XCTAssertEqual(snippetTitleLabel.label, title)

        return self
    }

    @discardableResult
    func isSwitchToListButtonTappable() -> Self {
        self.mapSwitchButton
            .yreEnsureExistsWithTimeout()
            .yreEnsureEnabled()
            .yreEnsureHittable()
        XCTAssertEqual(self.mapSwitchButton.label, "Список")

        return self
    }

    @discardableResult
    func isSwitchToFiltersButtonTappable() -> Self {
        CommonSearchResultsSteps().isSwitchToFiltersButtonTappable()
        return self
    }

    // MARK: Private

    private let mapSwitchButtonID = "search-results.controls.map-switch"
    private let heatmapLegendButtonID = "map.controls.button.heatmap_legend"
    private let subscribeButtonID = "search-results.controls.subscribe-button"
    private let heatmapDescriptionSnippetViewID = "map.views.snippet"
    private let mapControlsViewID = "map.controls_view"
    private let heatmapsSwitchingButtonID = "map.controls_view.heatmaps_switching_button"

    private lazy var mapContentView: XCUIElement = ElementsProvider.obtainElement(identifier: "Map Content Container View")
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
