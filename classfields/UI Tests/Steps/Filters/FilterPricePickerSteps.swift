//
//  FilterPricePickerSteps.swift
//  UI Tests
//
//  Created by Alexey Salangin on 7/3/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREAccessibilityIdentifiers

final class FilterPricePickerSteps {
    struct Price {
        let from: String
        let to: String
    }

    enum PriceType {
        case perOffer
        case perM2
        case perAre
    }

    enum PricePeriod {
        case perMonth
        case perYear
    }

    @discardableResult
    func isPricePickerPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что пикер \"Цена\" показан") { _ -> Void in
            let picker = self.pricePickerVC
            picker.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func change(period: PricePeriod) -> Self {
        let button = ElementsProvider.obtainButton(identifier: Identifiers.pricePeriodButton(for: period), in: self.pricePickerVC)
        button
            .yreEnsureExists()
            .tap()
        return self
    }

    @discardableResult
    func change(type: PriceType) -> Self {
        let button = ElementsProvider.obtainButton(identifier: Identifiers.priceTypeButton(for: type), in: self.pricePickerVC)
        button
            .yreEnsureExists()
            .tap()
        return self
    }

    @discardableResult
    func clear() -> Self {
        XCTContext.runActivity(named: "Очищаем поле \"от\"") { _ -> Void in
            let fromTextField = ElementsProvider.obtainElement(
                identifier: Identifiers.fromTextField,
                type: .textField,
                in: self.pricePickerVC
            )
            fromTextField.yreClearText()
        }

        XCTContext.runActivity(named: "Очищаем поле \"до\"") { _ -> Void in
            let toTextField = ElementsProvider.obtainElement(identifier: Identifiers.toTextField, type: .textField, in: self.pricePickerVC)
            toTextField.yreClearText()
        }
        return self
    }

    @discardableResult
    func enter(price: Price) -> Self {
        XCTContext.runActivity(named: "В поле \"от\" вводим \(price.from)") { _ -> Void in
            let fromTextField = ElementsProvider.obtainElement(
                identifier: Identifiers.fromTextField,
                type: .textField,
                in: self.pricePickerVC
            )
            fromTextField
                .yreEnsureExists()
                .yreTap()
                .yreClearText()
                .yreTypeText(price.from)
        }

        XCTContext.runActivity(named: "В поле \"до\" вводим \(price.to)") { _ -> Void in
            let toTextField = ElementsProvider.obtainElement(identifier: Identifiers.toTextField, type: .textField, in: self.pricePickerVC)
            toTextField
                .yreEnsureExists()
                .yreTap()
                .yreClearText()
                .yreTypeText(price.to)
        }
        return self
    }

    @discardableResult
    func apply() -> Self {
        XCTContext.runActivity(named: "Нажимае на кнопку \"Готово\"") { _ -> Void in
            let applyButton = ElementsProvider.obtainButton(identifier: Identifiers.applyButton, in: self.pricePickerVC)
            applyButton
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    // MARK: Private
    private enum Identifiers {
        static let pricePicker = FiltersPricePickerAccessibilityIdentifiers.moduleIdentifier
        static let fromTextField = FiltersPricePickerAccessibilityIdentifiers.fromTextFieldIdentifier
        static let toTextField = FiltersPricePickerAccessibilityIdentifiers.toTextFieldIdentifier
        static let applyButton = FiltersPricePickerAccessibilityIdentifiers.applyButtonIdentifier

        static func pricePeriodButton(for pricePeriod: PricePeriod) -> String {
            return "YRESegmentedControl-\(pricePeriod.placeholderName)"
        }

        static func priceTypeButton(for priceType: PriceType) -> String {
            return "YRESegmentedControl-\(priceType.placeholderName)"
        }
    }

    private let pricePickerVC = ElementsProvider.obtainElement(identifier: Identifiers.pricePicker)
}

extension FilterPricePickerSteps.PricePeriod {
    var placeholderName: String {
        switch self {
            case .perMonth:
                return "За месяц"
            case .perYear:
                return "За год"
        }
    }
}

extension FilterPricePickerSteps.PriceType {
    var placeholderName: String {
        switch self {
            case .perOffer:
                return "За всё"
            case .perM2:
                return "За м²"
            case .perAre:
                return "За сотку"
        }
    }
}
