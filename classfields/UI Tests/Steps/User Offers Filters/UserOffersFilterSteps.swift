//
//  UserOffersFilterSteps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 19.11.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREAccessibilityIdentifiers

final class UserOffersFilterSteps {
    enum ActionType {
        case all
        case sell
        case rent

        var readableName: String {
            switch self {
                case .all: return "Все"
                case .sell: return "Продажа"
                case .rent: return "Аренда"
            }
        }
    }

    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана фильтров") { _ -> Void in
            self.filtersVC.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isScreenNotPresented(timeout: TimeInterval) -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие экрана фильтров") { _ -> Void in
            self.filtersVC.yreEnsureNotExistsWithTimeout(timeout: timeout)
        }
        return self
    }

    @discardableResult
    func switchToAction(_ action: ActionType) -> Self {
        let cell = ElementsProvider.obtainElement(
            identifier: AccessibilityIdentifiers.actionTypeCell,
            in: self.filtersVC
        )

        cell
            .yreEnsureExistsWithTimeout()
            .yreEnsureEnabled()
            .yreEnsureVisible()
            .yreEnsureHittableWithTimeout()
            .tap()

        let pickerSteps = FilterSingleSelectionPickerSteps(AccessibilityIdentifiers.actionTypeCell)
        pickerSteps
            .isPickerPresented(with: action.readableName)
            .tapOnRow(action.readableName)
        return self
    }

    @discardableResult
    func submitFilters() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Показать\" на экране фильтров") { _ -> Void in
            let submitButton = ElementsProvider.obtainButton(
                identifier: AccessibilityIdentifiers.submitButton,
                in: self.filtersVC
            )
            submitButton
                .yreEnsureExistsWithTimeout()
                .tap()
        }
        return self
    }

    // MARK: - Private

    private lazy var filtersVC: XCUIElement = {
        return XCTContext.runActivity(named: "Ищем экран фильтров") { _ -> XCUIElement in
            return ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.view)
        }
    }()

    private lazy var filtersTable = self.filtersVC.tables.element

    private typealias AccessibilityIdentifiers = UserOffersFilterAccessibilityIdentifiers
}
