//
//  CommuteAddressSearchSteps.swift
//  UITests
//
//  Created by Leontyev Saveliy on 16/03/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class CommuteAddressSearchSteps {
    @discardableResult
    func isCommuteAddressSearchViewControllerExists() -> Self {
        self.viewController.yreEnsureExistsWithTimeout()
        return self
    }

    @discardableResult
    func isCommuteSuggestNotEmpty() -> Self {
        self.tableView
            .yreEnsureExistsWithTimeout()
            .cells
            .element(boundBy: 0)
            .yreEnsureExistsWithTimeout()
        return self
    }

    @discardableResult
    func isCommuteSuggestEmpty() -> Self {
        XCTAssertTrue(self.tableView
            .yreEnsureExistsWithTimeout()
            .cells.count == 0, "Suggest list must be empty")
        return self
    }

    @discardableResult
    func selectFirstSuggest() -> Self {
        self.tableView
            .yreEnsureExistsWithTimeout()
            .cells
            .element(boundBy: 0)
            .yreEnsureExistsWithTimeout()
            .tap()
        return self
    }

    private lazy var viewController = ElementsProvider.obtainElement(identifier: "CommuteAddressSearchVC")

    private lazy var tableView = ElementsProvider.obtainElement(identifier: "CommuteAddressSearch.tableView",
                                                                in: self.viewController)
}
