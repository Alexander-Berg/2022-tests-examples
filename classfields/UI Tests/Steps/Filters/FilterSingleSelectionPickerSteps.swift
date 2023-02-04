//
//  FilterSingleSelectionPicker.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 7/13/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import struct YREAccessibilityIdentifiers.TableBasedPickerAccessibilityIdentifier

final class FilterSingleSelectionPickerSteps {
    init(_ cellIdentifier: String) {
        self.cellIdentifier = cellIdentifier
    }

    @discardableResult
    func tapOnRow(_ value: String) -> Self {
        XCTContext.runActivity(named: "Нажимаем на элемент со значение \"\(value)\"") { _ -> Void in
            let item = ElementsProvider.obtainElement(identifier: value, in: self.pickerView)
            item.yreEnsureExistsWithTimeout()

            self.pickerView.scrollToElement(element: item, direction: .up)

            item.yreEnsureHittable()
                .yreTap()
        }

        return self
    }
    @discardableResult
    func isPickerPresented(with title: String) -> Self {
        XCTContext.runActivity(named: "Проверям, что пикер показан") { _ -> Void in
            self.pickerView
                .yreEnsureExistsWithTimeout()
                .yreEnsureVisible()

            // TODO: @arkadysmirnov. I tried but selector for Identifiers.toolbar doesn't work. I don't have time to figure out problem, that's why i'm looking for title in whole picker view.
            let title = ElementsProvider.obtainElement(identifier: title, in: self.pickerView)
            title.yreEnsureExists()
        }
        
        return self
    }

    @discardableResult
    func isPickerClosed() -> Self {
        XCTContext.runActivity(named: "Проверям, что пикер закрыт") { _ -> Void in
            self.pickerView.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnCloseButton() -> Self {
        XCTContext.runActivity(named: "Нажимае на кнопку \"Закрыть\"") { _ -> Void in
            let closeButton = ElementsProvider.obtainElement(identifier: Identifiers.closeButton, in: self.pickerView)
            closeButton.yreTap()
        }

        return self
    }

    @discardableResult
    func hasOneSelectedRow(_ value: String) -> Self {
        XCTContext.runActivity(named: "Проверяем, что элемент со значением \"\(value)\" выбран") { _ -> Void in
            let cell = self.pickerView.tables.containing(.cell, identifier: self.cellIdentifier + "." + value).element
            cell.yreEnsureExistsWithTimeout()

            XCTAssertEqual(self.pickerView.descendants(matching: .image).matching(identifier: Identifiers.selectionImage).count, 1)

            let selection = ElementsProvider.obtainElement(identifier: Identifiers.selectionImage, in: cell)
            selection.yreEnsureExistsWithTimeout()
        }

        return self
    }

    private var cellIdentifier: String
    private enum Identifiers {
        static let pickerView = TableBasedPickerAccessibilityIdentifier.SingleSelectionPicker.view
        static let selectionImage = TableBasedPickerAccessibilityIdentifier.SingleSelectionPicker.cellSelectionImageView
        static let toolbar = TableBasedPickerAccessibilityIdentifier.SingleSelectionPicker.toolbar
        static let closeButton = TableBasedPickerAccessibilityIdentifier.SingleSelectionPicker.toolbarLeftItem
    }

    private lazy var pickerView = ElementsProvider.obtainElement(identifier: Identifiers.pickerView)
}
