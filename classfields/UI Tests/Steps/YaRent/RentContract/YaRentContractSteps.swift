//
//  YaRentContractSteps.swift
//  UI Tests
//
//  Created by Evgenii Novikov on 08.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.YaRentContractAccessibilityIdentifiers

final class YaRentContractSteps {
    @discardableResult
    func ensurePresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие договора на экране") { _ -> Void in
            self.contentView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnFAQItem() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку FAQ") { _ -> Void in
            self.contentView.scrollToElement(element: self.faqItem, direction: .up)
            self.faqItem
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnHeaderItem() -> Self {
        XCTContext.runActivity(named: "Нажимаем на информацию с полным договором") { _ -> Void in
            self.headerItem
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnSignButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку Подписать") { _ -> Void in
            self.signButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnCommentsItem() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку комментарии менеджера") { _ -> Void in
            self.contentView.scrollToElement(element: self.faqItem, direction: .up)
            self.commentItem
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func ensureCommentItemNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутсвие кнопки Комментарии менеджера") { _ -> Void in
            self.commentItem.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func ensureTermsErrorNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутсвие ошибки под пользовательским соглашением") { _ -> Void in
            self.commentItem.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func ensureTermsErrorPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие ошибки под пользовательским соглашением") { _ -> Void in
            self.commentItem.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnTermsToggle() -> Self {
        XCTContext.runActivity(named: "Нажимаем на свитч подписания соглашений") { _ -> Void in
            self.contentView.scrollToElement(element: self.termsToggle, direction: .up)
            self.termsToggle
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = YaRentContractAccessibilityIdentifiers

    private lazy var contentView = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var faqItem = ElementsProvider.obtainElement(identifier: Identifiers.faqItem)
    private lazy var commentItem = ElementsProvider.obtainElement(identifier: Identifiers.commentItem)
    private lazy var headerItem = ElementsProvider.obtainElement(identifier: Identifiers.headerItem)
    private lazy var signButton = ElementsProvider.obtainElement(identifier: Identifiers.signButton)
    private lazy var termsError = ElementsProvider.obtainElement(identifier: Identifiers.termsError)
    private lazy var termsToggle = ElementsProvider.obtainElement(identifier: Identifiers.termsToggle)
}
