//
//  RentAPIStubConfiguration.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 10.12.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import Swifter

enum RentAPIStubConfiguration {
    // MARK: - User

    static func setupUserWithOwner(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .PATCH,
            path: Paths.user,
            filename: "rent-user-owner.debug"
        )
    }

    static func setupPatchINN(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .PATCH,
            path: Paths.user,
            filename: "patch-INN.debug"
        )
    }

    // MARK: - Cards

    static func setupOwnerCards(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.cardsOwner,
            filename: "rent-cards-owner.debug"
        )
    }

    // MARK: - Flats

    static func setupEmptyFlats(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flats,
            filename: "rent-flats-empty.debug"
        )
    }
    
    static func setupFlatsWithOneFlat(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flats,
            filename: "rent-flats-one.debug"
        )
    }

    static func setupFlatsWithAllOwnerStatuses(
        using dynamicStubs: HTTPDynamicStubs,
        withDraftFlats: Bool
    ) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flats,
            filename: withDraftFlats ? "rent-flats-owner-withDrafts.debug" : "rent-flats-owner.debug"
        )
    }

    static func setupYandexRentNewPayment(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flatPayment,
            filename: "rent-flat-new-payment.debug"
        )
    }

    static func setupYandexRentNewPaymentWithTerms(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flatPayment,
            filename: "rent-flat-new-paymentWithTerms.debug"
        )
    }

    static func setupYandexRentPaidPayment(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flatPayment,
            filename: "rent-flat-paid-payment.debug"
        )
    }

    static func setupYandexRentNewNextPayment(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flatPayment,
            filename: "rent-flat-new-nextPayment.debug"
        )
    }

    static func setupYandexRentPaidNextPayment(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flatPayment,
            filename: "rent-flat-paid-nextPayment.debug"
        )
    }

    static func setupFlatWithNotPaidPayment(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flats-with-info-notPaid.debug"
        )
    }

    static func setupFlatWithNextPayment(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flats-with-info-paid-nextPayment.debug"
        )
    }

    static func setupFlatWithPaidOutUnderGuaranteePayment(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flatPayment,
            filename: "rent-flat-paid-out-under-guarantee-payment.debug"
        )
    }

    static func setupYandexRentPaymentInit(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .POST,
            path: Paths.createFlatPayment,
            filename: "rent-payments-init.debug"
        )
    }

    // MARK: - Utilities

    static func setupYandexRentNewPaymentUtilities(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flatPayment,
            filename: "rent-flat-new-payment-utilities.debug"
        )
    }

    static func setupYandexRentPaymentInitUtilities(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .POST,
            path: Paths.createFlatPayment,
            filename: "rent-payments-init-utilities.debug"
        )
    }

    static func setupYandexRentPaidNextPaymentUtilities(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flatPayment,
            filename: "rent-flat-paid-nextPayment-utilities.debug"
        )
    }

    // MARK: - Tenant Notificatioins

    static func setupTenantFlatWithMeterReadingsReadyToSend(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-tenant-meter-readings.debug"
        )
    }

    static func setupTenantFlatWithMeterReadingsDeclined(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-tenant-meter-readings-declined.debug"
        )
    }

    static func setupTenantFlatWithSendReceiptPhotos(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-tenant-send-receipt-photos.debug"
        )
    }

    static func setupTenantFlatWithReceiptsDeclined(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-tenant-receipt-declined.debug"
        )
    }

    static func setupTenantFlatWithSendPaymentConfirmation(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-tenant-sendPaymentConfirmation.debug"
        )
    }

    static func setupTenantFlatWithPaymentConfirmationDeclined(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-tenant-paymentConfirmationDeclined.debug"
        )
    }

    static func setupTenantFlatWithRentEnded(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(method: .GET, path: Paths.flat, filename: "rent-flat-tenant-rentEnded.debug")
    }

    // MARK: - Owner Notificaitons

    static func setupOwnerFlatWithInsurance(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(method: .GET, path: Paths.flat, filename: "rent-flat-owner-insurance.debug")
    }

    static func setupOwnerFlatWithNoNotifications(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner-no-notifications.debug"
        )
    }

    static func setupOwnerFlatWithNotifications(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner.debug"
        )
    }

    static func setupOwnerFlatWithUserNotifications(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner-userNotifications.debug"
        )
    }

    static func setupOwnerFlatWithFallbacks(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner-fallbacks.debug"
        )
    }

    static func setupOwnerFlatWithKeysHandedOverToManager(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(method: .GET, path: Paths.flat, filename: "rent-flat-owner-keysHandedOverToManager.debug")
    }

    static func setupOwnerFlatWithConfirmedTodo(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner-confirmed-todo.debug"
        )
    }

    static func setupOwnerFlatWithReceivedAllMeterReadings(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner-receivedAllMeterReadings.debug"
        )
    }

    static func setupOwnerFlatWithReceiptsReceived(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner-receiptsReceived.debug"
        )
    }

    static func setupOwnerFlatWithPaymentConfirmationReceived(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner-paymentConfirmationReceived.debug"
        )
    }

    static func setupOwnerFlatWithTimeToSendBill(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner-timeToSendBills.debug"
        )
    }

    static func setupOwnerFlatWithBillDeclined(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner-billsDeclined.debug"
        )
    }

    static func setupOwnerNeedToFillOutInventory(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner-needToFillOutInventory.debug"
        )
    }

    static func setupOwnerNeedToConfirmInventory(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner-needToConfirmInventory.debug"
        )
    }

    static func setupTenantNeedToConfirmInventory(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-tenant-needToConfirmInventory.debug"
        )
    }

    static func setupOwnerFlatRentContractNotification(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner-rentContract.debug"
        )
    }

    static func setupTenantFlatWithBillsReceived(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-tenant-houseServiceBillsReceived.debug"
        )
    }

    // MARK: - Owner before signing contract

    static func setupOwnerFlatWithDraft(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner-draft.debug"
        )
    }

    static func setupOwnerFlatWithWaitingForConfirmation(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner-waiting-for-confirmation.debug"
        )
    }

    static func setupOwnerFlatWithConfirmedDraft(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner-confirmed.debug"
        )
    }

    static func setupOwnerFlatWithWorkInProgress(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner-work-in-progress.debug"
        )
    }

    static func setupOwnerFlatWithDenied(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner-denied.debug"
        )
    }

    static func setupOwnerFlatWithCanceled(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner-cancelled.debug"
        )
    }

    static func setupOwnerFlatWithLookingForTenant(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner-looking-for-tenant.debug"
        )
    }

    static func setupOwnerFlatWithAfterRent(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-owner-after-rent.debug"
        )
    }

    // MARK: - Showings

    static func setupServiceInfoWithShowings(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: "/2.0/service/info",
            filename: "service-info-showings.debug"
        )
    }

    static func setupShowingsEmpty(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.showings,
            filename: "rent-showings-empty.debug"
        )
    }

    static func setupShowingsWithoutNotifications(using dynamicStubs: HTTPDynamicStubs) {
        // offerID = 5205620753106733305
        dynamicStubs.register(
            method: .GET,
            path: Paths.showings,
            filename: "rent-showings-without-notifications.debug"
        )
    }

    static func setupShowingsWithFallbackNotification(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.showings,
            filename: "rent-showings-fallback-notification.debug"
        )
    }

    static func setupShowingsWithAppUpdateNotification(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.showings,
            filename: "rent-showings-appUpdate-notification.debug"
        )
    }

    static func setupShowingsWithHouseUtilitiesAcceptanceNotification(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.showings,
            filename: "rent-showings-houseUtilitiesAcceptance-notification.debug"
        )
    }

    static func setupShowingsWithRentContractSigningNotification(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.showings,
            filename: "rent-showings-rentContractSign-notification.debug"
        )
    }

    static func setupShowingsWithFirstPaymentNotification(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.showings,
            filename: "rent-showings-firstPayment-notification.debug"
        )
    }

    static func setupShowingsWithRoommatesSharingNotification(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.showings,
            filename: "rent-showings-roommatesSharing-notification.debug"
        )
    }

    static func setupShowingsWithEntryDateNotification(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.showings,
            filename: "rent-showings-entryDate-notification.debug"
        )
    }

    static func setupShowingsTenantCheckInDate(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .POST,
            path: Paths.showingsCheckInDate,
            filename: "commonEmptyResponse.debug"
        )
    }

    // MARK: - NetPromoterScore

    static func setupFlatWithSatisfactionNotification(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.flat,
            filename: "rent-flat-netPromoterScore.debug"
        )
    }

    static func setupSendNetPromoterScore(
        expectation: XCTestExpectation? = nil,
        validateRequest: @escaping (Int32, String?) -> Bool = { _, _ in true },
        using dynamicStubs: HTTPDynamicStubs
    ) {
        let middleware = MiddlewareBuilder()
            .callback({ request in
                guard let expectation = expectation else { return }

                DispatchQueue.main.async {
                    let data = Data(request.body)
                    guard
                        let json = try? JSONSerialization.jsonObject(with: data, options: []) as? [String: Any],
                        let score = json["score"] as? Int32
                    else { return }
                    let comment = json["comment"] as? String

                    if validateRequest(score, comment) {
                        expectation.fulfill()
                    }
                }
            })
            .respondWith(.ok(.contentsOfJSON("commonEmptyResponse.debug")))
            .build()

        dynamicStubs.register(
            method: .POST,
            path: "/2.0/rent/user/me/score",
            middleware: middleware
        )
    }

    // MARK: - Owner application
    static func setupOwnerApplicationDraft(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .GET,
            path: Paths.ownerApplicationDraft,
            filename: "ownerApplicationDraft.debug")
    }

    static func setupSaveOwnerApplicationDraft(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .PUT,
            path: Paths.ownerApplicationDraft,
            filename: "ownerApplicationDraft.debug")
    }

    // MARK: - Private
    private enum Paths {
        static let user = "/2.0/rent/user/me"
        static let flats = "/2.0/rent/user/me/flats"
        static let cardsOwner = "/2.0/rent/user/me/cards/owner"
        static let flat = "/2.0/rent/user/me/flats/\(Self.commonFlatID)"
        static let flatPayment = "/2.0/rent/user/me/flats/\(Self.commonFlatID)/payments/\(Self.commonPaymentID)"
        static let createFlatPayment = "/2.0/rent/user/me/flats/\(Self.commonFlatID)/payments/\(Self.commonPaymentID)/init"
        static let showings = "/2.0/rent/user/me/showings"
        static let showingsCheckInDate = "/2.0/rent/user/me/showings/\(Self.commonShowingID)/tenant-check-in-date"
        static let ownerApplicationDraft = "/2.0/rent/user/me/flats/draft"

        private static let commonFlatID = "flatID"
        private static let commonPaymentID = "paymentID"
        private static let commonShowingID = "showingID"
    }
}

extension RentAPIStubConfiguration {
    static func setupGetDownloadUrl(using dynamicStubs: HTTPDynamicStubs) {
        dynamicStubs.register(
            method: .POST,
            path: "/2.0/files/get-download-url",
            filename: "get-download-url-rent.debug"
        )
    }
}
