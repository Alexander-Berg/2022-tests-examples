//
//  SMSConfirmationSteps.swift
//  UI Tests
//
//  Created by Evgenii Novikov on 09.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREAccessibilityIdentifiers

final class SMSConfirmationSteps {
    @discardableResult
    func ensurePresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана ввода смс") { _ -> Void in
            self.contentView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnConfirmButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку подтверждения кода") { _ -> Void in
            self.confirmButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnDoneButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку закрытия подтверждения успешного ввода СМС") { _ -> Void in
            self.submittedDoneButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func enterSMS() -> Self {
        XCTContext.runActivity(named: "Вводим код из смс") { _ -> Void in
            self.smsField
                .yreEnsureExistsWithTimeout()
                .yreTypeText("55555")
        }
        return self
    }

    private typealias Identifiers = SMSConfirmationAccessibilityIdentifiers

    private lazy var contentView = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var confirmButton = ElementsProvider.obtainElement(identifier: Identifiers.confirmButton)
    private lazy var smsField = ElementsProvider.obtainElement(identifier: Identifiers.smsField)
    private lazy var submittedDoneButton = ElementsProvider.obtainElement(identifier: Identifiers.submittedDoneButton)
}
