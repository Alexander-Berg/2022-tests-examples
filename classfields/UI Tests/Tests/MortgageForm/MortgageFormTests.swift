//
//  MortgageFormTests.swift
//  UI Tests
//
//  Created by Timur Guliamov on 02.11.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig

final class MortgageFormTests: BaseTestCase {
    func testSuccessFormSending() {
        self.performCommonTests { formSteps in
            self.configureDemand(stubKind: .successed)
            self.configureDemandCommit(stubKind: .successed)

            let smsConfirmationSteps = MortgageFormSMSConfirmationSteps()
            let successSteps = MortgageFormSuccessSteps()

            formSteps
                .writeTextIn(.surname, text: "Ivanov")
                .writeTextIn(.name, text: "Ivan")
                .writeTextIn(.patronymic, text: "Ivanovich")
                .writeTextIn(.email, text: "tplay@meow.com")
                .writeTextIn(.phoneNumber, text: "9111111111")
                .tapOnView()
                .makeScreenshot(suffix: #function)
                .tapOnSendButton()

            smsConfirmationSteps
                .isScreenPresented()
                .writeText(text: "000000")

            successSteps
                .isScreenPresented()
                .makeScreenshot()
                .tapOnContinueButton()

            formSteps.isScreenNotPresented()
            smsConfirmationSteps.isScreenNotPresented()
            successSteps.isScreenNotPresented()
        }
    }

    func testFormScreenErrors() {
        // swiftlint:disable closure_body_length
        self.performCommonTests { formSteps in
            self.configureDemand(stubKind: .failed(.phoneValidation))
            formSteps
                .tapOnSendButton()
                .tapOnView()
                .isErrorUnderFieldPresented(.surname)
                .isErrorUnderFieldPresented(.name)
                .isErrorUnderFieldPresented(.email)
                .isErrorUnderFieldPresented(.phoneNumber)
                .isErrorUnderFieldNotPresented(.patronymic)
                .makeScreenshot(suffix: #function)

            formSteps
                .writeTextIn(.email, text: "tplay")
                .writeTextIn(.phoneNumber, text: "911111111")
                .tapOnSendButton()
                .tapOnView()
                .isErrorUnderFieldPresented(.surname)
                .isErrorUnderFieldPresented(.name)
                .isErrorUnderFieldPresented(.email)
                .isErrorUnderFieldPresented(.phoneNumber)
                .isErrorUnderFieldNotPresented(.patronymic)

            self.configureDemand(stubKind: .failed(.phoneValidation))

            formSteps
                .writeTextIn(.surname, text: "Иванов")
                .writeTextIn(.name, text: "Иван")
                .writeTextIn(.email, text: "@meow.com")
                .writeTextIn(.phoneNumber, text: "1")
                .tapOnSendButton()
                .tapOnView()
                .isErrorUnderFieldPresented(.phoneNumber)
                .isErrorUnderFieldNotPresented(.surname)
                .isErrorUnderFieldNotPresented(.name)
                .isErrorUnderFieldNotPresented(.email)
                .isErrorUnderFieldNotPresented(.patronymic)

            self.configureDemand(stubKind: .failed(.other))

            formSteps
                .tapOnSendButton()
                .waitForTopNotificationViewExistence()
        }
    }

    func testSMSConfirmationScreenErrors() {
        self.performCommonTests(isPreFillingRequired: true) { formSteps in

            let smsConfirmationSteps = MortgageFormSMSConfirmationSteps()

            self.configureDemand(stubKind: .successed)
            self.configureDemandCommit(stubKind: .failed(.badCode))

            formSteps
                .writeTextIn(.surname, text: "Фимилия")
                .writeTextIn(.name, text: "Имя")
                .tapOnSendButton()

            smsConfirmationSteps
                .isScreenPresented()
                .isRetryButtonDisabled()
                .writeText(text: "123456")
                .isErrorUnderFieldPresented()
                .tapOnView()

            self.configureDemandCommit(stubKind: .failed(.other))

            smsConfirmationSteps
                .waitForRetryButtonEnable()
                .tapOnRetryButton()
                .isRetryButtonDisabled()
                .tapOnContinueButton()
                .waitForTopNotificationViewExistence()
                .isErrorUnderFieldNotPresented()
        }
    }

    func testPreFilling() {
        self.performCommonTests(isPreFillingRequired: true) { formSteps in
            formSteps
                .isTextIn(.phoneNumber, equalTo: "+7 931 582-17-21")
                .isTextIn(.email, equalTo: "email@email.mail")
        }
    }

    // MARK: - Private

    private func performCommonTests(
        isPreFillingRequired: Bool = false,
        specificTests: (MortgageFormSteps) -> Void
    ) {
        MortgageListAPIStubConfigurator.setupMortgageProgramSearchWithFormAtService(using: self.dynamicStubs)

        if isPreFillingRequired {
            MortgageFormAPIStubConfigurator.setupUserWithPhoneNumber(using: self.dynamicStubs)
            MortgageFormAPIStubConfigurator.setupUserEmail(using: self.dynamicStubs)
        }

        let appConfiguration = ExternalAppConfiguration.commonUITests
        appConfiguration.selectedTabItem = .home

        self.relaunchApp(with: appConfiguration)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnMortgageListCell()

        MortgageListSteps()
            .isListNonEmpty()
            .scrollToMortgageProgramCell()
            .tapOnSubmitButton()

        specificTests(
            MortgageFormSteps()
                .isScreenPresented()
        )
    }

    private func configureDemand(stubKind: MortgageFormAPIStubConfigurator.MortgageDemandStubKind) {
        MortgageFormAPIStubConfigurator.setupMortgageMortgageDemand(
            using: self.dynamicStubs,
            stubKind: stubKind
        )
    }

    private func configureDemandCommit(stubKind: MortgageFormAPIStubConfigurator.MortgageDemandCommitStubKind) {
        MortgageFormAPIStubConfigurator.setupMortgageMortgageDemandCommit(
            using: self.dynamicStubs,
            stubKind: stubKind
        )
    }
}
