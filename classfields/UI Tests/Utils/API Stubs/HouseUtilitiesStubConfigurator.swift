//
//  HouseUtilitiesStubConfigurator.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 11.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest
import Swifter

enum HouseUtilitiesStubConfigurator {
    static func setupEmptyPeriod(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.periodWithDetails,
            filename: "house-service-periods-emptyPeriod.debug"
        )
    }

    static func setupNotSentMeterReadingsPeriod(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.periodWithDetails,
            filename: "house-service-periods-meterReadingsNotSent.debug"
        )
    }

    static func setupReadyToSendMetersReadingsPeriod(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.periodWithDetails,
            filename: "house-service-periods-meterReadingsReadyToSend.debug"
        )
    }

    static func setupSendingMeterReadingsPeriod(using dynamicStubs: HTTPDynamicStubs, expectation: XCTestExpectation? = nil) {
        let middleware = MiddlewareBuilder()
            .callback { _ in expectation?.fulfill() }
            .respondWith(.ok(.contentsOfJSON("house-service-periods-meterReadingsSending.debug")))
            .build()

        dynamicStubs.register(
            method: .GET,
            path: Paths.periodWithDetails,
            middleware: middleware
        )
    }

    static func setupDeclinedMeterReadingsPeriod(using dynamicStubs: HTTPDynamicStubs, expectation: XCTestExpectation? = nil) {
        let middleware = MiddlewareBuilder()
            .callback { _ in expectation?.fulfill() }
            .respondWith(.ok(.contentsOfJSON("house-service-periods-meterReadingsDeclined.debug")))
            .build()

        dynamicStubs.register(
            method: .GET,
            path: Paths.periodWithDetails,
            middleware: middleware
        )
    }

    static func setupSentMeterReadingsPeriod(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.periodWithDetails,
            filename: "house-service-periods-meterReadingsSent.debug"
        )
    }

    static func setupSendMeterReadingsWithSuccess(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .PUT,
            path: Paths.sendMeterReadings,
            filename: "house-service-periods-sendMeterReadingsWithSuccess.debug"
        )
    }

    static func setupFilledReceiptsPeriod(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.periodWithDetails,
            filename: "house-service-periods-filledReceipts.debug"
        )
    }

    static func setupSendReceiptsWithSuccess(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .PUT,
            path: Paths.sendReceipts,
            filename: "house-service-periods-sendReceiptsWithSuccess.debug"
        )
    }

    static func setupFilledPaymentConfirmation(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.periodWithDetails,
            filename: "house-service-periods-filledPaymentConfirmation.debug"
        )
    }

    static func setupSendPaymentConfirmationWithSuccess(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .PUT,
            path: Paths.sendPaymentConfirmation,
            filename: "house-service-periods-sendPaymentConfirmationWithSuccess.debug"
        )
    }

    static func setupCreateBillPayment(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .POST,
            path: Paths.createBillPayment,
            filename: "house-service-periods-bills-payments.debug"
        )
    }

    static func setupBillSuccessfullyDeclined(
        predicate: Predicate<HttpRequest>,
        handler: @escaping () -> Void,
        using dynamicStubs: HTTPDynamicStubs
    ) {
        let middleware = MiddlewareBuilder.predicate(
            predicate,
            stubFilename: "house-service-periods-declineBill.debug",
            handler: handler
        ).build()
        dynamicStubs.register(
            method: .PUT,
            path: Paths.declineBill,
            middleware: middleware
        )
    }

    static func setupBillDeclineIsBlocked(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .PUT,
            path: Paths.declineBill,
            filename: "house-service-periods-declineBillIsBlocked.debug"
        )
    }

    static func setupDeclineMeterReadingsWithSuccess(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .PUT,
            path: Paths.declineMeterReadings,
            filename: "house-service-periods-declineMeterReadingsWithSuccess.debug"
        )
    }

    static func setupDeclineReceiptsWithSuccess(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .PUT,
            path: Paths.declineReceipts,
            filename: "house-service-periods-declineReceiptsWithSuccess.debug"
        )
    }

    static func setupDeclinePaymentConfirmationWithSuccess(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .PUT,
            path: Paths.declinePaymentConfirmation,
            filename: "house-service-periods-declinePaymentConfirmationWithSuccess.debug"
        )
    }

    static func setupSendBillWithSuccess(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .PUT,
            path: Paths.sendBill,
            filename: "house-service-periods-sendBill.debug"
        )
    }

    static func setupDeclinedBillPeriod(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.periodWithDetails,
            filename: "house-service-periods-declinedBill.debug"
        )
    }

    private enum Paths {
        static let periodWithDetails = "/2.0/rent/user/me/flats/\(Self.commonFlatID)/house-services/periods/\(Self.commonPeriodID)"
        static let sendMeterReadings = "2.0/rent/user/me/flats/\(Self.commonFlatID)/house-services/periods/\(Self.commonPeriodID)/\(Self.commonMeterReadingsID)"
        static let sendReceipts = "2.0/rent/user/me/flats/\(Self.commonFlatID)/house-services/periods/\(Self.commonPeriodID)/receipts"
        static let sendPaymentConfirmation = "2.0/rent/user/me/flats/\(Self.commonFlatID)/house-services/periods/\(Self.commonPeriodID)/confirmations"
        static let createBillPayment = "2.0/rent/user/me/flats/\(Self.commonFlatID)/house-services/periods/\(Self.commonPeriodID)/bills/payments"
        static let declineBill = "2.0/rent/user/me/flats/\(Self.commonFlatID)/house-services/periods/\(Self.commonPeriodID)/bills/decline"

        static let declineMeterReadings = "2.0/rent/user/me/flats/\(Self.commonFlatID)/house-services/periods/\(Self.commonPeriodID)/\(Self.commonMeterReadingsID)/decline"
        static let declineReceipts = "2.0/rent/user/me/flats/\(Self.commonFlatID)/house-services/periods/\(Self.commonPeriodID)/receipts/decline"
        static let declinePaymentConfirmation = "2.0/rent/user/me/flats/\(Self.commonFlatID)/house-services/periods/\(Self.commonPeriodID)/confirmations/decline"
        static let sendBill = "2.0/rent/user/me/flats/\(Self.commonFlatID)/house-services/periods/\(Self.commonPeriodID)/bills"

        private static let commonFlatID = "flatID"
        private static let commonPeriodID = "periodID"
        private static let commonMeterReadingsID = "meterReadingsID"
    }
}
