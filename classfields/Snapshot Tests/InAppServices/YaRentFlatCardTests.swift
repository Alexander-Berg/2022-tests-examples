//
//  YaRentFlatCardTests.swift
//  Unit Tests
//
//  Created by Denis Mamnitskii on 24.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREModel

@testable import YREYaRentFlatCardModule

final class YaRentFlatCardTests: XCTestCase {
    func testNotificationHouseUtilitiesAcceptSettings() {
        let notification: FlatNotification = .houseUtilitiesAcceptSettings(flatID: "")
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationHouseUtilitiesCounterReadingsDeclined() {
        let state: FlatNotification.HouseUtilitiesCounterReadingsDeclined = .init(
            flatID: "",
            periodID: "",
            periodDate: Self.periodDate
        )
        let notification: FlatNotification = .houseUtilitiesCounterReadingsDeclined(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationHouseUtilitiesSendCounterReadings() {
        let state: FlatNotification.HouseUtilitiesSendCounterReadings = .init(
            flatID: "",
            periodID: "",
            periodDate: Self.periodDate
        )
        let notification: FlatNotification = .houseUtilitiesSendCounterReadings(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationHouseUtilitiesSendReceipts() {
        let state: FlatNotification.HouseUtilitiesSendReceipts = .init(
            flatID: "",
            periodID: "",
            periodDate: Self.periodDate
        )
        let notification: FlatNotification = .houseUtilitiesSendReceipts(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationHouseUtilitiesReceiptsDeclined() {
        let state: FlatNotification.HouseUtilitiesReceiptsDeclined = .init(
            flatID: "",
            periodID: "",
            periodDate: Self.periodDate
        )
        let notification: FlatNotification = .houseUtilitiesReceiptsDeclined(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationHouseUtilitiesSendPaymentConfirmation() {
        let state: FlatNotification.HouseUtilitiesSendPaymentConfirmation = .init(
            flatID: "",
            periodID: "",
            periodDate: Self.periodDate
        )
        let notification: FlatNotification = .houseUtilitiesSendPaymentConfirmation(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationHouseUtilitiesPaymentConfirmationDeclined() {
        let state: FlatNotification.HouseUtilitiesPaymentConfirmationDeclined = .init(
            flatID: "",
            periodID: "",
            periodDate: Self.periodDate
        )
        let notification: FlatNotification = .houseUtilitiesPaymentConfirmationDeclined(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationHouseUtilitiesBillsReceived() {
        let state: FlatNotification.HouseUtilitiesBillsReceived = .init(
            periodID: "",
            periodDate: Self.periodDate
        )
        let notification: FlatNotification = .houseUtilitiesBillsReceived(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationHouseUtilitiesMeterReadingsReceived() {
        let periodDate: HouseUtilitiesPeriod.PeriodDate = .init(month: 1, year: 2022)
        let state: FlatNotification.HouseUtilitiesMeterReadingsReceived = .init(
            flatID: "",
            periodID: "",
            periodDate: periodDate
        )
        let notification: FlatNotification = .houseUtilitiesMeterReadingsReceived(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationHouseUtilitiesAllMeterReadingsReceived() {
        let periodDate: HouseUtilitiesPeriod.PeriodDate = .init(month: 1, year: 2022)
        let state: FlatNotification.HouseUtilitiesAllMeterReadingsReceived = .init(
            flatID: "",
            periodID: "",
            periodDate: periodDate
        )
        let notification: FlatNotification = .houseUtilitiesAllMeterReadingsReceived(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationHouseUtilitiesTimeToSendBills() {
        let periodDate: HouseUtilitiesPeriod.PeriodDate = .init(month: 1, year: 2022)
        let state: FlatNotification.HouseUtilitiesTimeToSendBills = .init(
            flatID: "",
            periodID: "",
            periodDate: periodDate
        )
        let notification: FlatNotification = .houseUtilitiesTimeToSendBills(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationHouseUtilitiesBillsPaid() {
        let periodDate: HouseUtilitiesPeriod.PeriodDate = .init(month: 2, year: 2022)
        let state: FlatNotification.HouseUtilitiesBillsPaid = .init(
            flatID: "",
            periodDate: periodDate
        )
        let notification: FlatNotification = .houseUtilitiesBillsPaid(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationHouseUtilitiesBillsDeclined() {
        let state: FlatNotification.HouseUtilitiesBillsDeclined = .init(
            flatID: "",
            periodID: ""
        )
        let notification: FlatNotification = .houseUtilitiesBillsDeclined(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationHouseUtilitiesReceiptsReceived() {
        let periodDate: HouseUtilitiesPeriod.PeriodDate = .init(month: 2, year: 2022)
        let state: FlatNotification.HouseUtilitiesReceiptsReceived = .init(
            flatID: "",
            periodID: "",
            periodDate: periodDate
        )
        let notification: FlatNotification = .houseUtilitiesReceiptsReceived(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationHouseUtilitiesPaymentConfirmationReceived() {
        let periodDate: HouseUtilitiesPeriod.PeriodDate = .init(month: 2, year: 2022)
        let state: FlatNotification.HouseUtilitiesPaymentConfirmationReceived = .init(
            flatID: "",
            periodID: "",
            periodDate: periodDate
        )
        let notification: FlatNotification = .houseUtilitiesPaymentConfirmationReceived(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationTenantRentFirstPayment() {
        let state: FlatNotification.TenantRentFirstPayment = .init(flatID: "", paymentID: "")
        let notification: FlatNotification = .tenantRentFirstPayment(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationTenantRentPaid() {
        let paidDate: Date = .init(timeIntervalSince1970: 0)
        let state: FlatNotification.TenantRentPaid = .init(flatID: "", paymentID: "", paidToDate: paidDate)
        let notification: FlatNotification = .tenantRentPaid(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationTenantRentPaymentToday() {
        let state: FlatNotification.TenantRentPaymentToday = .init(flatID: "", paymentID: "")
        let notification: FlatNotification = .tenantRentPaymentToday(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationTenantRentReadyToPay() {
        let date: Date = .init(timeIntervalSince1970: 0)
        let state: FlatNotification.TenantRentReadyToPay = .init(flatID: "", paymentID: "", paymentDate: date)
        let notification: FlatNotification = .tenantRentReadyToPay(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationTenantRentPaymentOutdated() {
        let state: FlatNotification.TenantRentPaymentOutdated = .init(flatID: "", paymentID: "", amount: 0)
        let notification: FlatNotification = .tenantRentPaymentOutdated(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerRentWaitingForPaymentDate() {
        let paymentDate: Date = .init(timeIntervalSince1970: 0)
        let state: FlatNotification.OwnerRentWaitingForPaymentDate = .init(flatID: "", paymentDate: paymentDate)
        let notification: FlatNotification = .ownerRentWaitingForPaymentDate(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerRentHoldingForPaymentDate() {
        let paymentDate: Date = .init(timeIntervalSince1970: 0)
        let state: FlatNotification.OwnerRentHoldingForPaymentDate = .init(flatID: "", paymentDate: paymentDate)
        let notification: FlatNotification = .ownerRentHoldingForPaymentDate(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerRentExpectingPayment() {
        let paymentDate: Date = .init(timeIntervalSince1970: 0)
        let state: FlatNotification.OwnerRentExpectingPayment = .init(flatID: "", paymentDate: paymentDate)
        let notification: FlatNotification = .ownerRentExpectingPayment(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerRentWaitingForPayout() {
        let notification: FlatNotification = .ownerRentWaitingForPayout(flatID: "")
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerRentCardUnavailable() {
        let notification: FlatNotification = .ownerRentCardUnavailable(flatID: "")
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerRentPayoutBroken() {
        let notification: FlatNotification = .ownerRentPayoutBroken(flatID: "")
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerRentPaidOutToCard() {
        let state: FlatNotification.OwnerRentPaidOutToCard = .init(flatID: "", maskedCardNumber: "3546")
        let notification: FlatNotification = .ownerRentPaidOutToCard(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerRentPaidOutToAccount() {
        let notification: FlatNotification = .ownerRentPaidOutToAccount(flatID: "")
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerHouseUtilitiesSettingsConfigurationRequired() {
        let notification: FlatNotification = .houseUtilitiesConfigurationRequired
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationHouseUtilitiesConfigurationIncomplete() {
        let notification: FlatNotification = .houseUtilitiesConfigurationIncomplete
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerKeysStillWithYou() {
        let notification: FlatNotification = .ownerKeysStillWithYou
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerKeysStillWithManager() {
        let notification: FlatNotification = .ownerKeysStillWithManager
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerKeysHandedOverToManager() {
        let state: FlatNotification.OwnerKeysHandedOverToManager = .init(documentID: "")
        let notification: FlatNotification = .ownerKeysHandedOverToManager(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerDraftNeedToFinish() {
        let notification: FlatNotification = .ownerDraftNeedToFinish
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerDraftNeedConfirmation() {
        let notification: FlatNotification = .ownerDraftNeedConfirmation
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerWaitingForArendaTeamContact() {
        let notification: FlatNotification = .ownerWaitingForArendaTeamContact
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerNeedToAddPassport() {
        let notification: FlatNotification = .ownerNeedToAddPassport
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerPreparingFlatForExposition() {
        let notification: FlatNotification = .ownerPreparingFlatForExposition
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerLookingForTenants() {
        let notificationValue: FlatNotification.OwnerLookingForTenants = .init(offerID: "")
        let notification: FlatNotification = .ownerLookingForTenants(notificationValue)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerCheckTenantCandidates() {
        let notification: FlatNotification = .ownerCheckTenantCandidates
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerRequestDeclined() {
        let notification: FlatNotification = .ownerRequestDeclined
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerRequestCanceled() {
        let notification: FlatNotification = .ownerRequestCanceled
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerRentOver() {
        let notification: FlatNotification = .ownerRentOver
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationTenantRentEnded() {
        let notification: FlatNotification = .tenantRentEnded(url: Self.rentURL)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerPrepareFlatForMeeting() {
        let notification: FlatNotification = .ownerPrepareFlatForMeeting
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerConfirmedTodo() {
        let ownerConfirmedTodo: FlatNotification.OwnerConfirmedTodo = .init(
            items: [
                .init(type: .addPassport, error: nil, isDone: false),
                .init(type: .addFlatPhotos, error: "ошибка загрузки", isDone: false),
                .init(type: .addFlatInfo, error: nil, isDone: true)
            ],
            hasUnknownItem: false
        )
        let notification: FlatNotification = .ownerConfirmedTodo(ownerConfirmedTodo)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerNeedToFillOutInventory() {
        let notification: FlatNotification = .ownerNeedToFillOutInventory(ownerRequestID: "")
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerNeedToConfirmInventory() {
        let notification: FlatNotification = .ownerNeedToConfirmInventory(ownerRequestID: "")
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationTenantNeedToConfirmInventory() {
        let notification: FlatNotification = .tenantNeedToConfirmInventory(ownerRequestID: "")
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    // MARK: - Non-flat notifications

    func testNotificationRentOwnerWithoutINN() {
        let notification = FlatNotification.ownerWithoutINN
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationRentOwnerWithManyCards() {
        let notification = FlatNotification.ownerWithManyCards
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationRentOwnerWithoutCard() {
        let state: FlatNotification.OwnerWithoutCard = .init(paymentInfo: nil)
        let notification = FlatNotification.ownerWithoutCard(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationRentOwnerWithoutCardWithPayment() {
        let paymentInfo = FlatNotificationPaymentInfo(amountInKopecks: 4_200_000)
        let state: FlatNotification.OwnerWithoutCard = .init(paymentInfo: paymentInfo)
        let notification = FlatNotification.ownerWithoutCard(state)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationOwnerPaymentInfoTodoTodo() {
        let ownerPaymentInfoTodo: FlatNotification.OwnerPaymentInfoTodo = .init(
            items: [
                .init(type: .addINN, error: nil, isDone: false),
                .init(type: .addPaymentCard, error: nil, isDone: true)
            ],
            hasUnknownItem: false
        )
        let notification: FlatNotification = .ownerPaymentInfoTodo(ownerPaymentInfoTodo)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    // MARK: - Notification Sheet

    func testNotificationSheet() {
        let sheetType: RentNotificationSheet.SheetType = .textLink(
            text: "👀 Вы получаете арендную плату вовремя, даже если жилец задерживает платеж.",
            link: nil
        )
        let sheet: RentNotificationSheet = .init(type: sheetType, analyticsType: "")
        let paymentDate: Date = .init(timeIntervalSince1970: 0)
        let state: FlatNotification.OwnerRentWaitingForPaymentDate = .init(flatID: "", paymentDate: paymentDate)
        let notification: RentNotification = .init(.ownerRentWaitingForPaymentDate(state), sheet: sheet)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationSheetWithLink() {
        let sheetLink: RentNotificationSheet.SheetType.Link = .init(text: "Подробнее.", url: Self.rentURL)
        let sheetType: RentNotificationSheet.SheetType = .textLink(
            text: "👀 Старайтесь платить за аренду вовремя. На всякий случай изучите условия по задержке оплаты аренды.",
            link: sheetLink
        )
        let sheet: RentNotificationSheet = .init(type: sheetType, analyticsType: "")
        let state: FlatNotification.HouseUtilitiesBillsReceived = .init(
            periodID: "",
            periodDate: Self.periodDate
        )
        let notification: RentNotification = .init(.houseUtilitiesBillsReceived(state), sheet: sheet)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    // MARK: - Common

    func testNotificationFallback() {
        let url: URL = .init(fileURLWithPath: "")
        let fallback: FallbackNotification = .init(title: "Ого, что-то произошло",
                                                   subtitle: "Нажмите на кнопку, чтобы узнать, что же это было",
                                                   action: .button(buttonText: "Кнопка", url: url))
        let notification: FlatNotification = .fallback(fallback)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testNotificationAppUpdate() {
        let fallback: FallbackNotification = .init(title: "Новая нотификация",
                                                   subtitle: "Которая не поддерживается версией приложения".yre_insertNBSPs(),
                                                   action: .appUpdate)
        let notification: FlatNotification = .fallback(fallback)
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testSatisfactionNotification() {
        let notification: FlatNotification = .netPromoterScore
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    // MARK: - Rent contract notifications

    func testOwnerRentContractSignedByOwnerNotification() {
        let notification: FlatNotification = .ownerRentContractSignedByOwner(contractID: "1")
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testOwnerRentContractChangesRequestedNotification() {
        let notification: FlatNotification = .ownerRentContractChangesRequested(contractID: "1")
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testOwnerSignRentContractNotification() {
        let notification: FlatNotification = .ownerSignRentContract(contractID: "1")
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testOwnerRentContractIsActiveNotification() {
        let notification: FlatNotification = .ownerRentContractIsActive
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }

    func testTenantRentContractIsActiveNotification() {
        let notification: FlatNotification = .tenantRentContractIsActive
        let view = Self.notificationView(for: notification)
        self.assertSnapshot(view)
    }
}

extension YaRentFlatCardTests {
    private static let periodDate: HouseUtilitiesPeriod.PeriodDate = .init(month: 1, year: 2022)
    // swiftlint:disable:next force_unwrapping
    private static let rentURL: URL = URL(string: "https://arenda.yandex.ru")!

    private static func notificationView(for notification: RentNotification) -> UIView {
        let viewModel = YaRentFlatCardNotificationViewModelGenerator.rentNotification(for: notification,
                                                                                      withFlatID: "",
                                                                                      flatStatus: .unknown)
        return Self.notificationView(with: viewModel)
    }

    private static func notificationView(for notification: FlatNotification) -> UIView {
        let configuration = YaRentFlatCardNotificationViewModelGenerator.flatNotification(for: notification,
                                                                                          withFlatID: "",
                                                                                          flatStatus: .unknown)
        let viewModel: RentNotificationViewModel = .init(notification: configuration, sheet: nil)
        return Self.notificationView(with: viewModel)
    }

    private static func notificationView(with viewModel: RentNotificationViewModel) -> UIView {
        let cell = RentNotificationCell()
        cell.configure(viewModel: viewModel)

        let frame = Self.frame(
            by: { RentNotificationCell.size(width: $0, viewModel: viewModel).height }
        )
        let view: UIView = cell.contentView
        view.frame = frame
        return view
    }
}
