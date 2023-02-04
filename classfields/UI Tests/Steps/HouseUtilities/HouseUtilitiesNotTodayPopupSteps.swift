//
//  HouseUtilitiesNotTodayPopupSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 18.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import enum YREAccessibilityIdentifiers.HouseUtilitiesMetersAccessibilityIdentifiers
import XCTest

final class HouseUtilitiesNotTodayPopupSteps {
    @discardableResult
    func ensureScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана 'Рано передавать'") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapSendLaterButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Передам позже'") { _ -> Void in
            self.sendLaterButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapSendAnywayButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Всё равно передать'") { _ -> Void in
            self.sendAnywayButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = HouseUtilitiesMetersAccessibilityIdentifiers.NotTodayPopup

    private lazy var screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var sendLaterButton = ElementsProvider.obtainButton(
        identifier: Identifiers.sendLaterButton,
        in: self.screenView
    )
    private lazy var sendAnywayButton = ElementsProvider.obtainButton(
        identifier: Identifiers.sendAnywayButton,
        in: self.screenView
    )
}
