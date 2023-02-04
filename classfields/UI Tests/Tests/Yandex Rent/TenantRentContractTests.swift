//
//  TenantRentContractTests.swift
//  UI Tests
//
//  Created by Evgenii Novikov on 11.07.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig

final class TenantRentContractTests: BaseTestCase {
    override func setUp() {
        super.setUp()

        RentAPIStubConfiguration.setupServiceInfoWithShowings(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupFlatsWithOneFlat(using: self.dynamicStubs)
        YaRentContractAPIStubConfiguration.setupRentContract(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupShowingsWithRentContractSigningNotification(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupGetDownloadUrl(using: self.dynamicStubs)
        YaRentContractAPIStubConfiguration.setupSMSForSigning(using: self.dynamicStubs)
        YaRentContractAPIStubConfiguration.setupSignSMS(using: self.dynamicStubs)

        let configuration: ExternalAppConfiguration = .inAppServicesTests
        self.relaunchApp(with: configuration)
    }

    func testRentContractFlow() {
        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnFlatShowings()

        YaRentTenantShowingsSteps()
            .isContentPresented()
            .showing(at: 0)
            .isNotificationPresented()
            .tapOnNotificationAction()

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
            .ensureCommentButtonNotPresented()
            .tapOnBackButton()

        YaRentContractSteps()
            .ensurePresented()
            .ensureCommentItemNotPresented()
            .ensureTermsErrorNotPresented()
            .tapOnSignButton()
            .ensureTermsErrorPresented()
            .tapOnTermsToggle()
            .tapOnSignButton()

        SMSConfirmationSteps()
            .ensurePresented()
            .enterSMS()
            .tapOnConfirmButton()

        YaRentTenantShowingsSteps()
            .isContentPresented()
            .showing(at: 0)
            .isNotificationPresented()
            .tapOnNotificationAction()

        YaRentContractSteps()
            .ensurePresented()
            .tapOnTermsToggle()
            .tapOnSignButton()

        YaRentContractAPIStubConfiguration.setupSignSMSWithPaymentID(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupYandexRentNewPaymentWithTerms(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupFlatWithNotPaidPayment(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupYandexRentPaymentInit(using: self.dynamicStubs)

        SMSConfirmationSteps()
            .ensurePresented()
            .enterSMS()
            .tapOnConfirmButton()

        PaymentMethodsSteps()
            .isPaymentMethodsScreenPresented()
            .pay()

        RentAPIStubConfiguration.setupYandexRentPaidNextPayment(using: self.dynamicStubs)

        TinkoffPaymentsSteps()
            .isScreenPresented()
            .payWithSuccess()

        PaymentStatusSteps()
            .isScreenPresented()
            .loadingIsFinished()
            .tryExecuteAction(.done)

        YaRentContractFinishPopupSteps()
            .ensureFinishPopupPresented()
            .tapFinishPopupButton()
    }
}
