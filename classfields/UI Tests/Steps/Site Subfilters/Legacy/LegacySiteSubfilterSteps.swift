//
//  LegacySiteSubfilterSteps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 25.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREAccessibilityIdentifiers
import YRETestsUtils

final class LegacySiteSubfilterSteps {
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
    func openSingleSelectionPicker(for parameter: SingleSelectParameter) -> FilterSingleSelectionPickerSteps {
        let cell = ElementsProvider.obtainElement(
            identifier: parameter.accessibilityIdentifier,
            in: self.filtersVC
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
    func openNumberRangePicker(for parameter: NumberRangeParameter) -> FilterNumberRangePickerSteps {
        let cell = ElementsProvider.obtainElement(
            identifier: parameter.accessibilityIdentifier,
            in: self.filtersVC
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
    func submitFilters() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Показать\" на экране фильтров") { _ -> Void in
            self.submitButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func compareWithScreenshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом фильтры на экране планировки") { _ -> Void in
            let screenshot = self.filtersVC.yreWaitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
        }
        return self
    }

    // MARK: - Private

    private func scrollToElement(
        element: XCUIElement,
        velocity: CGFloat = 1.0,
        swipeLimits: UInt = 5
    ) {
        XCTContext.runActivity(named: "Скроллим к элементу") { _ -> Void in
            guard element.exists else { return }

            let table = self.filtersTable
            let submitButton = ElementsProvider.obtainElement(
                identifier: AccessibilityIdentifiers.submitButton,
                in: self.filtersVC
            )

            let submitButtonFrame = submitButton.frame

            table.scroll(
                to: element,
                adjustInteractionFrame: { $0.yreSubtract(submitButtonFrame, from: .maxYEdge) },
                velocity: velocity,
                swipeLimits: swipeLimits
            )
        }
    }

    private var filtersTable: XCUIElement {
        self.filtersVC.tables.element
    }

    private var filtersVC: XCUIElement {
        ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.view)
    }

    private var submitButton: XCUIElement {
        ElementsProvider.obtainButton(identifier: AccessibilityIdentifiers.submitButton, in: self.filtersVC)
    }

    private typealias AccessibilityIdentifiers = LegacySiteSubfilterAccessibilityIdentifiers
}
