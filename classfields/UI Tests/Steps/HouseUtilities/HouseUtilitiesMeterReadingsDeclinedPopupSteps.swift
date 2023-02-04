//
//  HouseUtilitiesMeterReadingsDeclinedSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 16.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import enum YREAccessibilityIdentifiers.HouseUtilitiesMetersAccessibilityIdentifiers
import XCTest

final class HouseUtilitiesMeterReadingsDeclinedPopupSteps {
    @discardableResult
    func ensureScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана 'Показания отклонены'") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOkButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Хорошо'") { _ -> Void in
            self.okButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = HouseUtilitiesMetersAccessibilityIdentifiers.MeterReadingsDeclinedPopup

    private lazy var screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var okButton = ElementsProvider.obtainButton(
        identifier: Identifiers.okButton,
        in: self.screenView
    )
}
