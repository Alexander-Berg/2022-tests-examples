//
//  ChatActionsAlertSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 21.05.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import enum YREAccessibilityIdentifiers.ChatAccessibilityIdentifiers

final class ChatActionsAlertSteps: AnyAlertSteps {
    convenience init() {
        self.init(
            elementType: .sheet,
            alertID: Identifiers.alert
        )
    }

    @discardableResult
    func tapOnMuteButton() -> Self {
        XCTContext.runActivity(named: "Тапаем на кнопку \"Выключить уведомления\"") { _ -> Void in
            self.tapOnButton(withID: Identifiers.muteAction)
        }
        return self
    }

    @discardableResult
    func tapOnUnmuteButton() -> Self {
        XCTContext.runActivity(named: "Тапаем на кнопку \"Включить уведомления\"") { _ -> Void in
            self.tapOnButton(withID: Identifiers.unmuteAction)
        }
        return self
    }

    @discardableResult
    func tapOnBlockButton() -> Self {
        XCTContext.runActivity(named: "Тапаем на кнопку \"Заблокировать чат\"") { _ -> Void in
            self.tapOnButton(withID: Identifiers.blockAction)
        }
        return self
    }

    @discardableResult
    func tapOnUnblockButton() -> Self {
        XCTContext.runActivity(named: "Тапаем на кнопку \"Разблокировать чат\"") { _ -> Void in
            self.tapOnButton(withID: Identifiers.unblockAction)
        }
        return self
    }

    @discardableResult
    func tapOnDeleteButton() -> Self {
        XCTContext.runActivity(named: "Тапаем на кнопку \"Удалить чат\"") { _ -> Void in
            self.tapOnButton(withID: Identifiers.deleteAction)
        }
        return self
    }

    private typealias Identifiers = ChatAccessibilityIdentifiers.ActionsAlert
}
