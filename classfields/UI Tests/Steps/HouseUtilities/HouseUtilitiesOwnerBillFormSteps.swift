//
//  HouseUtilitiesOwnerBillFormSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 21.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import enum YREAccessibilityIdentifiers.HouseUtilitiesOwnerBillFormAccessibilityIdentifiers
import XCTest

final class HouseUtilitiesOwnerBillFormSteps {
    @discardableResult
    func ensureScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем показ экрана 'Выставление счёта'") { _ -> Void in
            self.view.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapAddPhoto() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку 'добавить фото' на экране 'Выставление счёта'") { _ -> Void in
            self.addPhotoButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func typeAmount(text: String) -> Self {
        XCTContext.runActivity(named: "Вводим в поле суммы текст '\(text)' на экране 'Выставление счёта'") { _ -> Void in
            self.amountField
                .yreEnsureExistsWithTimeout()
                .yreTap()
                .yreTypeText(text)
        }
        return self
    }

    @discardableResult
    func tapCommentField() -> Self {
        XCTContext.runActivity(named: "Нажимаем на поле комментария на экране 'Выставление счёта'") { _ -> Void in
            self.commentField
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapSubmitButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку `отправить` на экране 'Выставление счёта'") { _ -> Void in
            self.submitButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = HouseUtilitiesOwnerBillFormAccessibilityIdentifiers.Form

    private lazy var view = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var addPhotoButton = ElementsProvider.obtainElement(
        identifier: Identifiers.addPhotoButton,
        in: self.view
    )
    private lazy var amountField = ElementsProvider.obtainElement(
        identifier: Identifiers.amountField,
        in: self.view
    )
    private lazy var commentField = ElementsProvider.obtainElement(
        identifier: Identifiers.commentField,
        in: self.view
    )
    private lazy var submitButton = ElementsProvider.obtainElement(
        identifier: Identifiers.submitButton,
        in: self.view
    )
}
