//
//  YaRentInventoryRoomFormSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 07.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.YaRentInventoryAccessibilityIdentifiers

final class YaRentInventoryRoomFormSteps {
    @discardableResult
    func ensureScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие формы комнаты") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func ensureNameFieldEqual(text: String) -> Self {
        XCTContext.runActivity(named: "Проверяем название комнаты совпадает с '\(text)'") { _ -> Void in
            self.nameField
                .yreEnsureExistsWithTimeout()
                .yreEnsureValueEqual(to: text)
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
    func typeName(_ text: String) -> Self {
        XCTContext.runActivity(named: "Вводим название комнаты \(text)") { _ -> Void in
            self.nameField
                .yreEnsureExists()
                .yreTap()
                .yreTypeText(text)
        }
        return self
    }

    @discardableResult
    func tapOnPreset(index: Int) -> Self {
        XCTContext.runActivity(named: "Нажимаем на пресет \(index)") { _ -> Void in
            let cellID = Identifiers.roomNamePreset(index: index)
            ElementsProvider
                .obtainElement(identifier: cellID)
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnSaveButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Сохранить'") { _ -> Void in
            self.saveButton
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnDeleteButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Удалить'") { _ -> Void in
            self.deleteButton
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = YaRentInventoryAccessibilityIdentifiers.RoomForm

    private lazy var screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var nameField = ElementsProvider.obtainElement(identifier: Identifiers.roomNameField, in: self.screenView)
    private lazy var saveButton = ElementsProvider.obtainElement(identifier: Identifiers.saveRoomButton, in: self.screenView)
    private lazy var deleteButton = ElementsProvider.obtainElement(identifier: Identifiers.deleteRoomButton, in: self.screenView)
}
