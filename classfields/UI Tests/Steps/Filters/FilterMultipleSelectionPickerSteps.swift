//
//  FilterMultipleSelectionPickerSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 16.07.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import struct YREAccessibilityIdentifiers.TableBasedPickerAccessibilityIdentifier

final class FilterMultipleSelectionPickerSteps {
    @discardableResult
    func isPickerPresented(with title: String) -> Self {
        let activityName = "Проверяем наличие пикера множественного выбора для параметра \"\(title)\""
        XCTContext.runActivity(named: activityName) { _ -> Void in
            self.pickerView
                .yreEnsureExistsWithTimeout()
                .yreEnsureVisible()
            
            // @l-saveliy: Toolbar marked as not accessible element by default. So we can't obtain it.
            // Check title label directly in picker view
            ElementsProvider
                .obtainElement(identifier: title, in: self.pickerView)
                .yreEnsureExists()
        }
        return self
    }

    @discardableResult
    func isPickerClosed() -> Self {
        let activityName = "Проверяем отсутствие пикера множественного выбора"
        XCTContext.runActivity(named: activityName) { _ -> Void in
            self.pickerView.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnCloseButton() -> Self {
        let activityName = "Тапаем на кнопку \"Отмена\" на пикере множественного выбора"
        XCTContext.runActivity(named: activityName) { _ -> Void in
            ElementsProvider
                .obtainElement(identifier: Identifiers.closeButton, in: self.pickerView)
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnApplyButton() -> Self {
        let activityName = "Тапаем на кнопку \"Готово\" на пикере множественного выбора"
        XCTContext.runActivity(named: activityName) { _ -> Void in
            ElementsProvider
                .obtainElement(identifier: Identifiers.applyButton, in: self.pickerView)
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnRow(_ value: String) -> Self {
        let activityName = "Тапаем на ячейку \"\(value)\" на пикере множественного выбора"
        XCTContext.runActivity(named: activityName) { _ -> Void in
            let item = ElementsProvider
                .obtainElement(identifier: value, in: self.pickerView)
                .yreEnsureExistsWithTimeout()
            
            self.pickerView.scrollToElement(element: item, direction: .up)
            
            item.yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    private enum Identifiers {
        static let pickerView = TableBasedPickerAccessibilityIdentifier.MultipleSelectionPicker.view
        static let closeButton = TableBasedPickerAccessibilityIdentifier.MultipleSelectionPicker.toolbarLeftItem
        static let applyButton = TableBasedPickerAccessibilityIdentifier.MultipleSelectionPicker.toolbarRightItem
    }

    private lazy var pickerView = ElementsProvider.obtainElement(identifier: Identifiers.pickerView)
}
