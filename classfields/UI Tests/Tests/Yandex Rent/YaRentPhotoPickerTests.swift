//
//  YaRentPhotoPickerTests.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 22.02.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest

final class YaRentPhotoPickerTests: BaseTestCase {
    func testEmptyPhotoPickerConfirmation() {
        let photoPickerSteps = self.openPhotoPicker(prefilled: false)

        let photoSystemDialog = SystemDialogs
            .makePhotoActivity(self)
            .optional()
            .activate()

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
            .submitPhotos()

        photoPickerSteps
            .tapCloseButton()
            .declineCloseConfirmation()
            .tapOnRemove(at: 0)
            .tapCloseButton()
            .isCloseConfirmationNotPresented()
            .ensureScreenDismissed()
    }

    func testPrefilledPhotoPickerConfirmation() {
        let photoPickerSteps = self.openPhotoPicker(prefilled: true)

        let photoSystemDialog = SystemDialogs
            .makePhotoActivity(self)
            .optional()
            .activate()

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
            .submitPhotos()

        photoPickerSteps
            .tapCloseButton()
            .declineCloseConfirmation()

        photoPickerSteps
            .tapOnRemove(at: 2)
            .tapCloseButton()
            .isCloseConfirmationNotPresented()
            .ensureScreenDismissed()
    }

    private func openPhotoPicker(prefilled: Bool) -> YaRentPhotoPickerSteps {
        InAppServicesStubConfigurator.setupEmptyServiceInfo(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupFlatsWithOneFlat(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupTenantFlatWithPaymentConfirmationDeclined(using: self.dynamicStubs)

        if prefilled {
            HouseUtilitiesStubConfigurator.setupFilledPaymentConfirmation(using: self.dynamicStubs)
        }
        else {
            HouseUtilitiesStubConfigurator.setupEmptyPeriod(using: self.dynamicStubs)
        }

        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnFirstFlat()

        YaRentFlatCardSteps()
            .isContentLoaded()

        YaRentFlatNotificationSteps(.paymentConfirmationDeclined)
            .isPresented()
            .action()

        return YaRentPhotoPickerSteps()
    }
}
