//
//  Created by Alexey Aleshkov on 23/07/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREAccessibilityIdentifiers

final class FilterNumberRangePickerSteps {
    struct Range {
        let from: String
        let to: String
    }

    @discardableResult
    func isScreenPresented(with title: String) -> Self {
        XCTContext.runActivity(named: "Проверям, что пикер \"\(title)\" показан") { _ -> Void in
            self.rootView.yreEnsureExistsWithTimeout()
                .yreEnsureVisible()

            // @l-saveliy: Toolbar marked as not accessible element by default. So we can't obtain it.
            // Check title label directly in picker view
            let title = ElementsProvider.obtainElement(identifier: title, in: self.rootView)
            title.yreEnsureExists()
        }

        return self
    }

    @discardableResult
    func isScreenClosed() -> Self {
        XCTContext.runActivity(named: "Проверям, что пикер закрыт") { _ -> Void in
            self.rootView.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func clear() -> Self {
        XCTContext.runActivity(named: "Очищаем поле \"от\"") { _ -> Void in
            let fromTextField = ElementsProvider.obtainElement(identifier: Identifiers.fromTextField, type: .textField, in: self.rootView)
            fromTextField.yreClearText()
        }

        XCTContext.runActivity(named: "Очищаем поле \"до\"") { _ -> Void in
            let toTextField = ElementsProvider.obtainElement(identifier: Identifiers.toTextField, type: .textField, in: self.rootView)
            toTextField.yreClearText()
        }
        return self
    }

    @discardableResult
    func enter(_ range: Range) -> Self {
        XCTContext.runActivity(named: "В поле \"от\" вводим \(range.from)") { _ -> Void in
            let fromTextField = ElementsProvider.obtainElement(identifier: Identifiers.fromTextField, type: .textField, in: self.rootView)
            fromTextField
                .yreEnsureExists()
                .yreTap()
                .yreTypeText(range.from)
        }

        XCTContext.runActivity(named: "В поле \"до\" вводим \(range.to)") { _ -> Void in
            let toTextField = ElementsProvider.obtainElement(identifier: Identifiers.toTextField, type: .textField, in: self.rootView)
            toTextField
                .yreEnsureExists()
                .yreTap()
                .yreTypeText(range.to)
        }

        return self
    }

    @discardableResult
    func close() -> Self {
        let closeButton = ElementsProvider.obtainElement(identifier: Identifiers.closeButton, in: self.rootView)
        closeButton
            .yreEnsureExists()
            .yreTap()

        return self
    }

    @discardableResult
    func apply() -> Self {
        XCTContext.runActivity(named: "Нажимае на кнопку \"Готово\"") { _ -> Void in
            let applyButton = ElementsProvider.obtainButton(identifier: Identifiers.applyButton, in: self.rootView)
            applyButton
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    // MARK: Private

    private enum Identifiers {
        static let numberRangePicker = FiltersNumberRangePickerAccessibilityIdentifiers.moduleIdentifier
        static let fromTextField = FiltersNumberRangePickerAccessibilityIdentifiers.fromTextFieldIdentifier
        static let toTextField = FiltersNumberRangePickerAccessibilityIdentifiers.toTextFieldIdentifier
        static let applyButton = FiltersNumberRangePickerAccessibilityIdentifiers.applyButtonIdentifier
        static let closeButton = FiltersNumberRangePickerAccessibilityIdentifiers.closeButtonIdentifier
    }

    private let rootView = ElementsProvider.obtainElement(identifier: Identifiers.numberRangePicker)
}
