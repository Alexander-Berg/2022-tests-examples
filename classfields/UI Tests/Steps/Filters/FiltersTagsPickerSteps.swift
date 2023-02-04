//
//  FiltersTagsPickerSteps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 18.09.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREAccessibilityIdentifiers

final class FiltersTagsPickerSteps {
    @discardableResult
    func isTagsPickerPresented() -> Self {
        let picker = self.viewController
        picker.yreEnsureExistsWithTimeout()
        return self
    }

    @discardableResult
    func isSuggestListNotEmpty() -> Self {
        self.tableView
            .yreEnsureExistsWithTimeout()
            .cells
            .element(boundBy: 0)
            .yreEnsureExistsWithTimeout()
        return self
    }

    @discardableResult
    func selectSuggest(by title: String) -> Self {
        self.tableView.yreEnsureExistsWithTimeout()

        let identifier = Identifiers.tagPickerCellIdentifierPrefix + title
        let cell = ElementsProvider.obtainElement(identifier: identifier, in: self.tableView)
        cell.yreEnsureExistsWithTimeout()
            .yreTap()

        return self
    }

    @discardableResult
    func enter(text: String) -> Self {
        let searchTextField = ElementsProvider.obtainElement(identifier: Identifiers.searchTextFieldIdentifier)
        searchTextField
            .yreEnsureExists()
            .yreTap()
            .yreTypeText(text)

        return self
    }

    @discardableResult
    func clearEnteredText() -> Self {
        let clearTextButton = ElementsProvider.obtainElement(identifier: Identifiers.clearTextButtonIdentifier)
        clearTextButton
            .yreEnsureExists()
            .yreTap()

        return self
    }

    @discardableResult
    func close() -> Self {
        let closeButton = ElementsProvider.obtainElement(identifier: Identifiers.backButtonIdentifier, in: self.viewController)
        closeButton
            .yreEnsureExists()
            .yreTap()

        return self
    }

    @discardableResult
    func isSearchTextFieldEmpty() -> Self {
        let searchTextField = ElementsProvider.obtainElement(identifier: Identifiers.searchTextFieldIdentifier)
        XCTAssertTrue(searchTextField.title.isEmpty)
        return self
    }

    private lazy var viewController = ElementsProvider.obtainElement(identifier: Identifiers.viewControllerIdentifier)
    private lazy var tableView = ElementsProvider.obtainElement(
        identifier: Identifiers.tableViewIdentifier,
        in: self.viewController
    )

    private typealias Identifiers = FiltersTagsPickerAccessibilityIdentifiers
}
