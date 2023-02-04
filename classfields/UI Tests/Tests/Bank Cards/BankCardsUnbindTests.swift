//
//  Created by Alexey Aleshkov on 26/03/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class BankCardsUnbindTests: BaseTestCase {
    func testSuccessfulUnbindCardWithTrailingSwipe() {
        let profileMenu = ProfileMenuSteps()
        let bankCards = BankCardsListSteps()
        let unbindAlert = UnbindBankCardAlertSteps()
        let unbindCard = UnbindBankCardSteps()

        BankCardsAPIStubConfigurator.setupNaturalUser(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardListOfOne(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupUnbindCardSucceeded(using: self.dynamicStubs)

        self.relaunchApp(with: .bankCardsUITests)

        profileMenu
            .screenIsPresented()
            .bankCardsCellIsPresented()
            .tapOnBankCardsCell()

        bankCards
            .screenIsPresented()
            .loaderIsPresented()
            .bankCardsListIsPresented(timeout: Constants.timeout)

        bankCards.bankCardCell(containing: AccessibilityIdentifiers.bankCardToRemove)
            .cellIsPresented()
            .performTrailingSwipe()
            .buttonIsPresented()
            .tap()

        unbindAlert
            .screenIsPresented()
            .tapOnSubmit()

        unbindCard
            .screenIsPresented()
            .loaderIsPresented()
            .successResultIsPresented(timeout: Constants.timeout)
            .tapOnCloseButton()
            .screenIsDismissed()

        bankCards.bankCardCell(containing: AccessibilityIdentifiers.bankCardToRemove)
            .cellIsDismissed()

        BankCardsAPIStubConfigurator.setupCardListEmpty(using: self.dynamicStubs)

        bankCards
            .performPullToRefresh()
            .pullToRefreshBecomeHidden(timeout: Constants.timeout)

        bankCards.bankCardCell(containing: AccessibilityIdentifiers.bankCardToRemove)
            .cellIsDismissed()
    }

    func testCancelUnbindCardWithTrailingSwipe() {
        let profileMenu = ProfileMenuSteps()
        let bankCards = BankCardsListSteps()
        let unbindAlert = UnbindBankCardAlertSteps()

        BankCardsAPIStubConfigurator.setupNaturalUser(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardListOfOne(using: self.dynamicStubs)

        self.relaunchApp(with: .bankCardsUITests)

        profileMenu
            .screenIsPresented()
            .bankCardsCellIsPresented()
            .tapOnBankCardsCell()

        bankCards
            .screenIsPresented()
            .loaderIsPresented()
            .bankCardsListIsPresented(timeout: Constants.timeout)

        bankCards.bankCardCell(containing: AccessibilityIdentifiers.bankCardToRemove)
            .cellIsPresented()
            .performTrailingSwipe()
            .buttonIsPresented()
            .tap()

        unbindAlert
            .screenIsPresented()
            .tapOnCancel()

        bankCards.bankCardCell(containing: AccessibilityIdentifiers.bankCardToRemove)
            .cellIsPresented()
            .trailingSwipeButton()
            .buttonIsDismissed()
    }

    func testSuccessfulUnbindCardWithinEditingMode() {
        let profileMenu = ProfileMenuSteps()
        let bankCards = BankCardsListSteps()
        let unbindAlert = UnbindBankCardAlertSteps()
        let unbindCard = UnbindBankCardSteps()

        BankCardsAPIStubConfigurator.setupNaturalUser(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardListOfMany3(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupUnbindCardSucceeded(using: self.dynamicStubs)

        self.relaunchApp(with: .bankCardsUITests)

        profileMenu
            .screenIsPresented()
            .bankCardsCellIsPresented()
            .tapOnBankCardsCell()

        bankCards
            .screenIsPresented()
            .loaderIsPresented()
            .bankCardsListIsPresented(timeout: Constants.timeout)

        let cellSteps = bankCards.bankCardCell(containing: AccessibilityIdentifiers.bankCardToRemove)
            .cellIsPresented()

        bankCards
            .tapOnEditButton()

        cellSteps
            .accessoryDeleteButton()
            .buttonIsPresented()
            .tap()

        unbindAlert
            .screenIsPresented()
            .tapOnSubmit()

        unbindCard
            .screenIsPresented()
            .loaderIsPresented()
            .successResultIsPresented(timeout: Constants.timeout)
            .tapOnCloseButton()
            .screenIsDismissed()

        bankCards.bankCardCell(containing: AccessibilityIdentifiers.bankCardToRemove)
            .cellIsDismissed()

        let otherCellSteps = bankCards.bankCardCell(containing: AccessibilityIdentifiers.bankCard)
        otherCellSteps.cellIsPresented()

        otherCellSteps.accessoryDeleteButton()
            .buttonIsPresented()

        bankCards.tapOnReadyButton()

        otherCellSteps.accessoryDeleteButton()
            .buttonIsDismissed()

        BankCardsAPIStubConfigurator.setupCardListOfMany2(using: self.dynamicStubs)

        bankCards
            .performPullToRefresh()
            .pullToRefreshBecomeHidden(timeout: Constants.timeout)

        bankCards.bankCardCell(containing: AccessibilityIdentifiers.bankCardToRemove)
            .cellIsDismissed()
        bankCards.bankCardCell(containing: AccessibilityIdentifiers.bankCard)
            .cellIsPresented()
    }

    func testFailedToUnbindCard() {
        let profileMenu = ProfileMenuSteps()
        let bankCards = BankCardsListSteps()
        let unbindAlert = UnbindBankCardAlertSteps()
        let unbindCard = UnbindBankCardSteps()

        BankCardsAPIStubConfigurator.setupNaturalUser(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardListOfOne(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupUnbindCardFailed(using: self.dynamicStubs)

        self.relaunchApp(with: .bankCardsUITests)

        profileMenu
            .screenIsPresented()
            .bankCardsCellIsPresented()
            .tapOnBankCardsCell()

        bankCards
            .screenIsPresented()
            .loaderIsPresented()
            .bankCardsListIsPresented(timeout: Constants.timeout)

        bankCards.bankCardCell(containing: AccessibilityIdentifiers.bankCardToRemove)
            .cellIsPresented()
            .performTrailingSwipe()
            .buttonIsPresented()
            .tap()

        unbindAlert
            .screenIsPresented()
            .tapOnSubmit()

        unbindCard
            .screenIsPresented()
            .loaderIsPresented()
            .failResultIsPresented(timeout: Constants.timeout)
            .tapOnCloseButton()
            .screenIsDismissed()

        bankCards.bankCardCell(containing: AccessibilityIdentifiers.bankCardToRemove)
            .cellIsPresented()

        bankCards
            .performPullToRefresh()
            .pullToRefreshBecomeHidden(timeout: Constants.timeout)

        bankCards.bankCardCell(containing: AccessibilityIdentifiers.bankCardToRemove)
            .cellIsPresented()
    }

    func testRetryToUnbindCard() {
        let profileMenu = ProfileMenuSteps()
        let bankCards = BankCardsListSteps()
        let unbindAlert = UnbindBankCardAlertSteps()
        let unbindCard = UnbindBankCardSteps()

        BankCardsAPIStubConfigurator.setupNaturalUser(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardListOfOne(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupUnbindCardFailed(using: self.dynamicStubs)

        self.relaunchApp(with: .bankCardsUITests)

        profileMenu
            .screenIsPresented()
            .bankCardsCellIsPresented()
            .tapOnBankCardsCell()

        bankCards
            .screenIsPresented()
            .loaderIsPresented()
            .bankCardsListIsPresented(timeout: Constants.timeout)

        bankCards.bankCardCell(containing: AccessibilityIdentifiers.bankCardToRemove)
            .cellIsPresented()
            .performTrailingSwipe()
            .buttonIsPresented()
            .tap()

        unbindAlert
            .screenIsPresented()
            .tapOnSubmit()

        unbindCard
            .screenIsPresented()
            .loaderIsPresented()
            .failResultIsPresented(timeout: Constants.timeout)

        BankCardsAPIStubConfigurator.setupUnbindCardSucceeded(using: self.dynamicStubs)

        unbindCard
            .tapOnRetryButton()
            .successResultIsPresented(timeout: Constants.timeout)
            .tapOnCloseButton()
            .screenIsDismissed()

        bankCards.bankCardCell(containing: AccessibilityIdentifiers.bankCardToRemove)
            .cellIsDismissed()

        BankCardsAPIStubConfigurator.setupCardListEmpty(using: self.dynamicStubs)

        bankCards
            .performPullToRefresh()
            .pullToRefreshBecomeHidden(timeout: Constants.timeout)

        bankCards.bankCardCell(containing: AccessibilityIdentifiers.bankCardToRemove)
            .cellIsDismissed()
    }

    // MARK: Private

    private enum Constants {
        static let timeout: TimeInterval = BankCardsAPIStubConfigurator.Constants.timeout + 3
    }

    private enum AccessibilityIdentifiers {
        static let bankCardToRemove = "4444"
        static let bankCard = "4448"
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
            filename: "bankcards-unbind-user-natural.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupCardListOfMany3(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/banker/user/me/cards",
            filename: "bankcards-unbind-list-many3.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupCardListOfMany2(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/banker/user/me/cards",
            filename: "bankcards-unbind-list-many2.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupCardListOfOne(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/banker/user/me/cards",
            filename: "bankcards-unbind-list-one.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupCardListEmpty(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/banker/user/me/cards",
            filename: "bankcards-unbind-list-zero.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupUnbindCardSucceeded(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .DELETE,
            path: "/2.0/banker/user/me/card/555555|4444/gate/yandexkassa_v3/unbind",
            filename: "bankcards-unbind.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupUnbindCardFailed(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .DELETE,
            path: "/2.0/banker/user/me/card/555555|4444/gate/yandexkassa_v3/unbind",
            middleware: MiddlewareBuilder
                .chainOf([
                    .requestTime(Constants.timeout),
                    .respondWith(.internalServerError()),
                ])
                .build()
            )
    }
}
