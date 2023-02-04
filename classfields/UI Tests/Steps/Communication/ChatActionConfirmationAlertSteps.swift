//
//  ChatActionConfirmationAlertSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 21.05.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import enum YREAccessibilityIdentifiers.ChatAccessibilityIdentifiers

final class ChatActionConfirmationAlertSteps: AnyAlertSteps {
    convenience init() {
        self.init(
            elementType: .alert,
            alertID: Identifiers.alert
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

    private typealias Identifiers = ChatAccessibilityIdentifiers.ConfirmationAlert
}
