//
//  YaRentContractInputChangesSteps.swift
//  UI Tests
//
//  Created by Evgenii Novikov on 09.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.YaRentContractAccessibilityIdentifiers

final class YaRentContractInputChangesSteps {
    @discardableResult
    func ensureInputChangesPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие ввода изменений на экране") { _ -> Void in
            self.view.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func enterComment() -> Self {
        XCTContext.runActivity(named: "Вводим изменения") { _ -> Void in
            self.textView
                .yreEnsureExistsWithTimeout()
                .yreTap()
                .yreTypeText("Hello Qwerty!")
        }
        return self
    }

    @discardableResult
    func tapOnSendButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку отправки изменений") { _ -> Void in
            self.sendButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = YaRentContractAccessibilityIdentifiers

    private lazy var view = ElementsProvider.obtainElement(identifier: Identifiers.InputChanges.view)
    private lazy var textView = ElementsProvider.obtainElement(identifier: Identifiers.InputChanges.textView)
    private lazy var sendButton = ElementsProvider.obtainElement(identifier: Identifiers.InputChanges.sendButton)
}
