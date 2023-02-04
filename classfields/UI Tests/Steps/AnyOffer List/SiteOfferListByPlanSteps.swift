//
//  SiteOfferListByPlanSteps.swift
//  UI Tests
//
//  Created by Fedor Solovev on 05.11.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import class YREAccessibilityIdentifiers.SiteOfferListByPlanAccessibilityIdentifiers

final class SiteOfferListByPlanSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что экран со списком офферов по планировке открылся") { _ -> Void in
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
        XCTContext.runActivity(named: "Проверяем, что список офферов не пуст") { _ -> Void in
            self.listStepsProvider.isListNonEmpty()
        }
        return self
    }
    
    func cell(withIndex index: Int) -> SiteOfferByPlanSnippetSteps {
        return XCTContext.runActivity(named: "Получаем элемент списка под индексом \(index)") { _ -> SiteOfferByPlanSnippetSteps in
            let cell = self.listStepsProvider.сell(withIndex: index)
            cell.yreEnsureExistsWithTimeout()

            return SiteOfferByPlanSnippetSteps(element: cell)
        }
    }

    @discardableResult
    func isSortButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем, что кнопка с сортировками нажимается") { _ -> Void in
            self.sortButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabled()
                .yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func tapSortButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку с сортировками") { _ -> Void in
            self.sortButton
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

    private lazy var screen: XCUIElement = ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.viewIdentifier)
    private lazy var callButton: XCUIElement = ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.callButtonIdentifier)

    private lazy var offerPlanTableViewCell: XCUIElement = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.cellIdentifier,
        type: .any,
        in: self.screen
    )

    private lazy var sortButton: XCUIElement = ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.sortButtonIdentifier)
    private lazy var filterButton: XCUIElement = ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.filterButtonIdentifier)

    private lazy var listStepsProvider = AnyOfferListStepsProvider(
        container: self.screen,
        cellID: AccessibilityIdentifiers.cellIdentifier
    )

    private typealias AccessibilityIdentifiers = SiteOfferListByPlanAccessibilityIdentifiers
}

extension SiteOfferListByPlanSteps: CallButtonHandler {
    @discardableResult
    func tapOnCallButton() -> Self {
        XCTContext.runActivity(named: "Нажимает на кнопку Позвонить") { _ -> Void in
            self.callButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabled()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }
}
