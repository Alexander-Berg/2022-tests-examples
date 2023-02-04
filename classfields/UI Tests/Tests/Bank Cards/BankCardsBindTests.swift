//
//  Created by Alexey Aleshkov on 26/03/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

// swiftlint:disable file_length

final class BankCardsBindTests: BaseTestCase {
    func disabled_testSuccessfulBindCardWithAPIProvidedEmailNoConfirm() {
        let profileMenu = ProfileMenuSteps()
        let bankCards = BankCardsListSteps()
        let bindCard = BindBankCardSteps()
        let checkoutSteps = YooKassaPaymentsSteps()

        BankCardsAPIStubConfigurator.setupNaturalUser(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardListBefore(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupUserEmailExistence(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardBindNoConfirm(using: self.dynamicStubs)

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
            .tapOnAddCard()

        checkoutSteps.makeActivity()
            .fillForm(
                cardNumber: Constants.validCardNumber,
                expireMonth: "12",
                expireShortYear: "99",
                cvc: "000"
            )
            .run()

        BankCardsAPIStubConfigurator.setupCardListAfter(using: self.dynamicStubs)

        bindCard
            .screenIsPresented()
            .loaderIsPresented()
            .successResultIsPresented(timeout: Constants.timeout)
            .tapOnSuccessProceedButton()
            .screenIsDismissed()

        bankCards.bankCardCell(containing: Constants.validCardNumberSuffix)
            .cellIsPresented()
    }

    // FIXME: this test disabled because of submit email button don't react on taps
    // https://st.yandex-team.ru/VSAPPS-8534
    func disabled_testSuccessfulBindCardWithUserProvidedEmailNoConfirm() {
        let profileMenu = ProfileMenuSteps()
        let bankCards = BankCardsListSteps()
        let bindCard = BindBankCardSteps()
        let emailConfirm = UserEmailConfirmationSteps()
        let checkoutSteps = YooKassaPaymentsSteps()

        BankCardsAPIStubConfigurator.setupNaturalUser(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardListBefore(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupUserEmailAbsence(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardBindNoConfirm(using: self.dynamicStubs)

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
            .tapOnAddCard()

        checkoutSteps.makeActivity()
            .fillForm(
                cardNumber: Constants.validCardNumber,
                expireMonth: "12",
                expireShortYear: "99",
                cvc: "000"
            )
            .run()

        BankCardsAPIStubConfigurator.setupCardListAfter(using: self.dynamicStubs)

        bindCard
            .screenIsPresented()
            .loaderIsPresented()

        emailConfirm
            .screenIsPresented()
            .fillForm(email: Constants.emailAddress)
            .formIsValid()
            .tapOnSubmit()
            .screenIsDismissed()

        bindCard
            .successResultIsPresented(timeout: Constants.timeout)
            .tapOnSuccessProceedButton()
            .screenIsDismissed()

        bankCards.bankCardCell(containing: Constants.validCardNumberSuffix)
            .cellIsPresented()
    }

    func disabled_testCancelBindCardWithUserProvidedEmail() {
        let profileMenu = ProfileMenuSteps()
        let bankCards = BankCardsListSteps()
        let bindCard = BindBankCardSteps()
        let emailConfirm = UserEmailConfirmationSteps()
        let checkoutSteps = YooKassaPaymentsSteps()

        BankCardsAPIStubConfigurator.setupNaturalUser(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardListBefore(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupUserEmailAbsence(using: self.dynamicStubs)

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
            .tapOnAddCard()

        checkoutSteps.makeActivity()
            .fillForm(
                cardNumber: Constants.validCardNumber,
                expireMonth: "12",
                expireShortYear: "99",
                cvc: "000"
            )
            .run()

        bindCard
            .screenIsPresented()
            .loaderIsPresented()

        emailConfirm
            .screenIsPresented()
            .tapOnCloseButton()
            .screenIsDismissed()

        bindCard
            .screenIsDismissed()

        bankCards.bankCardCell(containing: Constants.validCardNumberSuffix)
            .cellIsDismissed()
    }

    func disabled_testSuccessfulBindCardWithAPIProvidedEmailUrlConfirm() {
        let profileMenu = ProfileMenuSteps()
        let bankCards = BankCardsListSteps()
        let bindCard = BindBankCardSteps()
        let checkoutSteps = YooKassaPaymentsSteps()
        let webPageSteps = BindBankCard3dsAuthenticationSteps()

        BankCardsAPIStubConfigurator.setupNaturalUser(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardListBefore(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupUserEmailExistence(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardBindUrlConfirm(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardBindUrlConfirm3dsForm(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardBindUrlConfirm3dsFormSubmit(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardBindStatusBound(using: self.dynamicStubs)

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
            .tapOnAddCard()

        checkoutSteps.makeActivity()
            .fillForm(
                cardNumber: Constants.validCardNumber,
                expireMonth: "12",
                expireShortYear: "99",
                cvc: "000"
            )
            .run()

        bindCard
            .screenIsPresented()

        BankCardsAPIStubConfigurator.setupCardListAfter(using: self.dynamicStubs)

        webPageSteps.makeActivity()
            .fillForm(phoneNumber: Constants.phoneNumber)
            .run()

        bindCard
            .successResultIsPresented(timeout: Constants.timeout)
            .tapOnSuccessProceedButton()
            .screenIsDismissed()

        bankCards.bankCardCell(containing: Constants.validCardNumberSuffix)
            .cellIsPresented()
    }

    func disabled_testSuccessfulBindCardWithAPIProvidedEmailSMSConfirm() {
        let profileMenu = ProfileMenuSteps()
        let bankCards = BankCardsListSteps()
        let bindCard = BindBankCardSteps()
        let checkoutSteps = YooKassaPaymentsSteps()

        BankCardsAPIStubConfigurator.setupNaturalUser(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardListBefore(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupUserEmailExistence(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardBindSMSConfirm(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardBindStatusCascadeForSMS(using: self.dynamicStubs)

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
            .tapOnAddCard()

        checkoutSteps.makeActivity()
            .fillForm(
                cardNumber: Constants.validCardNumber,
                expireMonth: "12",
                expireShortYear: "99",
                cvc: "000"
            )
            .run()

        BankCardsAPIStubConfigurator.setupCardListAfter(using: self.dynamicStubs)

        bindCard
            .screenIsPresented()
            .waitForMessageIsPresented()
            .tapOnNoMessageProceedButton()
            .noMessageIsPresented()
            .successResultIsPresented(timeout: Constants.smsConfirmTimeout)
            .tapOnSuccessProceedButton()
            .screenIsDismissed()

        bankCards.bankCardCell(containing: Constants.validCardNumberSuffix)
            .cellIsPresented()
    }

    func disabled_testInProgressBindCardWithAPIProvidedEmailUrlConfirm() {
        let profileMenu = ProfileMenuSteps()
        let bankCards = BankCardsListSteps()
        let bindCard = BindBankCardSteps()
        let checkoutSteps = YooKassaPaymentsSteps()
        let webPageSteps = BindBankCard3dsAuthenticationSteps()

        BankCardsAPIStubConfigurator.setupNaturalUser(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardListBefore(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupUserEmailExistence(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardBindUrlConfirm(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardBindUrlConfirm3dsForm(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardBindUrlConfirm3dsFormSubmit(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupLongTimeCardBindStatusBound(using: self.dynamicStubs)

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
            .tapOnAddCard()

        checkoutSteps.makeActivity()
            .fillForm(
                cardNumber: Constants.validCardNumber,
                expireMonth: "12",
                expireShortYear: "99",
                cvc: "000"
            )
            .run()

        bindCard
            .screenIsPresented()

        BankCardsAPIStubConfigurator.setupCardListAfter(using: self.dynamicStubs)

        webPageSteps.makeActivity()
            .fillForm(phoneNumber: Constants.phoneNumber)
            .run()

        bindCard
            .progressResultIsPresented(timeout: Constants.longTimeout)
            .tapOnProgressProceedButton()
            .screenIsDismissed()

        bankCards.bankCardCell(containing: Constants.validCardNumberSuffix)
            .cellIsPresented()
    }

    func disabled_testCancelBindCardWithAPIProvidedEmailUrlConfirm() {
        let profileMenu = ProfileMenuSteps()
        let bankCards = BankCardsListSteps()
        let bindCard = BindBankCardSteps()
        let checkoutSteps = YooKassaPaymentsSteps()
        let webPageSteps = BindBankCard3dsAuthenticationSteps()

        BankCardsAPIStubConfigurator.setupNaturalUser(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardListBefore(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupUserEmailExistence(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardBindUrlConfirm(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardBindUrlConfirm3dsForm(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardBindUrlConfirm3dsFormSubmit(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardBindStatusProgress(using: self.dynamicStubs)

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
            .tapOnAddCard()

        checkoutSteps.makeActivity()
            .fillForm(
                cardNumber: Constants.validCardNumber,
                expireMonth: "12",
                expireShortYear: "99",
                cvc: "000"
            )
            .run()

        bindCard
            .screenIsPresented()

        webPageSteps.makeActivity()
            .cancel()
            .run()

        bindCard
            .progressResultIsPresented(timeout: Constants.longTimeout)
            .tapOnProgressProceedButton()
            .screenIsDismissed()

        bankCards.bankCardCell(containing: Constants.validCardNumberSuffix)
            .cellIsDismissed()
    }

    func disabled_testFailedToAttachExpiredCard() {
        let profileMenu = ProfileMenuSteps()
        let bankCards = BankCardsListSteps()
        let bindCard = BindBankCardSteps()
        let checkoutSteps = YooKassaPaymentsSteps()

        BankCardsAPIStubConfigurator.setupNaturalUser(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardListBefore(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupUserEmailExistence(using: self.dynamicStubs)
        BankCardsAPIStubConfigurator.setupCardBindFailedCardExpired(using: self.dynamicStubs)

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
            .tapOnAddCard()

        checkoutSteps.makeActivity()
            .fillForm(
                cardNumber: Constants.validCardNumber,
                expireMonth: "12",
                expireShortYear: "99",
                cvc: "000"
            )
            .run()

        bindCard
            .screenIsPresented()
            .loaderIsPresented()
            .failResultIsPresented(timeout: Constants.timeout)
            .failRetryButtonIsHidden()
            .tapOnCloseButton()
            .screenIsDismissed()
    }

    // MARK: Private

    private enum Constants {
        static let timeout: TimeInterval = BankCardsAPIStubConfigurator.Constants.timeout + 3
        static let longTimeout: TimeInterval = BankCardsAPIStubConfigurator.Constants.longTimeout + 3
        static let smsConfirmTimeout: TimeInterval = 10

        static let validCardNumber = "5555 5555 5555 4444"
        static let validCardNumberSuffix = "4444"

        static let expiredCardNumber = "5555 5555 5555 4543"

        static let emailAddress = "email@example.com"
        static let phoneNumber = "88005553535"
    }
}

private final class BankCardsAPIStubConfigurator {
    enum Constants {
        static let timeout: TimeInterval = 0.5
        static let emailTimeout: TimeInterval = 1 // to ensure loader is visible
        static let longTimeout: TimeInterval = 12 // longer than `BindBankCardInteractor.Consts.paymentStatusTimeout`
    }

    static func setupNaturalUser(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/1.0/user",
            filename: "bankcards-bind-user-natural.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupCardListBefore(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/banker/user/me/cards",
            filename: "bankcards-bind-list-before.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupCardListAfter(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/banker/user/me/cards",
            filename: "bankcards-bind-list-after.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupUserEmailExistence(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/banker/user/me/email",
            filename: "bankcards-user-email.debug",
            requestTime: Constants.emailTimeout
        )
    }

    static func setupUserEmailAbsence(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/banker/user/me/email",
            filename: "bankcards-user-noemail.debug",
            requestTime: Constants.emailTimeout
        )
    }

    static func setupCardBindNoConfirm(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .POST,
            path: "/2.0/banker/user/me/card/bind/requestV2",
            filename: "bankcards-bind-noconfirm.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupCardBindSMSConfirm(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .POST,
            path: "/2.0/banker/user/me/card/bind/requestV2",
            filename: "bankcards-bind-smsconfirm.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupCardBindUrlConfirm(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .POST,
            path: "/2.0/banker/user/me/card/bind/requestV2",
            filename: "bankcards-bind-urlconfirm.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupCardBindUrlConfirm3dsForm(using dynamicStubs: HTTPDynamicStubs) {
        let confirmFormFilename = "bankcards-bind-urlconfirm-form.debug"
        dynamicStubs.register(
            method: .GET,
            path: "/payments/card-auth", // sync with urlConfirmation.url in JSON backend response
            middleware: MiddlewareBuilder
                .chainOf([
                    .requestTime(Constants.timeout),
                    .respondWith(.ok(.contentsOfHTML(confirmFormFilename))),
                ])
                .build()
        )
    }

    static func setupCardBindUrlConfirm3dsFormSubmit(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .POST,
            path: "/sandbox/bank-card/", // sync with form POST action URL in html form file
            middleware: MiddlewareBuilder
                .chainOf([
                    .requestTime(Constants.timeout),
                    .respondWith(.redirect("https://custom.redirect.url/")), // sync with PaymentTestSettings.returnURLString
                ])
                .build()
        )
    }

    static func setupCardBindFailedCardExpired(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .POST,
            path: "/2.0/banker/user/me/card/bind/requestV2",
            filename: "bankcards-bind-error-cardexpired.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupCardBindStatusCascadeForSMS(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/banker/user/me/card/bind/status/602c878c7580b66fccb140ffa3ab5cdc",
            middleware: MiddlewareBuilder
                .chainOf([
                    .onceOf(
                        .chainOf([
                            .requestTime(Constants.timeout),
                            .respondWith(.ok(.contentsOfJSON("bankcards-bind-status-progress.debug"))),
                        ])
                    ),
                    .chainOf([
                        .requestTime(Constants.timeout),
                        .respondWith(.ok(.contentsOfJSON("bankcards-bind-status-bound.debug"))),
                    ])
                ])
                .build()
        )
    }

    static func setupCardBindStatusBound(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/banker/user/me/card/bind/status/602c878c7580b66fccb140ffa3ab5cdc",
            filename: "bankcards-bind-status-bound.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupLongTimeCardBindStatusBound(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/banker/user/me/card/bind/status/602c878c7580b66fccb140ffa3ab5cdc",
            filename: "bankcards-bind-status-bound.debug",
            requestTime: Constants.longTimeout
        )
    }

    static func setupCardBindStatusProgress(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/banker/user/me/card/bind/status/602c878c7580b66fccb140ffa3ab5cdc",
            filename: "bankcards-bind-status-progress.debug",
            requestTime: Constants.timeout
        )
    }

    static func setupCardBindStatusCancel(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/banker/user/me/card/bind/status/602c878c7580b66fccb140ffa3ab5cdc",
            filename: "bankcards-bind-status-cancel.debug",
            requestTime: Constants.timeout
        )
    }
}
