//
//  GeoIntentSteps.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 6/22/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation

final class GeoIntentSteps {
    @discardableResult
    func tapOnRegionButton() -> Self {
        let view = ElementsProvider.obtainElement(identifier: Identifiers.geoIntentPicker)
        view.yreEnsureExists()
        let regionButton = ElementsProvider.obtainElement(identifier: Identifiers.regionButton, in: view)
        regionButton
            .yreEnsureExistsWithTimeout()
            .yreTap()
        return self
    }

    @discardableResult
    func typeTextIntoGeoIntentSearchField(text: String) -> Self {
        let view = ElementsProvider.obtainElement(identifier: Identifiers.geoIntentPicker)
        view.yreEnsureExistsWithTimeout()
        let textField = ElementsProvider.obtainElement(identifier: Identifiers.geoIntentSearchField, in: view)
        if textField.yreWaitForExistence() {
            textField
                .yreTap()
                .typeText(text)
        }

        return self
    }

    @discardableResult
    func tapToFirstSuggestedGeoIntent() -> Self {
        let table = ElementsProvider.obtainElement(identifier: Identifiers.geoIntentSuggestList, type: .table)
        if table.yreWaitForExistence() {
            let firstCell = table.cells.firstMatch
            firstCell.yreTap()
        }

        return self
    }

    @discardableResult
    func tapSubmitButton() -> Self {
        let view = ElementsProvider.obtainElement(identifier: Identifiers.geoIntentPicker)
        view.yreEnsureExistsWithTimeout()
        let button = ElementsProvider.obtainElement(identifier: Identifiers.geoIntentSubmitButton, in: view)
        button
            .yreEnsureExistsWithTimeout()
            .yreTap()

        return self
    }

    func tapOnBackButton() {
        let backButton = ElementsProvider.obtainButton(
            identifier: Identifiers.geoIntentCancelButton
        )

        backButton
            .yreEnsureExistsWithTimeout()
            .tap()
    }

    // MARK: Private
    private enum Identifiers {
        static let geoIntentPicker = "geoIntent.picker"
        static let geoIntentSearchField = "geoIntent.searchField"
        static let geoIntentSuggestList = "geoIntent.suggest.list"
        static let geoIntentSearchResultView = "geoIntent.searchResult"
        static let geoIntentSubmitButton = "geoIntent.submit"
        static let geoIntentCancelButton = "geoIntent.cancelButton"
        static let regionButton = "geoIntent.regionSelectionButton"
    }
}
