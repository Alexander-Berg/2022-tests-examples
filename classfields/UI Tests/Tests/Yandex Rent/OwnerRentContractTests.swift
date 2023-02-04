//
//  OwnerRentContractTests.swift
//  UI Tests
//
//  Created by Evgenii Novikov on 27.05.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig

final class OwnerRentContractTests: BaseTestCase {
    override func setUp() {
        super.setUp()

        InAppServicesStubConfigurator.setupEmptyServiceInfo(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupFlatsWithOneFlat(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupOwnerFlatRentContractNotification(using: self.dynamicStubs)
        YaRentContractAPIStubConfiguration.setupRentContract(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupGetDownloadUrl(using: self.dynamicStubs)
        YaRentContractAPIStubConfiguration.setupInputChanges(using: self.dynamicStubs)
        YaRentContractAPIStubConfiguration.setupSMSForSigning(using: self.dynamicStubs)
        YaRentContractAPIStubConfiguration.setupSignSMS(using: self.dynamicStubs)
    }

    func testRentContractFlow() throws {
        let config = ExternalAppConfiguration.inAppServicesTests
        self.relaunchApp(with: config)
        self.openFlatCard()

        YaRentFlatNotificationSteps(.ownerSignRentContract)
            .isPresented()
            .action()

        YaRentContractSteps()
            .ensurePresented()
            .tapOnHeaderItem()

        DocumentPreviewSteps()
            .isScreenPresented()
            .tapCloseButton()

        YaRentContractSteps()
            .ensurePresented()
            .tapOnFAQItem()

        YaRentContractFAQSteps()
            .ensureFAQPresented()
            .ensureQuestionViewPresented()
            .ensureAnswerViewNotPresented()
            .tapOnExpandButton()
            .ensureAnswerViewPresented()
            .tapOnExpandButton()
            .ensureAnswerViewNotPresented()
            .tapOnCommentButton()

        YaRentContractInputChangesSteps()
            .ensureInputChangesPresented()
            .enterComment()
            .tapOnSendButton()

        YaRentContractFinishPopupSteps()
            .ensureFinishPopupPresented()
            .tapFinishPopupButton()

        YaRentContractAPIStubConfiguration.setupOwnerRentContractWithComments(using: self.dynamicStubs)

        YaRentFlatNotificationSteps(.ownerSignRentContract)
            .isPresented()
            .action()

        YaRentContractSteps()
            .ensurePresented()
            .tapOnFAQItem()

        YaRentContractFAQSteps()
            .ensureFAQPresented()
            .ensureCommentButtonNotPresented()
            .tapOnBackButton()

        YaRentContractSteps()
            .ensurePresented()
            .tapOnCommentsItem()

        YaRentContractManagerCommentsSteps()
            .ensurePresented()
            .tapOnBackButton()

        YaRentContractSteps()
            .ensurePresented()
            .tapOnSignButton()

        SMSConfirmationSteps()
            .ensurePresented()
            .enterSMS()
            .tapOnConfirmButton()

        YaRentContractFinishPopupSteps()
            .ensureFinishPopupPresented()
            .tapFinishPopupButton()

        YaRentFlatNotificationSteps(.ownerSignRentContract)
            .isPresented()
    }

    // MARK: - Private

    @discardableResult
    private func openFlatCard() -> YaRentFlatCardSteps {
        let servicesSteps = InAppServicesSteps()

        servicesSteps
            .isScreenPresented()
            .isContentPresented()
            .tapOnFirstFlat()

        let flatSteps = YaRentFlatCardSteps()

        flatSteps
            .isScreenPresented()
            .isContentLoaded()

        return flatSteps
    }
}
