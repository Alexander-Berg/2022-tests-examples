//
//  YaRentPaymentTests.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 10.12.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREAppConfig

final class YaRentPaymentTests: BaseTestCase {
    override func setUp() {
        super.setUp()

        InAppServicesStubConfigurator.setupServiceInfoWithTenant(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupFlatsWithOneFlat(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupYandexRentNewPayment(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupFlatWithNotPaidPayment(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupYandexRentPaymentInit(using: self.dynamicStubs)
    }

    func testPushNotificationsPromoWithEnabledPushNotificationsState() {
        self.relaunchApp(with: .inAppServicesTests)

        self.performCommonSteps()

        PaymentStatusSteps()
            .isScreenPresented()
            .loadingIsFinished()
            .tryExecuteAction(.done)
    }

    func testPushNotificationsPromoWithDisabledPushNotificationsState() {
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

    func testPaidOutUnderGuaranteePayment() {
        RentAPIStubConfiguration.setupFlatWithPaidOutUnderGuaranteePayment(using: self.dynamicStubs)
        self.relaunchApp(with: .inAppServicesTests)

        self.performCommonSteps()

        PaymentStatusSteps()
            .isScreenPresented()
            .loadingIsFinished()
            .tryExecuteAction(.done)
    }

    func testOpenTermsLink() {
        RentAPIStubConfiguration.setupYandexRentNewPaymentWithTerms(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnFirstFlat()

        YaRentFlatCardSteps()
            .isScreenPresented()
            .isContentLoaded()

        YaRentFlatNotificationSteps(.tenantFirstPayment)
            .isPresented()
            .action()

        PaymentMethodsSteps()
            .isPaymentMethodsScreenPresented()
            .tapTermsLink()

        WrappedBrowserSteps()
            .isScreenPresented()
    }

    private func performCommonSteps() {
        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnFirstFlat()

        YaRentFlatCardSteps()
            .isScreenPresented()
            .isContentLoaded()

        YaRentFlatNotificationSteps(.tenantFirstPayment)
            .isPresented()
            .action()

        PaymentMethodsSteps()
            .isPaymentMethodsScreenPresented()
            .ensureTermsLabelNotPresented()
            .pay()

        RentAPIStubConfiguration.setupYandexRentPaidNextPayment(using: self.dynamicStubs)

        TinkoffPaymentsSteps()
            .isScreenPresented()
            .payWithSuccess()
    }
}
