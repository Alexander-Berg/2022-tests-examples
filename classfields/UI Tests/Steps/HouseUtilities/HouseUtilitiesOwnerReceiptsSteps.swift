//
//  HouseUtilitiesOwnerReceiptsSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 16.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import enum YREAccessibilityIdentifiers.HouseUtilitiesOwnerReceiptsAI
import XCTest

final class HouseUtilitiesOwnerReceiptsSteps {
    @discardableResult
    func ensureScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана квитанций и подтверждения оплаты") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func ensureContentPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана квитанций и подтверждения оплаты") { _ -> Void in
            self.contentView
                .yreEnsureExistsWithTimeout()
                .yreEnsureVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnDeclineButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку отклонить") { _ -> Void in
            self.declineButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = HouseUtilitiesOwnerReceiptsAI

    private lazy var screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var contentView = ElementsProvider.obtainElement(identifier: Identifiers.contentView)

    private lazy var declineButton = ElementsProvider.obtainElement(
        identifier: Identifiers.declineButton,
        in: self.contentView
    )
}
