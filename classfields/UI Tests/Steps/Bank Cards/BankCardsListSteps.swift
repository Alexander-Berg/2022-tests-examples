//
//  Created by Alexey Aleshkov on 27/03/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import enum YREAccessibilityIdentifiers.BankCardsListAccessibilityIdentifiers
import enum YREAccessibilityIdentifiers.BankCardCellAccessibilityIdentifiers

final class BankCardsListSteps {
    // MARK: - List

    lazy var rootView = ElementsProvider.obtainElement(
        identifier: BankCardsIdentifiers.viewIdentifier,
        type: .other
    )

    lazy var loaderView = ElementsProvider.obtainElement(
        identifier: BankCardsIdentifiers.loaderIdentifier,
        type: .other,
        in: self.rootView
    )

    lazy var bankCardsList = ElementsProvider.obtainElement(
        identifier: BankCardsIdentifiers.tableIdentifier,
        type: .table,
        in: self.rootView
    )

    lazy var refreshControl = ElementsProvider.obtainElement(
        identifier: BankCardsIdentifiers.refreshControlIdentifier,
        type: .other,
        in: self.bankCardsList
    )

    lazy var addCardCell = ElementsProvider.obtainElement(
        identifier: BankCardCellIdentifiers.addCellIdentifier,
        type: .cell,
        in: self.bankCardsList
    )

    lazy var editButton = ElementsProvider.obtainElement(
        identifier: BankCardsIdentifiers.editButtonTitle,
        type: .button
    )

    lazy var readyButton = ElementsProvider.obtainElement(
        identifier: BankCardsIdentifiers.submitButtonTitle,
        type: .button
    )

    // MARK: - Private

    private typealias BankCardsIdentifiers = BankCardsListAccessibilityIdentifiers
    private typealias BankCardCellIdentifiers = BankCardCellAccessibilityIdentifiers
}

extension BankCardsListSteps {
    @discardableResult
    func screenIsPresented() -> Self {
        self.rootView.yreEnsureExistsWithTimeout()
        return self
    }

    @discardableResult
    func loaderIsPresented() -> Self {
        self.loaderView.yreEnsureExistsWithTimeout()
        return self
    }

    @discardableResult
    func bankCardsListIsPresented(timeout: TimeInterval) -> Self {
        self.bankCardsList.yreEnsureExistsWithTimeout(timeout: timeout)
        return self
    }

    @discardableResult
    func performPullToRefresh() -> Self {
        self.bankCardsList.yrePullToRefresh()
        return self
    }

    @discardableResult
    func pullToRefreshBecomeHidden(timeout: TimeInterval = Constants.timeout) -> Self {
        self.refreshControl.yreEnsureNotExistsWithTimeout(timeout: timeout)
        return self
    }

    func bankCardCell(containing text: String) -> BankCardCellSteps {
        let bankCardPredicate = NSPredicate(format: "label CONTAINS[c] %@", text)
        let bankCardCell = self.bankCardsList.cells.containing(bankCardPredicate).element
        let result = BankCardCellSteps(element: bankCardCell)
        return result
    }

    func indexOfBankCardCell(containing text: String) -> Int? {
        let bankCardPredicate = NSPredicate(format: "label CONTAINS[c] %@", text)
        let index = self.bankCardsList.cells.allElementsBoundByIndex
            .firstIndex(where: { bankCardPredicate.evaluate(with: $0) })
        return index
    }

    @discardableResult
    func addCardIsPresented() -> Self {
        self.addCardCell.yreEnsureExistsWithTimeout()
        return self
    }

    @discardableResult
    func addCardIsDismissed() -> Self {
        self.addCardCell.yreEnsureNotExistsWithTimeout()
        return self
    }

    @discardableResult
    func tapOnEditButton() -> Self {
        self.editButton
            .yreEnsureExists()
            .yreTap()
        return self
    }

    @discardableResult
    func tapOnReadyButton() -> Self {
        self.readyButton
            .yreEnsureExistsWithTimeout()
            .yreTap()
        return self
    }

    @discardableResult
    func tapOnAddCard() -> Self {
        self.addCardCell
            .yreEnsureExists()
            .yreTap()
        return self
    }
}
