//
//  YaRentInventoryObjectFormSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 08.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.YaRentInventoryAccessibilityIdentifiers

final class YaRentInventoryObjectFormSteps {
    @discardableResult
    func ensureScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие формы комнаты") { _ -> Void in
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
    func tapAddObjectPhoto() -> Self {
        XCTContext.runActivity(named: "Нажимаем на поле добавление фотографии объекта") { _ -> Void in
            self.screenView.scrollToElement(element: self.addObjectPhotoField, direction: .up)

            self.addObjectPhotoField
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func typeName(_ text: String) -> Self {
        XCTContext.runActivity(named: "Вводим название объекта \(text)") { _ -> Void in
            self.screenView.scrollToElement(element: self.nameField, direction: .up)

            self.nameField
                .yreEnsureExistsWithTimeout()
                .yreTap()
                .yreTypeText(text)
        }
        return self
    }

    @discardableResult
    func tapDoneToolbarButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку 'Готово' в тулбаре") { _ -> Void in
            self.doneToolbarButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapDefectComment() -> Self {
        XCTContext.runActivity(named: "Нажимаем на поле добавление комментария к дефекту") { _ -> Void in
            self.screenView.scrollToElement(element: self.defectCommentField, direction: .up)

            self.defectCommentField
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func toggleHasDefectSwitch() -> Self {
        XCTContext.runActivity(named: "Переключаем свитч наличия дефекта") { _ -> Void in
            self.hasDefectSwitch
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapAddDefectPhoto() -> Self {
        XCTContext.runActivity(named: "Нажимаем на поле добавление фотографии дефекта") { _ -> Void in
            self.addDefectPhotoField
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

    private typealias Identifiers = YaRentInventoryAccessibilityIdentifiers.ObjectForm

    private lazy var screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)

    private lazy var addObjectPhotoField = ElementsProvider.obtainElement(identifier: Identifiers.addObjectPhotoField, in: self.screenView)
    private lazy var nameField = ElementsProvider.obtainElement(identifier: Identifiers.objectNameField, in: self.screenView)
    private lazy var hasDefectSwitch = ElementsProvider.obtainElement(identifier: Identifiers.hasDefectSwitch, in: self.screenView)
    private lazy var addDefectPhotoField = ElementsProvider.obtainElement(identifier: Identifiers.addDefectPhotoField, in: self.screenView)
    private lazy var defectCommentField = ElementsProvider.obtainElement(identifier: Identifiers.defectCommentField, in: self.screenView)

    private lazy var saveButton = ElementsProvider.obtainElement(identifier: Identifiers.saveObjectButton, in: self.screenView)
    private lazy var deleteButton = ElementsProvider.obtainElement(identifier: Identifiers.deleteObjectButton, in: self.screenView)

    private lazy var doneToolbarButton = ElementsProvider.obtainElement(identifier: Identifiers.doneToolbarButton)
}
