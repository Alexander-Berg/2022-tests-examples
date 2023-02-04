//
//  SnippetAdditionalActionsSteps.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 21.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.SnippetActionsAccessibilityIdentifiers
import enum YREAccessibilityIdentifiers.SnippetActionsLocalization

final class SnippetAdditionalActionsSteps {
    enum Option {
        case offerAbuse
        case share
        case copyLink
        case addNote
        case editNote
        case hide
        case cancel
    }

    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие \"\(Self.screenTitle)\" на экране") { _ -> Void in
            self.internalSteps.screenIsPresented()
        }
        return self
    }

    @discardableResult
    func isScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие \"\(Self.screenTitle)\" на экране") { _ -> Void in
            self.internalSteps.screenIsDismissed()
        }
        return self
    }

    @discardableResult
    func isButtonPresented(_ option: Option) -> Self {
        let buttonTitle = Self.buttonTitle(option)
        XCTContext.runActivity(named: "Проверяем наличие кнопки \"\(buttonTitle)\"") { _ -> Void in
            self.internalSteps
                .obtainButton(withID: buttonTitle)
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isButtonNotPresented(_ option: Option) -> Self {
        let buttonTitle = Self.buttonTitle(option)
        XCTContext.runActivity(named: "Проверяем отсутствие кнопки \"\(buttonTitle)\"") { _ -> Void in
            self.internalSteps
                .obtainButton(withID: buttonTitle)
                .yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func tapOnButton(_ option: Option) -> Self {
        let buttonTitle = Self.buttonTitle(option)
        XCTContext.runActivity(named: "Нажимаем на кнопку \"\(buttonTitle)\"") { _ -> Void in
            self.internalSteps
                .tapOnButton(withID: buttonTitle)
        }
        return self
    }

    @discardableResult
    func confirmHideAction() -> Self {
        XCTContext.runActivity(named: "Подтверждаем действие") { _ -> Void in
            let alert = AnyAlertSteps(elementType: .sheet, alertID: Identifiers.hideItemConfirmationScreen.rawValue)
            alert
                .screenIsPresented()
                .tapOnButton(withID: "Скрыть")
                .screenIsDismissed()
        }
        return self
    }

    @discardableResult
    func declineHideAction() -> Self {
        XCTContext.runActivity(named: "Отклоняем действие") { _ -> Void in
            let alert = AnyAlertSteps(elementType: .sheet, alertID: Identifiers.hideItemConfirmationScreen.rawValue)
            alert
                .screenIsPresented()
                .tapOnButton(withID: "Отмена")
                .screenIsDismissed()
        }
        return self
    }

    @discardableResult
    func compareWithScreenshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом \"\(Self.screenTitle)\"") { _ -> Void in
            self.internalSteps.compareWithScreenshot(identifier: identifier)
        }
        return self
    }

    // MARK: - Private

    private typealias Identifiers = SnippetActionsAccessibilityIdentifiers
    private typealias Localization = SnippetActionsLocalization

    private static let screenTitle = "Меню действий"
    private lazy var internalSteps = AnyAlertSteps(elementType: .sheet, alertID: Identifiers.optionsScreen.rawValue)

    private static func buttonTitle(_ option: Option) -> String {
        switch option {
            case .offerAbuse: return Localization.offerAbuse.rawValue
            case .share: return Localization.share.rawValue
            case .copyLink: return Localization.copyLink.rawValue
            case .addNote: return Localization.addNote.rawValue
            case .editNote: return Localization.editNote.rawValue
            case .hide: return Localization.hide.rawValue
            case .cancel: return Localization.cancel.rawValue
        }
    }
}
