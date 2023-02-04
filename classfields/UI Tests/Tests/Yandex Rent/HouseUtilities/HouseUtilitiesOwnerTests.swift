//
//  HouseUtilitiesOwnerTests.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 15.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest

final class HouseUtilitiesOwnerTests: BaseTestCase {
    override func setUp() {
        super.setUp()

        InAppServicesStubConfigurator.setupEmptyServiceInfo(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupFlatsWithOneFlat(using: self.dynamicStubs)
    }

    func testDeclineMeterReadings() {
        RentAPIStubConfiguration.setupOwnerFlatWithReceivedAllMeterReadings(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupSentMeterReadingsPeriod(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupDeclineMeterReadingsWithSuccess(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatNotification(.receivedAllMeterReadings)

        let listSteps = HouseUtilitiesMetersListSteps()
            .ensureListPresented()
            .ensureListContentPresented()
            .tapOnCell(at: 0)

        HouseUtilitiesMeterReadingsSteps()
            .ensureScreenPresented()
            .ensureDetailsPresented()
            .tapDeclineReadingsButton()

        TextPickerSteps(name: "Отклонение показаний")
            .ensureScreenPresented()
            .typeText("Текст причины отклонения показаний")
            .tapOnActionButton()

        let expectation = XCTestExpectation(description: "перезапросили инфу по периоду")
        HouseUtilitiesStubConfigurator.setupDeclinedMeterReadingsPeriod(using: self.dynamicStubs, expectation: expectation)

        HouseUtilitiesMeterReadingsDeclinedPopupSteps()
            .ensureScreenPresented()
            .tapOkButton()

        listSteps
            .ensureListContentPresented()

        expectation.yreEnsureFullFilledWithTimeout()
    }

    func testNoMeterReadingsPopup() {
        RentAPIStubConfiguration.setupOwnerFlatWithReceivedAllMeterReadings(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupNotSentMeterReadingsPeriod(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatNotification(.receivedAllMeterReadings)

        let listSteps = HouseUtilitiesMetersListSteps()
            .ensureListPresented()
            .ensureListContentPresented()
            .tapOnCell(at: 0)

        HouseUtilitiesNoMeterReadingsPopupSteps()
            .ensureScreenPresented()
            .tapOkButton()

        listSteps
            .ensureListContentPresented()
    }

    func testDeclineReceiptsPhoto() {
        RentAPIStubConfiguration.setupOwnerFlatWithReceiptsReceived(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupFilledReceiptsPeriod(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupDeclineReceiptsWithSuccess(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatNotification(.receiptsReceived)

        HouseUtilitiesOwnerReceiptsSteps()
            .ensureScreenPresented()
            .ensureContentPresented()
            .tapOnDeclineButton()

        TextPickerSteps(name: "Отклонение квитанций")
            .ensureScreenPresented()
            .typeText("Текст причины отклонения квитанций")
            .tapOnActionButton()

        YaRentFlatCardSteps()
            .tapReceiptsDeclinedPopupActionButton()
    }

    func testDeclinePaymentConfirmationPhoto() {
        RentAPIStubConfiguration.setupOwnerFlatWithPaymentConfirmationReceived(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupFilledPaymentConfirmation(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupDeclinePaymentConfirmationWithSuccess(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatNotification(.paymentConfirmationReceived)

        HouseUtilitiesOwnerReceiptsSteps()
            .ensureScreenPresented()
            .ensureContentPresented()
            .tapOnDeclineButton()

        TextPickerSteps(name: "Отклонение подтверждения оплаты")
            .ensureScreenPresented()
            .typeText("Текст причины отклонения подтверждения оплаты")
            .tapOnActionButton()

        YaRentFlatCardSteps()
            .tapReceiptsDeclinedPopupActionButton()
    }

    func testBillCreation() {
        RentAPIStubConfiguration.setupOwnerFlatWithTimeToSendBill(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupEmptyPeriod(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupSendBillWithSuccess(using: self.dynamicStubs)

        let imageUploadingExpectation = XCTestExpectation(description: "фото счёта загружено")
        YaRentImageStubConfigurator.setupImageUploading(
            with: self.dynamicStubs,
            uploadingExpectation: imageUploadingExpectation,
            expectedCount: 1
        )

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatNotification(.timeToSendBills)

        let billFormSteps = HouseUtilitiesOwnerBillFormSteps()
            .ensureScreenPresented()
            .tapAddPhoto()

        ImagePickerSteps()
            .ensureScreenPresented()
            .selectPhoto(at: 0)
            .submitPhotos()

        billFormSteps
            .typeAmount(text: "1234,56")
            .tapCommentField()

        TextPickerSteps(name: "Комментарий к счёту")
            .ensureScreenPresented()
            .typeText("Тестовый комментарий к счёту")
            .tapOnActionButton()

        // Uploading image is unpredictable in time. Wait no more than 60 seconds for sure
        // 40 seconds is enough for my machine
        imageUploadingExpectation.yreEnsureFullFilledWithTimeout(
            timeout: 60,
            message: "Looks like we need more time for uploading or waiting mechanism should be fixed"
        )

        billFormSteps
            .tapSubmitButton()

        YaRentFlatCardSteps()
            .tapBillSentPopupActionButton()
    }

    func testResendDeclinedBill() {
        RentAPIStubConfiguration.setupOwnerFlatWithBillDeclined(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupDeclinedBillPeriod(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupSendBillWithSuccess(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatNotification(.billsDeclined)

        HouseUtilitiesDeclinedBillSteps()
            .ensureScreenPresented()
            .tapRebillButton()

        HouseUtilitiesOwnerBillFormSteps()
            .ensureScreenPresented()
            .tapSubmitButton()

        YaRentFlatCardSteps()
            .tapBillSentPopupActionButton()
    }

    private func openFlatNotification(_ notification: YaRentFlatNotificationSteps.NotificationType) {
        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnFirstFlat()

        YaRentFlatCardSteps()
            .isContentLoaded()

        YaRentFlatNotificationSteps(notification)
            .isPresented()
            .action()
    }
}
