//
//  TextPickerSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 15.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.TextPickerAccessibilityIdentifiers

final class TextPickerSteps {
    init(name: String) {
        self.name = name
    }

    @discardableResult
    func ensureScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана '\(self.name)'") { _ -> Void in
            self.viewController.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func ensureScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем закрытие экрана '\(self.name)'") { _ -> Void in
            self.viewController.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func clearText() -> Self {
        XCTContext.runActivity(named: "Очищаем текст '\(self.name)'") { _ -> Void in
            self.textField
                .yreEnsureExistsWithTimeout()
                .yreClearText()
        }
        return self
    }

    @discardableResult
    func typeText(_ text: String) -> Self {
        XCTContext.runActivity(named: "Меняем текст на \"\(text)\" на экране '\(self.name)'") { _ -> Void in
            self.textField
                .yreEnsureExistsWithTimeout()
                .yreClearText()
                .yreTypeText(text)
        }
        return self
    }

    @discardableResult
    func tapCancelButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку \"Отменить\" на экране '\(self.name)'") { _ -> Void in
            self.cancelButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnDoneButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку \"Готово\" на экране '\(self.name)'") { _ -> Void in
            self.doneButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnActionButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку действия на экране '\(self.name)'") { _ -> Void in
            self.actionButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnFirstPreset() -> Self {
        self.tapOnPreset(index: 0)
    }

    @discardableResult
    func tapOnPreset(index: Int) -> Self {
        XCTContext.runActivity(named: "Нажимает на пресет \(index) на экране '\(self.name)'") { _ -> Void in
            ElementsProvider
                .obtainElement(identifier: Identifiers.preset(at: index))
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = TextPickerAccessibilityIdentifiers

    private let name: String

    private lazy var viewController = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var cancelButton = ElementsProvider.obtainElement(identifier: Identifiers.cancelButton)
    private lazy var doneButton = ElementsProvider.obtainElement(identifier: Identifiers.doneButton)
    private lazy var actionButton = ElementsProvider.obtainElement(identifier: Identifiers.actionButton)
    private lazy var textField = ElementsProvider.obtainElement(
        identifier: Identifiers.textView,
        type: .any,
        in: viewController
    )
}
