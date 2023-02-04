//
//  Created by Alexey Aleshkov on 06/04/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import enum YREAccessibilityIdentifiers.BankCardsListAccessibilityIdentifiers
import enum YREAccessibilityIdentifiers.BankCardCellAccessibilityIdentifiers

class BankCardsListCellSteps {
    init(element: XCUIElement) {
        self.element = element
    }

    @discardableResult
    func cellIsPresented() -> Self {
        self.element.yreEnsureExistsWithTimeout()
        return self
    }

    @discardableResult
    func cellIsDismissed() -> Self {
        self.element.yreEnsureNotExistsWithTimeout()
        return self
    }

    @discardableResult
    func tap() -> Self {
        self.element.yreTap()
        return self
    }

    // MARK: - Private

    fileprivate let element: XCUIElement

    fileprivate typealias BankCardsIdentifiers = BankCardsListAccessibilityIdentifiers
    fileprivate typealias BankCardCellIdentifiers = BankCardCellAccessibilityIdentifiers
}

final class BankCardCellSteps: BankCardsListCellSteps {
    final class ButtonSteps {
        init(element: XCUIElement) {
            self.element = element
        }

        @discardableResult
        func buttonIsPresented() -> Self {
            self.element.yreEnsureVisibleWithTimeout()
            return self
        }

        @discardableResult
        func buttonIsDismissed() -> Self {
            self.element.yreEnsureNotVisibleWithTimeout()
            return self
        }

        @discardableResult
        func tap() -> Self {
            self.element.yreTap()
            return self
        }

        // MARK: - Private

        private let element: XCUIElement
    }

    @discardableResult
    func cellIsPreferred(timeout: TimeInterval = Constants.timeout) -> Self {
        self.preferredIcon.yreEnsureExistsWithTimeout(timeout: timeout)
        return self
    }

    @discardableResult
    func cellIsNotPreferred(timeout: TimeInterval = Constants.timeout) -> Self {
        self.preferredIcon.yreEnsureNotExistsWithTimeout(timeout: timeout)
        return self
    }

    @discardableResult
    func trailingSwipeButton() -> ButtonSteps {
        let result = ButtonSteps(element: self.swipeButton)
        return result
    }

    @discardableResult
    func performTrailingSwipe() -> ButtonSteps {
        self.element.yreSwipeLeft()
        return self.trailingSwipeButton()
    }

    @discardableResult
    func accessoryDeleteButton() -> ButtonSteps {
        let result = ButtonSteps(element: self.deleteButton)
        return result
    }

    // MARK: - Private

    private lazy var preferredIcon = ElementsProvider.obtainElement(
        identifier: BankCardCellIdentifiers.preferredIconIdentifier,
        type: .image,
        in: self.element
    )

    private lazy var deleteButton = ElementsProvider.obtainElement(
        identifier: BankCardCellIdentifiers.deleteButtonIdentifier,
        type: .other,
        in: self.element
    )

    private lazy var swipeButton = ElementsProvider.obtainElement(
        identifier: BankCardsIdentifiers.unbindButtonTitle,
        type: .button,
        in: self.element
    )
}
