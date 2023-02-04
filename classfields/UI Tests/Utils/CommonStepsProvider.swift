//
//  CommonStepsProvider.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 18.01.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

/// Convenient methods to assert the most common cases.
/// Not intended to be used directly from tests.
final class CommonStepsProvider {
    static func isNavigationPanelWithTitle(_ title: String) {
        let navigationPanelContent = ElementsProvider.obtainNavigationBarTitleView()
        navigationPanelContent.yreEnsureExistsWithTimeout()

        let titleLabels = navigationPanelContent.staticTexts
        XCTAssertGreaterThanOrEqual(titleLabels.count, 1)

        let titleLabelIndex = titleLabels.count - 1
        let titleLabel = titleLabels.element(boundBy: titleLabelIndex)
        XCTAssertEqual(titleLabel.label, title)
    }

    static func isSearchParamsNavigationPanelWithTitle(_ title: String, subtitle: String? = nil) {
        let navigationPanelContent = ElementsProvider.obtainNavigationBarTitleView()
        navigationPanelContent.yreEnsureExistsWithTimeout()

        let searchParamsView = ElementsProvider.obtainElement(identifier: "Search Params Info")
        searchParamsView.yreEnsureExists()

        let titleLabel = searchParamsView.staticTexts["search_params.title"]
        XCTAssertEqual(titleLabel.label, title)

        if let subtitle = subtitle {
            let subtitleLabel = searchParamsView.staticTexts["search_params.subtitle"]
            XCTAssertEqual(subtitleLabel.label, subtitle)
        }
    }

    static func isSearchResultsNavigationPanelWithTitle(_ title: String?) {
        XCTContext.runActivity(named: "Проверяем текст в поле поиска") { _ -> Void in
            let navigationPanelContent = ElementsProvider.obtainNavigationBarTitleView()
            navigationPanelContent.yreEnsureExistsWithTimeout()
            
            let titleView = ElementsProvider.obtainElement(identifier: "Search Geo Params Info")
            titleView.yreEnsureExists()
            
            let geoField = ElementsProvider.obtainElement(identifier: "search-results.controls.geo-field", in: titleView)
            geoField.yreEnsureExists()
            
            let geoFieldValue = geoField.value as? String
            let geoFieldPlaceholderValue = geoField.placeholderValue

            if geoFieldValue != geoFieldPlaceholderValue {
                XCTAssertEqual(geoFieldValue, title)
            }
            else {
                XCTAssertNil(title)
            }
        }
    }
}
