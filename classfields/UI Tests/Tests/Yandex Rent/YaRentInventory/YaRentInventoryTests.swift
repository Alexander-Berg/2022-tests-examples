//
//  YaRentInventoryTests.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 03.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import YREAppConfig
import XCTest

final class YaRentInventoryTests: BaseTestCase {
    override func setUp() {
        super.setUp()

        InAppServicesStubConfigurator.setupEmptyServiceInfo(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupFlatsWithOneFlat(using: self.dynamicStubs)
    }

    func testOwnerAddRoom() {
        self.setUpTestForInventory(state: .initial)
        let roomListSteps = YaRentInventoryRoomListSteps()
            .ensureListContentPresented()
            .tapAddRoomButton()

        let addRoomRequestExpectation = XCTestExpectation(description: "отправка запроса на сохранение комнаты")
        YaRentInventoryStubConfigurator.setupEditInventory(using: self.dynamicStubs, expectation: addRoomRequestExpectation)

        YaRentInventoryRoomFormSteps()
            .ensureScreenPresented()
            .typeName("Тест")
            .ensureNameFieldEqual(text: "Тест")
            .tapOnPreset(index: 0)
            .ensureNameFieldEqual(text: "Гостиная")
            .tapOnPreset(index: 1)
            .ensureNameFieldEqual(text: "Комната")
            .ensureDeleteButtonHidden()
            .tapOnSaveButton()

        addRoomRequestExpectation.yreEnsureFullFilledWithTimeout()

        roomListSteps
            .ensureListContentPresented()
    }

    func testOwnerEditRoom() {
        self.setUpTestForInventory(state: .initial)

        let deleteRoomRequestExpectation = XCTestExpectation(description: "отправка запроса на удаление комнаты")
        YaRentInventoryStubConfigurator.setupEditInventory(using: self.dynamicStubs, expectation: deleteRoomRequestExpectation)

        let roomListSteps = YaRentInventoryRoomListSteps()
            .ensureListContentPresented()
            .tapEditRoomButton(index: 0)

        let formSteps = YaRentInventoryRoomFormSteps()
            .ensureScreenPresented()
            .tapOnDeleteButton()

        YaRentInventoryDeleteDialogSteps()
            .screenIsPresented()
            .pressCancelButton()

        formSteps
            .ensureScreenPresented()
            .tapOnDeleteButton()

        YaRentInventoryDeleteDialogSteps()
            .screenIsPresented()
            .pressDeleteButton()

        deleteRoomRequestExpectation.yreEnsureFullFilledWithTimeout()

        roomListSteps
            .ensureListContentPresented()
    }

    func testOwnerAddObject() {
        self.setUpTestForInventory(state: .initial)

        let addObjectRequestExpectation = XCTestExpectation(description: "отправка запроса на добавление объекта")
        YaRentInventoryStubConfigurator.setupEditInventory(using: self.dynamicStubs, expectation: addObjectRequestExpectation)

        let objectPhotosExpectation = XCTestExpectation(description: "загрузка фотографий объекта")
        YaRentImageStubConfigurator.setupImageUploading(
            with: self.dynamicStubs,
            uploadingExpectation: objectPhotosExpectation,
            expectedCount: 2 + 2
        )

        YaRentInventoryRoomListSteps()
            .ensureListContentPresented()
            .tapAddObjectButton(index: 0)

        let photoSystemDialog = SystemDialogs
            .makePhotoActivity(self)
            .optional()
            .activate()

        let objectFormSteps = YaRentInventoryObjectFormSteps()
            .ensureScreenPresented()
            .tapAddObjectPhoto()

        photoSystemDialog
            .tapOnButton(.allow)
            .deactivate()

        ImagePickerSteps()
            .ensureScreenPresented()
            .selectPhoto(at: 0)
            .selectPhoto(at: 1)
            .submitPhotos()

        objectFormSteps
            .toggleHasDefectSwitch()
            .tapAddDefectPhoto()

        ImagePickerSteps()
            .ensureScreenPresented()
            .selectPhoto(at: 0)
            .selectPhoto(at: 1)
            .submitPhotos()

        objectFormSteps
            .typeName("Тест")
            .tapDoneToolbarButton()
            .tapDefectComment()

        TextPickerSteps(name: "Описание дефекта")
            .ensureScreenPresented()
            .typeText("Описание дефекта. Оно будет вот таким вот. Смотри и радуйся.")
            .tapOnActionButton()

        objectPhotosExpectation.yreEnsureFullFilledWithTimeout(timeout: 60)

        objectFormSteps
            .ensureDeleteButtonHidden()
            .tapOnSaveButton()

        addObjectRequestExpectation.yreEnsureFullFilledWithTimeout()
    }

    func testOwnerEditObject() {
        self.setUpTestForInventory(state: .filled)

        let roomListSteps = YaRentInventoryRoomListSteps()
            .ensureListContentPresented()
            .tapObject(section: 0, index: 0)

        let formSteps = YaRentInventoryObjectFormSteps()
            .ensureScreenPresented()
            .tapOnDeleteButton()

        YaRentInventoryDeleteDialogSteps()
            .screenIsPresented()
            .pressCancelButton()

        formSteps
            .ensureScreenPresented()
            .tapOnDeleteButton()

        YaRentInventoryDeleteDialogSteps()
            .screenIsPresented()
            .pressDeleteButton()

        roomListSteps
            .ensureListContentPresented()
    }

    func testOwnerAddDefect() {
        self.setUpTestForInventory(state: .initial)

        YaRentInventoryRoomListSteps()
            .ensureListContentPresented()
            .tapContinueButton()

        let uploadPhotosEpectation = XCTestExpectation(description: "отправка фотографий дефекта")
        YaRentImageStubConfigurator.setupImageUploading(
            with: self.dynamicStubs,
            uploadingExpectation: uploadPhotosEpectation,
            expectedCount: 2
        )

        let addDefectRequestExpectation = XCTestExpectation(description: "отправка запроса на добавление дефекта")
        YaRentInventoryStubConfigurator.setupEditInventory(using: self.dynamicStubs, expectation: addDefectRequestExpectation)

        YaRentInventoryDefectListSteps()
            .ensureScreenPresented()
            .tapAddDefectButton()

        let defectFormSteps = YaRentInventoryDefectFormSteps()
            .ensureScreenPresented()
            .ensureDeleteButtonHidden()

        let photoSystemDialog = SystemDialogs
            .makePhotoActivity(self)
            .optional()
            .activate()

        defectFormSteps
            .tapAddPhoto()

        photoSystemDialog
            .tapOnButton(.allow)
            .deactivate()

        ImagePickerSteps()
            .ensureScreenPresented()
            .selectPhoto(at: 1)
            .selectPhoto(at: 2)
            .submitPhotos()

        defectFormSteps
            .tapComment()

        TextPickerSteps(name: "Описание дефекта")
            .ensureScreenPresented()
            .typeText("Описание дефекта. Оно будет вот таким вот. Смотри и радуйся.")
            .tapOnActionButton()

        uploadPhotosEpectation.yreEnsureFullFilledWithTimeout(timeout: 60)

        defectFormSteps
            .tapOnSaveButton()

        addDefectRequestExpectation.yreEnsureFullFilledWithTimeout()
    }

    func testOwnerEditDefect() {
        self.setUpTestForInventory(state: .filled)

        YaRentInventoryRoomListSteps()
            .ensureListContentPresented()
            .tapContinueButton()

        let editDefectRequestExpectation = XCTestExpectation(description: "отправка запроса на редактирование дефекта")
        YaRentInventoryStubConfigurator.setupEditInventory(using: self.dynamicStubs, expectation: editDefectRequestExpectation)

        YaRentInventoryDefectListSteps()
            .ensureScreenPresented()
            .tapDefect(index: 0)

        let formSteps = YaRentInventoryDefectFormSteps()
            .ensureScreenPresented()
            .tapOnDeleteButton()

        YaRentInventoryDeleteDialogSteps()
            .screenIsPresented()
            .pressCancelButton()

        formSteps
            .ensureScreenPresented()
            .tapOnDeleteButton()

        YaRentInventoryDeleteDialogSteps()
            .screenIsPresented()
            .pressDeleteButton()

        editDefectRequestExpectation.yreEnsureFullFilledWithTimeout()
    }

    func testOwnerPreviewObject() {
        self.setUpTestForInventory(state: .filled)

        YaRentInventoryRoomListSteps()
            .ensureListContentPresented()
            .tapContinueButton()

        YaRentInventoryDefectListSteps()
            .ensureScreenPresented()
            .tapContinueButton()

        YaRentInventoryPreviewListSteps()
            .ensureScreenPresented()
            .tapObject(section: 0, index: 0)

        YaRentInventoryObjectDetailsSteps()
            .ensureScreenPresented()
            .tapPhoto(index: 0)

        AnyOfferGallerySteps()
            .isPhotoVisible()
    }

    func testOwnerPreviewDefect() {
        self.setUpTestForInventory(state: .filled)

        YaRentInventoryRoomListSteps()
            .ensureListContentPresented()
            .tapContinueButton()

        YaRentInventoryDefectListSteps()
            .ensureScreenPresented()
            .tapContinueButton()

        YaRentInventoryPreviewListSteps()
            .ensureScreenPresented()
            .tapDefect(index: 0)

        YaRentInventoryObjectDetailsSteps()
            .ensureScreenPresented()
            .tapPhoto(index: 0)

        AnyOfferGallerySteps()
            .isPhotoVisible()
    }

    func testOwnerManagerComment() {
        self.setUpTestForInventory(state: .filled)

        let roomListSteps = YaRentInventoryRoomListSteps()
            .ensureListContentPresented()
            .tapManagerComment()

        YaRentInventoryManagerCommentPopupSteps()
            .ensureScreenPresented()
            .dismissScreen()

        roomListSteps
            .tapContinueButton()

        YaRentInventoryDefectListSteps()
            .ensureScreenPresented()
            .tapContinueButton()

        YaRentInventoryPreviewListSteps()
            .ensureScreenPresented()
            .tapManagerComment()

        YaRentInventoryManagerCommentPopupSteps()
            .ensureScreenPresented()
    }

    func testOwnerConfirmInventory() {
        self.setUpTestForInventory(state: .ownerNeedToConfirm)

        YaRentInventoryPreviewListSteps()
            .ensureScreenPresented()
            .tapContinueButton()

        SMSConfirmationSteps()
            .ensurePresented()
            .enterSMS()
            .tapOnConfirmButton()

        YaRentFlatCardSteps()
            .tapSuccessPopupActionButton()
    }

    func testTenantConfirmInventory() {
        self.setUpTestForInventory(state: .tenantNeedToConfirm)

        YaRentInventoryPreviewListSteps()
            .ensureScreenPresented()
            .ensureManagerCommentHidden()
            .tapContinueButton()

        SMSConfirmationSteps()
            .ensurePresented()
            .enterSMS()
            .tapOnConfirmButton()

        YaRentFlatCardSteps()
            .tapSuccessPopupActionButton()
    }

    private enum InventoryState {
        case initial
        case filled
        case ownerNeedToConfirm
        case tenantNeedToConfirm
    }

    private func setUpTestForInventory(state: InventoryState) {
        switch state {
            case .initial:
                RentAPIStubConfiguration.setupOwnerNeedToFillOutInventory(using: self.dynamicStubs)
                YaRentInventoryStubConfigurator.setupInitialInventory(using: self.dynamicStubs)
                self.launchInventory(notificationType: .ownerNeedToFillOutInventory)
            case .filled:
                RentAPIStubConfiguration.setupOwnerNeedToFillOutInventory(using: self.dynamicStubs)
                YaRentInventoryStubConfigurator.setupFilledInventory(using: self.dynamicStubs)
                self.launchInventory(notificationType: .ownerNeedToFillOutInventory)
            case .ownerNeedToConfirm:
                RentAPIStubConfiguration.setupOwnerNeedToConfirmInventory(using: self.dynamicStubs)
                YaRentInventoryStubConfigurator.setupOwnerNeedToConfirmInventory(using: self.dynamicStubs)
                YaRentInventoryStubConfigurator.setupRequestConfirmationSMSInfo(using: self.dynamicStubs)
                YaRentInventoryStubConfigurator.setupSubmitConfirmation(using: self.dynamicStubs)
                self.launchInventory(notificationType: .ownerNeedToConfirmInventory)
            case .tenantNeedToConfirm:
                RentAPIStubConfiguration.setupTenantNeedToConfirmInventory(using: self.dynamicStubs)
                YaRentInventoryStubConfigurator.setupTenantNeedToConfirmInventory(using: self.dynamicStubs)
                YaRentInventoryStubConfigurator.setupRequestConfirmationSMSInfo(using: self.dynamicStubs)
                YaRentInventoryStubConfigurator.setupSubmitConfirmation(using: self.dynamicStubs)
                self.launchInventory(notificationType: .tenantNeedToConfirmInventory)
        }
    }

    private func launchInventory(notificationType: YaRentFlatNotificationSteps.NotificationType) {
        self.relaunchApp(with: .inAppServicesTests)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnFirstFlat()

        YaRentFlatCardSteps()
            .isScreenPresented()
            .isContentLoaded()

        YaRentFlatNotificationSteps(notificationType)
            .isPresented()
            .action()
    }
}
