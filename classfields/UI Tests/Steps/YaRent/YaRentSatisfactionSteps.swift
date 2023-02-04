//
//  YaRentSatisfactionSteps.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 07.07.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREAccessibilityIdentifiers

final class YaRentSatisfactionSteps {
    enum PickerItem {
        case score(Int)
        case unselected
    }

    @discardableResult
    func isPromoPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что промо `Оцените нас` отображается") { _ -> Void in
            self.promo
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isPromoNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что промо `Оцените нас` не отображается") { _ -> Void in
            self.promo
                .yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func tapOnPromoAction() -> Self {
        XCTContext.runActivity(named: "Нажимаем `Оценить` в промо `Оцените нас`") { _ -> Void in
            self.promoAction
                .yreEnsureExists()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func isViewPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что экран с выбором оценки отображается") { _ -> Void in
            self.view
                .yreEnsureExistsWithTimeout()
                .yreEnsureVisible()
        }
        return self
    }

    @discardableResult
    func isViewNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что экран с выбором оценки не отображается") { _ -> Void in
            self.view
                .yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func closeScreen() -> Self {
        XCTContext.runActivity(named: "Закрываем экран с выбором оценки") { _ -> Void in
            ElementsProvider.obtainBackButton()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func scroll() -> Self {
        XCTContext.runActivity(named: "Скроллим пикер") { _ -> Void in

            self.picker
                .yreEnsureNotExists()
                .yreSwipeLeft()
        }
        return self
    }

    @discardableResult
    func tapOnItem(_ item: PickerItem) -> Self {
        XCTContext.runActivity(named: "Тапаем на элемент пикера `\(item.description)`") { _ -> Void in
            let itemElement = ElementsProvider.obtainElement(
                identifier: item.accessibilityIdentifier,
                type: .cell,
                in: self.picker
            )
            itemElement
                .yreEnsureVisibleWithTimeout()
                .yreForceTap()
        }
        return self
    }

    @discardableResult
    func fillMessage(with text: String) -> Self {
        XCTContext.runActivity(named: "Вводим сообщение с текстом \"\(text)\"") { _ -> Void in
            self.textField
                .yreEnsureVisible()
                .yreTap()
                .yreTypeText(text)
        }
        return self
    }

    @discardableResult
    func tapOnSendButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку `Отправить`") { _ -> Void in
            self.actionButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }


    // MARK: - Private

    private typealias Identifiers = YaRentSatisfactionRatingAccessibilityIdentifiers

    private lazy var view = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var picker = ElementsProvider.obtainElement(identifier: Identifiers.picker, in: self.view)
    private lazy var textField = ElementsProvider.obtainElement(identifier: Identifiers.textField, in: self.view)
    private lazy var actionButton = ElementsProvider.obtainElement(identifier: Identifiers.actionButton, in: self.view)

    private lazy var promo = ElementsProvider.obtainElement(identifier: Identifiers.Promo.view)
    private lazy var promoAction = ElementsProvider.obtainElement(identifier: Identifiers.Promo.action, in: self.promo)
}

extension YaRentSatisfactionSteps.PickerItem {
    fileprivate var description: String {
        switch self {
            case .unselected:
                return "Оценка не выбрана"

            case .score(let value):
                return "Оценка - \(value)"
        }
    }

    fileprivate var accessibilityIdentifier: String {
        switch self {
            case .unselected:
                return YaRentSatisfactionRatingAccessibilityIdentifiers.pickerUnselectedItem

            case .score(let value):
                return YaRentSatisfactionRatingAccessibilityIdentifiers.pickerVoteItem(value: value)
        }
    }
}
