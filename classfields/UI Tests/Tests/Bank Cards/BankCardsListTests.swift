//
//  Created by Alexey Aleshkov on 26/03/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class BankCardsListTests: BaseTestCase {
    func testEditingMode() {
        let profileMenu = ProfileMenuSteps()
        let bankCards = BankCardsListSteps()

        BankCardsAPIStubConfigurator.setupNaturalUser(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardList(using: self.dynamicStubs)

        self.relaunchApp(with: .bankCardsUITests)

        profileMenu
            .screenIsPresented()
            .bankCardsCellIsPresented()
            .tapOnBankCardsCell()

        bankCards
            .screenIsPresented()
            .loaderIsPresented()
            .bankCardsListIsPresented(timeout: Constants.timeout)
            .addCardIsPresented()

        bankCards.bankCardCell(containing: AccessibilityIdentifiers.anyBankCard)
            .cellIsPresented()

        bankCards
            .tapOnEditButton()
            .addCardIsPresented()

        bankCards.bankCardCell(containing: AccessibilityIdentifiers.anyBankCard)
            .accessoryDeleteButton()
            .buttonIsPresented()

        bankCards
            .tapOnReadyButton()
            .addCardIsPresented()

        bankCards.bankCardCell(containing: AccessibilityIdentifiers.anyBankCard)
            .accessoryDeleteButton()
            .buttonIsDismissed()
    }

    func testEditingModeNotOverlapped() {
        let profileMenu = ProfileMenuSteps()
        let bankCards = BankCardsListSteps()

        BankCardsAPIStubConfigurator.setupNaturalUser(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardList(using: self.dynamicStubs)

        self.relaunchApp(with: .bankCardsUITests)

        profileMenu
            .screenIsPresented()
            .bankCardsCellIsPresented()
            .tapOnBankCardsCell()

        bankCards
            .screenIsPresented()
            .loaderIsPresented()
            .bankCardsListIsPresented(timeout: Constants.timeout)

        let cellSteps = bankCards.bankCardCell(containing: AccessibilityIdentifiers.anyBankCard)
            .cellIsPresented()
        cellSteps.performTrailingSwipe()

        bankCards.tapOnEditButton()

        cellSteps
            .accessoryDeleteButton()
            .buttonIsPresented()
        cellSteps
            .trailingSwipeButton()
            .buttonIsDismissed()

        bankCards.tapOnReadyButton()

        cellSteps
            .accessoryDeleteButton()
            .buttonIsDismissed()
        cellSteps
            .trailingSwipeButton()
            .buttonIsDismissed()
    }

    func testPreferredCardPosition() {
        let profileMenu = ProfileMenuSteps()
        let bankCards = BankCardsListSteps()

        BankCardsAPIStubConfigurator.setupNaturalUser(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardListPreferredCardBefore(using: self.dynamicStubs)

        self.relaunchApp(with: .bankCardsUITests)

        profileMenu
            .screenIsPresented()
            .bankCardsCellIsPresented()
            .tapOnBankCardsCell()

        bankCards
            .screenIsPresented()
            .loaderIsPresented()
            .bankCardsListIsPresented(timeout: Constants.timeout)

        bankCards.bankCardCell(containing: AccessibilityIdentifiers.bankCardToPrefer)
            .cellIsPresented()
            .cellIsNotPreferred(timeout: Constants.timeout)
            .tap()
            .cellIsPreferred(timeout: Constants.timeout)
        let preferredCellIndexBefore = bankCards.indexOfBankCardCell(containing: AccessibilityIdentifiers.bankCardToPrefer)

        BankCardsAPIStubConfigurator.setupCardListPreferredCardAfter(using: self.dynamicStubs)

        bankCards
            .performPullToRefresh()
            .pullToRefreshBecomeHidden(timeout: Constants.timeout)

        bankCards.bankCardCell(containing: AccessibilityIdentifiers.bankCardToPrefer)
            .cellIsPresented()
            .cellIsPreferred()
        let preferredCellIndexAfter = bankCards.indexOfBankCardCell(containing: AccessibilityIdentifiers.bankCardToPrefer)

        XCTAssertEqual(preferredCellIndexBefore, preferredCellIndexAfter)
    }

    func disabled_testPreferredCardNotResetOnCardAdding() {
        let profileMenu = ProfileMenuSteps()
        let bankCards = BankCardsListSteps()
        let checkoutSteps = YooKassaPaymentsSteps()

        BankCardsAPIStubConfigurator.setupNaturalUser(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardListPreferredCardBefore(using: self.dynamicStubs)

        self.relaunchApp(with: .bankCardsUITests)

        profileMenu
            .screenIsPresented()
            .bankCardsCellIsPresented()
            .tapOnBankCardsCell()

        bankCards
            .screenIsPresented()
            .loaderIsPresented()
            .bankCardsListIsPresented(timeout: Constants.timeout)

        bankCards.bankCardCell(containing: AccessibilityIdentifiers.bankCardToPrefer)
            .cellIsPresented()
            .cellIsNotPreferred(timeout: Constants.timeout)
            .tap()
            .cellIsPreferred(timeout: Constants.timeout)
        let preferredCellIndexBefore = bankCards.indexOfBankCardCell(containing: AccessibilityIdentifiers.bankCardToPrefer)

        BankCardsAPIStubConfigurator.setupCardListPreferredCardAfter(using: self.dynamicStubs)

        bankCards
            .addCardIsPresented()
            .tapOnAddCard()

        checkoutSteps.makeActivity()
            .cancel()
            .run()

        bankCards.bankCardCell(containing: AccessibilityIdentifiers.bankCardToPrefer)
            .cellIsPresented()
            .cellIsPreferred()
        let preferredCellIndexAfter = bankCards.indexOfBankCardCell(containing: AccessibilityIdentifiers.bankCardToPrefer)

        XCTAssertEqual(preferredCellIndexBefore, preferredCellIndexAfter)
    }

    // MARK: Private

    private enum Constants {
        static let timeout: TimeInterval = BankCardsAPIStubConfigurator.Constants.timeout + 2
    }

    private enum AccessibilityIdentifiers {
        static let anyBankCard = "4477"

        static let bankCardToPrefer = "1111"
    }
}

private final class BankCardsAPIStubConfigurator {
    enum Constants {
        static let timeout: TimeInterval = 0.5
    }

    static func setupNaturalUser(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/1.0/user",
            filename: "bankcards-list-user-natural.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupCardList(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/banker/user/me/cards",
            filename: "bankcards-list.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupCardListPreferredCardBefore(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/banker/user/me/cards",
            filename: "bankcards-list-preferred-before.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupCardListPreferredCardAfter(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/banker/user/me/cards",
            filename: "bankcards-list-preferred-after.debug",
            requestTime: Constants.timeout
        )
    }
}
