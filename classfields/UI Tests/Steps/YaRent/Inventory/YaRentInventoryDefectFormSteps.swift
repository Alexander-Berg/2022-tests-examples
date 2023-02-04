//
//  YaRentInventoryDefectFormSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 09.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.YaRentInventoryAccessibilityIdentifiers

final class YaRentInventoryDefectFormSteps {
    @discardableResult
    func ensureScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие формы дефекта") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func ensureDeleteButtonHidden() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие кнопки 'Удалить'") { _ -> Void in
            self.deleteButton
                .yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func tapAddPhoto() -> Self {
        XCTContext.runActivity(named: "Нажимаем на поле добавление фотографии дефекта") { _ -> Void in
            self.screenView.scrollToElement(element: self.addPhotoField, direction: .up)

            self.addPhotoField
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapComment() -> Self {
        XCTContext.runActivity(named: "Нажимаем на поле добавление комментария к дефекту") { _ -> Void in
            self.screenView.scrollToElement(element: self.commentField, direction: .up)

            self.commentField
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnSaveButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Сохранить'") { _ -> Void in
            self.screenView.scrollToElement(element: self.saveButton, direction: .up)

            self.saveButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnDeleteButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Удалить'") { _ -> Void in
            self.screenView.scrollToElement(element: self.deleteButton, direction: .up)

            self.deleteButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = YaRentInventoryAccessibilityIdentifiers.DefectForm

    private lazy var screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)

    private lazy var addPhotoField = ElementsProvider.obtainElement(identifier: Identifiers.addPhotoField, in: self.screenView)
    private lazy var commentField = ElementsProvider.obtainElement(identifier: Identifiers.commentField, in: self.screenView)

    private lazy var saveButton = ElementsProvider.obtainElement(identifier: Identifiers.saveButton, in: self.screenView)
    private lazy var deleteButton = ElementsProvider.obtainElement(identifier: Identifiers.deleteButton, in: self.screenView)
}
