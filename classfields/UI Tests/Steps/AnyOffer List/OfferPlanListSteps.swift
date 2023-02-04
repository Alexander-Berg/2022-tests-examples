//
//  OfferPlanListSteps.swift
//  UI Tests
//
//  Created by Fedor Solovev on 02.11.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.OfferPlanListAccessibilityIdentifiers

final class OfferPlanListSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что экран со списком планировок открылся") { _ -> Void in
            self.screen.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isCallButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем, что кнопка Позвонить нажимается") { _ -> Void in
            self.callButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabled()
                .yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func isListNonEmpty() -> Self {
        XCTContext.runActivity(named: "Проверяем, что список планировок не пуст") { _ -> Void in
            self.listStepsProvider.isListNonEmpty()
        }
        return self
    }

    @discardableResult
    func isOfferPlanCellTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем, что ячейка с планировкой нажимается") { _ -> Void in
            self.offerPlanCell
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabled()
                .yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func tapOfferPlanCell() -> Self {
        XCTContext.runActivity(named: "Нажимаем на ячейку с планировкой") { _ -> Void in
            self.offerPlanCell
                .yreTap()
        }
        return self
    }

    func cell(withIndex index: Int) -> OfferPlanSnippetSteps {
        return XCTContext.runActivity(named: "Получаем элемент списка под индексом \(index)") { _ -> OfferPlanSnippetSteps in
            let cell = self.listStepsProvider.сell(withIndex: index)
            cell.yreEnsureExistsWithTimeout()

            return OfferPlanSnippetSteps(element: cell)
        }
    }

    @discardableResult
    func isSortPanelHeaderViewTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем, что панель с сортировкой нажимается") { _ -> Void in
            self.sortPanelHeaderView
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabled()
                .yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func tapSortPanelHeaderView() -> Self {
        XCTContext.runActivity(named: "Нажимаем на панель с сортировкой") { _ -> Void in
            self.sortPanelHeaderView
                .yreTap()
        }
        return self
    }

    @discardableResult
    func isFilterButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем, что кнопка с фильтрами нажимается") { _ -> Void in
            self.filterButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabled()
                .yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func tapFilterButton() -> LegacySiteSubfilterSteps {
        XCTContext.runActivity(named: "Нажимаем на кнопку с фильтрами") { _ -> Void in
            self.filterButton
                .yreTap()
        }
        return LegacySiteSubfilterSteps()
    }

    // MARK: Private

    private lazy var screen: XCUIElement = ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.view)
    private lazy var callButton: XCUIElement = ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.callButton)

    private lazy var offerPlanCell: XCUIElement = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.cell,
        type: .any,
        in: self.screen
    )

    private lazy var sortPanelHeaderView = ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.sortPanelHeaderView)

    private lazy var filterButton: XCUIElement = ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.filterButton)

    private lazy var listStepsProvider = AnyOfferListStepsProvider(
        container: self.screen,
        cellID: AccessibilityIdentifiers.cell
    )

    private typealias AccessibilityIdentifiers = OfferPlanListAccessibilityIdentifiers
}

extension OfferPlanListSteps: CallButtonHandler {
    @discardableResult
    func tapOnCallButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку Позвонить") { _ -> Void in
            self.callButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabled()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }
}
