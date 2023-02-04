//
//  YaRentInventoryDeleteDialogSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 08.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.YaRentInventoryAccessibilityIdentifiers

final class YaRentInventoryDeleteDialogSteps: AnyAlertSteps {
    convenience init() {
        self.init(
            elementType: .alert,
            alertID: Identifiers.dialog
        )
    }

    @discardableResult
    func pressDeleteButton() -> Self {
        XCTContext.runActivity(named: "Тапаем на кнопку 'Удалить'") { _ -> Void in
            self.tapOnButton(withID: Identifiers.deleteButton)
        }
        return self
    }

    @discardableResult
    func pressCancelButton() -> Self {
        XCTContext.runActivity(named: "Тапаем на кнопку 'Отменить'") { _ -> Void in
            self.tapOnButton(withID: Identifiers.cancelButton)
        }
        return self
    }

    private typealias Identifiers = YaRentInventoryAccessibilityIdentifiers.DeleteDialog
}
