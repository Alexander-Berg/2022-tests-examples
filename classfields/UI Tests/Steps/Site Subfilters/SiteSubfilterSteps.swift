//
//  SiteSubfilterSteps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 25.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREAccessibilityIdentifiers

final class SiteSubfilterSteps {
    @discardableResult
    func isSiteSubfiltersPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие фильтров") { _ -> Void in
            self.filtersView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isSiteSubfiltersNotPresented(timeout: TimeInterval) -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие фильтров") { _ -> Void in
            self.filtersView.yreEnsureNotExistsWithTimeout(timeout: timeout)
        }
        return self
    }

    @discardableResult
    func roomsTotals(_ roomsTotals: [RoomsTotal]) -> Self {
        for roomsTotal in roomsTotals {
            self.roomsTotal(roomsTotal)
        }
        return self
    }

    @discardableResult
    func isRoomsTotalsSelected(_ roomsTotals: [RoomsTotal]) -> Self {
        for roomsTotal in roomsTotals {
            self.isRoomsTotalSelected(roomsTotal)
        }
        return self
    }

    @discardableResult
    func isRoomsTotalsNotSelected(_ roomsTotals: [RoomsTotal]) -> Self {
        for roomsTotal in roomsTotals {
            self.isRoomsTotalNotSelected(roomsTotal)
        }
        return self
    }

    @discardableResult
    func priceParameter(
        _ parameter: PriceParameter,
        hasValue value: String
    ) -> Self {
        XCTContext.runActivity(named: "Сравниваем значение параметра \(parameter.readableName) со значением '\(value)'") { _ -> Void in
            let cell = ElementsProvider.obtainElement(identifier: parameter.accessibilityIdentifier, in: self.filtersView)
            cell.yreEnsureExistsWithTimeout()

            let valueText = ElementsProvider.obtainElement(identifier: value, in: cell)
            valueText.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func openSingleSelectionPicker(for parameter: SingleSelectParameter) -> FilterSingleSelectionPickerSteps {
        let cell = ElementsProvider.obtainElement(
            identifier: parameter.accessibilityIdentifier,
            in: self.filtersView
        )

        self.scrollToElement(element: cell)

        cell
            .yreEnsureExistsWithTimeout()
            .yreEnsureEnabled()
            .yreEnsureVisible()
            .yreEnsureHittableWithTimeout()
            .tap()
        return FilterSingleSelectionPickerSteps(parameter.accessibilityIdentifier)
    }

    @discardableResult
    func singleSelectionParameter(
        _ parameter: SingleSelectParameter,
        hasValue value: String
    ) -> Self {
        XCTContext.runActivity(named: "Сравниваем значение параметра \"\(parameter.readableName)\" со значением '\(value)'") { _ -> Void in
            let cell = ElementsProvider.obtainElement(identifier: parameter.accessibilityIdentifier, in: self.filtersView)
            cell.yreEnsureExistsWithTimeout()

            self.scrollToElement(element: cell)

            let valueText = ElementsProvider.obtainElement(identifier: value, in: cell)
            valueText.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func openNumberRangePicker(for parameter: NumberRangeParameter) -> FilterNumberRangePickerSteps {
        let cell = ElementsProvider.obtainElement(
            identifier: parameter.accessibilityIdentifier,
            in: self.filtersView
        )

        self.scrollToElement(element: cell)

        cell
            .yreEnsureExistsWithTimeout()
            .yreEnsureEnabled()
            .yreEnsureVisible()
            .yreEnsureHittableWithTimeout()
            .tap()
        return FilterNumberRangePickerSteps()
    }

    @discardableResult
    func numberRangeParameter(
        _ parameter: NumberRangeParameter,
        hasValue value: String
    ) -> Self {
        XCTContext.runActivity(named: "Сравниваем значение параметра \"\(parameter.readableName)\" со значением '\(value)'") { _ -> Void in
            let cell = ElementsProvider.obtainElement(identifier: parameter.accessibilityIdentifier, in: self.filtersView)
            cell.yreEnsureExistsWithTimeout()

            self.scrollToElement(element: cell)

            let valueText = ElementsProvider.obtainElement(identifier: value, in: cell)
            valueText.yreEnsureExistsWithTimeout()
        }
        return self
    }

    // MARK: - Private

    private func roomsTotal(_ roomsTotal: RoomsTotal) {
        XCTContext.runActivity(named: "Нажимаем на кнопку '\(roomsTotal.readableName)'") { _ -> Void in
            let button = ElementsProvider.obtainButton(identifier: roomsTotal.accessibilityIdentifier,
                                                       in: self.filtersView)
            button.yreEnsureExists().tap()
        }
    }

    private func isRoomsTotalSelected(_ roomsTotal: RoomsTotal) {
        XCTContext.runActivity(named: "Проверяем, что кнопка '\(roomsTotal.readableName)' выбрана") { _ -> Void in
            let button = ElementsProvider.obtainButton(identifier: roomsTotal.accessibilityIdentifier,
                                                       in: self.filtersView)
            button.yreEnsureSelected()
        }
    }

    private func isRoomsTotalNotSelected(_ roomsTotal: RoomsTotal) {
        XCTContext.runActivity(named: "Проверяем, что кнопка '\(roomsTotal.readableName)' не выбрана") { _ -> Void in
            let button = ElementsProvider.obtainButton(identifier: roomsTotal.accessibilityIdentifier,
                                                       in: self.filtersView)
            button.yreEnsureNotSelected()
        }
    }

    private func scrollToElement(element: XCUIElement) {
        XCTContext.runActivity(named: "Скроллим к элементу") { _ -> Void in
            guard element.exists else { return }
            self.filtersTable.scroll(to: element)
        }
    }

    private lazy var filtersView: XCUIElement = {
        return XCTContext.runActivity(named: "Ищем фильтры") { _ -> XCUIElement in
            return ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.view)
        }
    }()

    private lazy var filtersTable: XCUIElement = {
        self.filtersView.tables.element
    }()

    private typealias AccessibilityIdentifiers = SiteSubfilterAccessibilityIdentifiers
}
