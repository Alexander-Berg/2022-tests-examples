//
//  PlacePickerSteps.swift
//  UI Tests
//
//  Created by Alexey Salangin on 17.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREAccessibilityIdentifiers

final class PlacePickerSteps {
    @discardableResult
    func tapSubmitButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку подтверждения адреса") { _ in
            let submitButton = ElementsProvider.obtainElement(identifier: PlacePickerAccessibilityIdentifiers.submitButton)
            submitButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
            return self
        }
    }

    @discardableResult
    func waitAddressLoading() -> Self {
        XCTContext.runActivity(named: "Ждём загрузку адреса") { _ in
            let button = ElementsProvider.obtainElement(identifier: PlacePickerAccessibilityIdentifiers.submitButton)
            button
                .yreEnsureWithTimeout(\.label, .equalTo, "Этот адрес")

            return self
        }
    }
}
