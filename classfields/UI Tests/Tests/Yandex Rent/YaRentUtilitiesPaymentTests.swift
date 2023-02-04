//
//  YaRentUtilitiesPaymentTests.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 01.02.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREAppConfig
import Swifter

final class YaRentUtilitiesPaymentTests: BaseTestCase {
    override func setUp() {
        super.setUp()

        InAppServicesStubConfigurator.setupServiceInfoWithTenant(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupFlatsWithOneFlat(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupTenantFlatWithBillsReceived(using: self.dynamicStubs)
    }

    func testSuccessDeclinePayment() {
        HouseUtilitiesStubConfigurator.setupSentMeterReadingsPeriod(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupCreateBillPayment(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnFirstFlat()

        YaRentFlatCardSteps()
            .isContentLoaded()

        YaRentFlatNotificationSteps(.houseUtilitiesBillsReceived)
            .action()

        let declineText = "Текст с причиной отклонения"
        let expectedBody = DeclineRequestBody(reasonForDecline: declineText)
        let predicate = Predicate<HttpRequest>.body(expectedBody)
        let expectation = XCTestExpectation()
        HouseUtilitiesStubConfigurator.setupBillSuccessfullyDeclined(
            predicate: predicate,
            handler: { expectation.fulfill() },
            using: self.dynamicStubs
        )

        HouseUtilitiesBillDetailsSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapDeclineButton()
            .ensureScreenPresented()
            .typeText(declineText)
            .tapOnActionButton()

        let result = XCTWaiter.yreWait(for: [expectation], timeout: Constants.timeout)
        XCTAssertTrue(result)

        HouseUtilitiesBillDeclinedPopupSteps()
            .isScreenPresented()
            .tapContinueButton()
            .isScreenNotPresented()
    }

    func testBillDeclineIsBlocked() {
        HouseUtilitiesStubConfigurator.setupSentMeterReadingsPeriod(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupCreateBillPayment(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupBillDeclineIsBlocked(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnFirstFlat()

        YaRentFlatCardSteps()
            .isContentLoaded()

        YaRentFlatNotificationSteps(.houseUtilitiesBillsReceived)
            .action()

        let declineText = "Текст с причиной отклонения"
        HouseUtilitiesBillDetailsSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapDeclineButton()
            .ensureScreenPresented()
            .typeText(declineText)
            .tapOnActionButton()

        TopNotificationToastViewSteps()
            .waitForTopNotificationViewExistence()
    }

    func testPushNotificationsPromoWithEnabledPushNotificationsState() {
        HouseUtilitiesStubConfigurator.setupSentMeterReadingsPeriod(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupCreateBillPayment(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupYandexRentNewPaymentUtilities(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupYandexRentPaymentInitUtilities(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        self.performCommonSteps()

        PaymentStatusSteps()
            .isScreenPresented()
            .loadingIsFinished()
            .tryExecuteAction(.done)
    }

    func testPushNotificationsPromoWithDisabledPushNotificationsState() {
        HouseUtilitiesStubConfigurator.setupSentMeterReadingsPeriod(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupCreateBillPayment(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupYandexRentNewPaymentUtilities(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupYandexRentPaymentInitUtilities(using: self.dynamicStubs)

        let configuration = ExternalAppConfiguration.inAppServicesTests
        configuration.pushPermissionsState = .disabled
        configuration.didDisplayPushNotificationsPromo = true
        configuration.shouldUpdatePushPermissionsState = false
        self.relaunchApp(with: configuration)

        self.performCommonSteps()

        PaymentStatusSteps()
            .isScreenPresented()
            .loadingIsFinished()
            .tryExecuteAction(.done)
    }

    func testPushNotificationsPromoWithDisabledPushNotificationsStateAskFirstTime() {
        HouseUtilitiesStubConfigurator.setupSentMeterReadingsPeriod(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupCreateBillPayment(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupYandexRentNewPaymentUtilities(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupYandexRentPaymentInitUtilities(using: self.dynamicStubs)

        let configuration = ExternalAppConfiguration.inAppServicesTests
        configuration.pushPermissionsState = .disabled
        configuration.didDisplayPushNotificationsPromo = false
        configuration.shouldUpdatePushPermissionsState = false
        self.relaunchApp(with: configuration)

        self.performCommonSteps()

        PaymentStatusSteps()
            .isScreenPresented()
            .loadingIsFinished()
            .tryExecuteAction(.openSettings)
    }

    func testPushNotificationsPromoWithProvisionalPushNotificationsStateAskFirstTime() {
        HouseUtilitiesStubConfigurator.setupSentMeterReadingsPeriod(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupCreateBillPayment(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupYandexRentNewPaymentUtilities(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupYandexRentPaymentInitUtilities(using: self.dynamicStubs)

        let configuration = ExternalAppConfiguration.inAppServicesTests
        configuration.pushPermissionsState = .provisional
        configuration.didDisplayPushNotificationsPromo = false
        configuration.shouldUpdatePushPermissionsState = false
        self.relaunchApp(with: configuration)

        self.performCommonSteps()

        PaymentStatusSteps()
            .isScreenPresented()
            .loadingIsFinished()
            .tryExecuteAction(.requestPushPermissions)
    }

    func testPushNotificationsPromoWithProvisionalPushNotificationsState() {
        HouseUtilitiesStubConfigurator.setupSentMeterReadingsPeriod(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupCreateBillPayment(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupYandexRentNewPaymentUtilities(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupYandexRentPaymentInitUtilities(using: self.dynamicStubs)

        let configuration = ExternalAppConfiguration.inAppServicesTests
        configuration.pushPermissionsState = .provisional
        configuration.didDisplayPushNotificationsPromo = true
        configuration.shouldUpdatePushPermissionsState = false
        self.relaunchApp(with: configuration)

        self.performCommonSteps()

        PaymentStatusSteps()
            .isScreenPresented()
            .loadingIsFinished()
            .tryExecuteAction(.done)
    }

    private func performCommonSteps() {
        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnFirstFlat()

        YaRentFlatCardSteps()
            .isContentLoaded()

        YaRentFlatNotificationSteps(.houseUtilitiesBillsReceived)
            .action()

        HouseUtilitiesBillDetailsSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapPayButton()

        PaymentMethodsSteps()
            .isPaymentMethodsScreenPresented()
            .pay()

        RentAPIStubConfiguration.setupYandexRentPaidNextPaymentUtilities(using: self.dynamicStubs)

        TinkoffPaymentsSteps()
            .isScreenPresented()
            .payWithSuccess()
    }
}

private struct DeclineRequestBody: Codable & Equatable {
    let reasonForDecline: String
}
