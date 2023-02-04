//
//  YaRentPhotoPickerDialogSteps.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 21.02.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.YaRentPhotoPickerAccessibilityIdentifiers

final class YaRentPhotoPickerDialogSteps: AnyAlertSteps {
    convenience init() {
        self.init(
            elementType: .alert,
            alertID: Identifiers.dialog
        )
    }

    @discardableResult
    func pressYesButton() -> Self {
        XCTContext.runActivity(named: "Тапаем на кнопку \"Да\"") { _ -> Void in
            self.tapOnButton(withID: Identifiers.yesAction)
        }
        return self
    }

    @discardableResult
    func pressNoButton() -> Self {
        XCTContext.runActivity(named: "Тапаем на кнопку \"Нет\"") { _ -> Void in
            self.tapOnButton(withID: Identifiers.noAction)
        }
        return self
    }

    private typealias Identifiers = YaRentPhotoPickerAccessibilityIdentifiers.Dialog
}
