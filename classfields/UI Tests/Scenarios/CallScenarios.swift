//
//  CallScenarios.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 05.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest

protocol CallButtonHandler {
    @discardableResult
    func tapOnCallButton() -> Self
}

// MARK: - Common

final class CallScenarios {
    init(dynamicStubs: HTTPDynamicStubs, callButtonHandler: CallButtonHandler) {
        self.dynamicStubs = dynamicStubs
        self.callButtonHandler = callButtonHandler
    }

    private let dynamicStubs: HTTPDynamicStubs
    private let callButtonHandler: CallButtonHandler
}

extension CallScenarios {
    @discardableResult
    func runBillingOnCall(
        expectedPhoneNumber: String,
        expectedRedirectID: String
    ) -> Self {
        let analyticsAgent = AnalyticsAgent.shared
        analyticsAgent.removeAllPreviousEvents()

        let callEventLogEvent = EventLogAnalyticsEvent(
            name: "PHONE_CALL",
            payload: [
                "objectInfo": [
                    "phoneInfo": [
                        "phoneRedirectId": .string(expectedRedirectID),
                        "phone": [
                            "wholePhoneNumber": .string(expectedPhoneNumber)
                        ],
                    ],
                ],
            ]
        )
        let callMetricaEvent = MetricaAnalyticsEvent(name: "Позвонить")

        let callEventLogExpectation = analyticsAgent.expect(event: callEventLogEvent)
        let callMetricaExpectation = analyticsAgent.expect(event: callMetricaEvent)

        self.callButtonHandler
            .tapOnCallButton()

        PhoneQuerySteps()
            .isNotPresented()

        [callEventLogExpectation, callMetricaExpectation].yreEnsureFullFilledWithTimeout()

        return self
    }

    @discardableResult
    func runCallWithTwoPhoneNumbers(expectedPhones: [String]) -> Self {
        // Dirty hack to ensure the call button has finished its loading after the `runBillingOnCall` scenario
        sleep(2)

        let analyticsAgent = AnalyticsAgent.shared
        analyticsAgent.removeAllPreviousEvents()

        let callEvent = MetricaAnalyticsEvent(name: "Позвонить")
        let callExpectation = analyticsAgent.expect(event: callEvent)

        self.callButtonHandler
            .tapOnCallButton()

        PhoneQuerySteps()
            .isPresented()
            .hasOptions(expectedPhones)
            .close()
            .isNotPresented()

        callExpectation.yreEnsureFullFilledWithTimeout()

        return self
    }

    @discardableResult
    func runCallWithError() -> Self {
        self.callButtonHandler
            .tapOnCallButton()

        CallSteps()
            .waitForTopNotificationViewExistence()

        return self
    }
}

// MARK: - Offer

final class OfferCallScenarios {
    init(
        dynamicStubs: HTTPDynamicStubs,
        callButtonHandler: CallButtonHandler,
        offerID: String = Consts.offerID
    ) {
        self.internalScenarios = CallScenarios(
            dynamicStubs: dynamicStubs,
            callButtonHandler: callButtonHandler
        )
        self.dynamicStubs = dynamicStubs
        self.offerID = offerID
    }

    private enum Consts {
        static let offerID: String = "157352957988240017"
    }

    private let dynamicStubs: HTTPDynamicStubs
    private let internalScenarios: CallScenarios
    private let offerID: String
}

extension OfferCallScenarios {
    @discardableResult
    func runBillingOnCall(
        expectedPhoneNumber: String = "+74951043512",
        expectedRedirectID: String = "test_redirectID_value"
    ) -> Self {
        SnippetsListAPIStubConfigurator.setupGetOfferBillingPhoneNumbers(
            using: self.dynamicStubs,
            offerID: self.offerID
        )
        self.internalScenarios.runBillingOnCall(
            expectedPhoneNumber: expectedPhoneNumber,
            expectedRedirectID: expectedRedirectID
        )
        return self
    }

    @discardableResult
    func runCallWithTwoPhoneNumbers(
        expectedPhones: [String] = ["+74952271740", "+74952717401"]
    ) -> Self {
        SnippetsListAPIStubConfigurator.setupGetOfferTwoPhoneNumbers(
            using: self.dynamicStubs,
            offerID: self.offerID
        )
        self.internalScenarios.runCallWithTwoPhoneNumbers(
            expectedPhones: expectedPhones
        )
        return self
    }

    @discardableResult
    func runCallWithError() -> Self {
        SnippetsListAPIStubConfigurator.setupGetOfferNumbersError(
            using: self.dynamicStubs,
            offerID: self.offerID
        )
        self.internalScenarios.runCallWithError()

        return self
    }
}

// MARK: - Site

final class SiteCallScenarios {
    init(
        dynamicStubs: HTTPDynamicStubs,
        callButtonHandler: CallButtonHandler,
        siteID: String = Consts.siteID
    ) {
        self.internalScenarios = CallScenarios(
            dynamicStubs: dynamicStubs,
            callButtonHandler: callButtonHandler
        )
        self.dynamicStubs = dynamicStubs
        self.siteID = siteID
    }

    private enum Consts {
        static let siteID: String = "296015"
    }

    private let dynamicStubs: HTTPDynamicStubs
    private let internalScenarios: CallScenarios
    private let siteID: String
}

extension SiteCallScenarios {
    @discardableResult
    func runBillingOnCall(
        expectedPhoneNumber: String = "+74951043512",
        expectedRedirectID: String = "test_redirectID_value"
    ) -> Self {
        SnippetsListAPIStubConfigurator.setupGetSiteSinglePhoneNumber(
            using: self.dynamicStubs,
            siteID: self.siteID
        )
        self.internalScenarios.runBillingOnCall(
            expectedPhoneNumber: expectedPhoneNumber,
            expectedRedirectID: expectedRedirectID
        )
        return self
    }

    @discardableResult
    func runCallWithTwoPhoneNumbers(
        expectedPhones: [String] = ["+74956546546", "+74955465461"]
    ) -> Self {
        SnippetsListAPIStubConfigurator.setupGetSiteTwoPhoneNumbers(
            using: self.dynamicStubs,
            siteID: self.siteID
        )
        self.internalScenarios.runCallWithTwoPhoneNumbers(
            expectedPhones: expectedPhones
        )
        return self
    }

    @discardableResult
    func runCallWithError() -> Self {
        SnippetsListAPIStubConfigurator.setupGetSiteNumbersError(
            using: self.dynamicStubs,
            siteID: self.siteID
        )
        self.internalScenarios.runCallWithError()

        return self
    }
}

// MARK: - Village

final class VillageCallScenarios {
    init(
        dynamicStubs: HTTPDynamicStubs,
        callButtonHandler: CallButtonHandler
    ) {
        self.internalScenarios = CallScenarios(
            dynamicStubs: dynamicStubs,
            callButtonHandler: callButtonHandler
        )
        self.dynamicStubs = dynamicStubs
    }

    private let dynamicStubs: HTTPDynamicStubs
    private let internalScenarios: CallScenarios
}

extension VillageCallScenarios {
    @discardableResult
    func runBillingOnCall(
        expectedPhoneNumber: String = "+74951043512",
        expectedRedirectID: String = "test_redirectID_value"
    ) -> Self {
        SnippetsListAPIStubConfigurator.setupGetVillageOnePhoneNumber(
            using: self.dynamicStubs
        )
        self.internalScenarios.runBillingOnCall(
            expectedPhoneNumber: expectedPhoneNumber,
            expectedRedirectID: expectedRedirectID
        )
        return self
    }

    @discardableResult
    func runCallWithTwoPhoneNumbers(
        expectedPhones: [String] = ["+74951043512", "+74950435121"]
    ) -> Self {
        SnippetsListAPIStubConfigurator.setupGetVillageTwoPhoneNumbers(
            using: self.dynamicStubs
        )
        self.internalScenarios.runCallWithTwoPhoneNumbers(
            expectedPhones: expectedPhones
        )
        return self
    }

    @discardableResult
    func runCallWithError() -> Self {
        SnippetsListAPIStubConfigurator.setupGetVillageNumbersError(
            using: self.dynamicStubs
        )
        self.internalScenarios.runCallWithError()

        return self
    }
}
