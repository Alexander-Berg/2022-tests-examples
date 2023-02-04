//
//  HouseUtilitiesTenantTests.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 10.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest

final class HouseUtilitiesTenantTests: BaseTestCase {
    override func setUp() {
        super.setUp()

        InAppServicesStubConfigurator.setupEmptyServiceInfo(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupFlatsWithOneFlat(using: self.dynamicStubs)
    }

    func testSendMeterReadings() {
        let uploadImageExpectation = XCTestExpectation(description: "загрузили картинку показаний")

        RentAPIStubConfiguration.setupTenantFlatWithMeterReadingsReadyToSend(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupReadyToSendMetersReadingsPeriod(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupSendMeterReadingsWithSuccess(using: self.dynamicStubs)
        YaRentImageStubConfigurator.setupImageUploading(
            with: self.dynamicStubs,
            uploadingExpectation: uploadImageExpectation,
            expectedCount: 1
        )

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatNotification(.sendMeterReadings)

        let listSteps = HouseUtilitiesMetersListSteps()

        listSteps
            .ensureListPresented()
            .ensureListContentPresented()
            .tapOnCell(at: 0)

        let photoSystemDialog = SystemDialogs
            .makePhotoActivity(self)
            .optional()
            .activate()

        let meterReadingsSteps = HouseUtilitiesMeterReadingsSteps()

        meterReadingsSteps
            .ensureScreenPresented()
            .ensurePickerPresented()
            .typeValue(text: "123", at: 0)
            .tapAddPhoto(at: 0)

        photoSystemDialog
            .tapOnButton(.allow)
            .deactivate()

        ImagePickerSteps()
            .ensureScreenPresented()
            .selectPhoto(at: 1) // first image is too heavy

        // Uploading image is unpredictable in time. Wait no more than 15 seconds for sure
        // 10 seconds is enough for my machine
        uploadImageExpectation.yreEnsureFullFilledWithTimeout(
            timeout: 15,
            message: "Looks like we need more time for uploading or waiting mechanism should be fixed"
        )

        let reloadPeriodExpectation = XCTestExpectation(description: "перезапросили инфу по периоду")
        HouseUtilitiesStubConfigurator.setupSendingMeterReadingsPeriod(using: self.dynamicStubs, expectation: reloadPeriodExpectation)

        meterReadingsSteps
            .tapSendReadingsButton()

        listSteps
            .ensureListPresented()
        reloadPeriodExpectation.yreEnsureFullFilledWithTimeout()
    }

    func testNotTodayPopup() {
        RentAPIStubConfiguration.setupTenantFlatWithMeterReadingsReadyToSend(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupNotSentMeterReadingsPeriod(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatNotification(.sendMeterReadings)

        let listSteps = HouseUtilitiesMetersListSteps()

        listSteps
            .ensureListPresented()
            .ensureListContentPresented()
            .tapOnCell(at: 0)

        let notTodayPopupSteps = HouseUtilitiesNotTodayPopupSteps()

        notTodayPopupSteps
            .ensureScreenPresented()
            .tapSendLaterButton()

        listSteps
            .tapOnCell(at: 0)

        notTodayPopupSteps
            .ensureScreenPresented()
            .tapSendAnywayButton()

        HouseUtilitiesMeterReadingsSteps()
            .ensureScreenPresented()
            .ensurePickerPresented()
    }

    func testEditMeterReadings() {
        RentAPIStubConfiguration.setupTenantFlatWithMeterReadingsReadyToSend(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupSendingMeterReadingsPeriod(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatNotification(.sendMeterReadings)

        HouseUtilitiesMetersListSteps()
            .ensureListPresented()
            .ensureListContentPresented()
            .tapOnCell(at: 0)

        HouseUtilitiesMeterReadingsSteps()
            .ensureScreenPresented()
            .ensureDetailsPresented()
            .tapEditReadingsButton()
            .ensurePickerPresented()
    }

    func testDeclinedMeterReadingsNotification() {
        RentAPIStubConfiguration.setupTenantFlatWithMeterReadingsDeclined(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupReadyToSendMetersReadingsPeriod(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatNotification(.meterReadingsDeclined)

        HouseUtilitiesMetersListSteps()
            .ensureListPresented()
            .ensureListContentPresented()
    }

    func testSendReceipts() {
        RentAPIStubConfiguration.setupTenantFlatWithSendReceiptPhotos(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupEmptyPeriod(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupSendReceiptsWithSuccess(using: self.dynamicStubs)

        self.testPhotoPicker(from: .sendReceipts)
    }

    func testDeclinedReceipts() {
        RentAPIStubConfiguration.setupTenantFlatWithReceiptsDeclined(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupEmptyPeriod(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatNotification(.receiptsDeclined)

        YaRentPhotoPickerSteps()
            .ensureScreenPresented()
            .ensureContentPresented()
    }

    func testPaymentConfirmation() {
        RentAPIStubConfiguration.setupTenantFlatWithSendPaymentConfirmation(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupEmptyPeriod(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupSendPaymentConfirmationWithSuccess(using: self.dynamicStubs)

        self.testPhotoPicker(from: .sendPaymentConfirmation)
    }

    func testPaymentConfirmationDeclined() {
        RentAPIStubConfiguration.setupTenantFlatWithPaymentConfirmationDeclined(using: self.dynamicStubs)
        HouseUtilitiesStubConfigurator.setupEmptyPeriod(using: self.dynamicStubs)

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatNotification(.paymentConfirmationDeclined)

        YaRentPhotoPickerSteps()
            .ensureScreenPresented()
            .ensureContentPresented()
    }

    private func testPhotoPicker(from notification: YaRentFlatNotificationSteps.NotificationType) {
        let expectation = XCTestExpectation(description: "отправили фотографии")

        YaRentImageStubConfigurator.setupImageUploading(
            with: self.dynamicStubs,
            uploadingExpectation: expectation,
            expectedCount: 2
        )

        self.relaunchApp(with: .inAppServicesTests)
        self.openFlatNotification(notification)

        let photoSystemDialog = SystemDialogs
            .makePhotoActivity(self)
            .optional()
            .activate()

        let photoPickerSteps = YaRentPhotoPickerSteps()

        photoPickerSteps
            .ensureScreenPresented()
            .ensureContentPresented()
            .tapAddPhoto()

        photoSystemDialog
            .tapOnButton(.allow)
            .deactivate()

        ImagePickerSteps()
            .ensureScreenPresented()
            .selectPhoto(at: 0)
            .selectPhoto(at: 1)
            .submitPhotos()

        // Uploading image is unpredictable in time. Wait no more than 60 seconds for sure
        // 40 seconds is enough for my machine
        expectation.yreEnsureFullFilledWithTimeout(
            timeout: 60,
            message: "Looks like we need more time for uploading or waiting mechanism should be fixed"
        )

        photoPickerSteps
            .tapSubmitButton()
        YaRentFlatCardSteps()
            .tapSuccessPopupActionButton()
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
