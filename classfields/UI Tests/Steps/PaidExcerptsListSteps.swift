//
//  PaidExcerptsListSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 21.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import enum YREAccessibilityIdentifiers.PaidExcerptsListAccessibilityIdentifiers
import XCTest
import YRETestsUtils

final class PaidExcerptsListSteps {
    lazy var viewController = ElementsProvider.obtainElement(identifier: Identifiers.viewController)
    lazy var authView = ElementsProvider.obtainElement(
        identifier: Identifiers.authView,
        in: self.viewController
    )
    lazy var loginButton = ElementsProvider.obtainElement(
        identifier: Identifiers.loginButton,
        in: self.authView
    )
    lazy var emptyView = ElementsProvider.obtainElement(
        identifier: Identifiers.emptyView,
        in: self.viewController
    )
    lazy var showOffersButton = ElementsProvider.obtainElement(
        identifier: Identifiers.showOffersButton,
        in: self.emptyView
    )
    lazy var tableView = ElementsProvider.obtainElement(
        identifier: Identifiers.tableView,
        in: self.viewController
    )

    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана списка отчетов") { _ -> Void in
            self.viewController.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isAuthViewPresented() -> Self {
        XCTContext.runActivity(
            named: "Проверяем наличие элемента 'Неавторизованный пользователь' на экране списка отчетов"
        ) { _ -> Void in
            self.authView.yreEnsureExistsWithTimeout()
            self.loginButton.yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func isEmptyViewPresented() -> Self {
        XCTContext.runActivity(
            named: "Проверяем наличие элемента 'Пустой список' на экране списка отчетов"
        ) { _ -> Void in
            self.emptyView.yreEnsureExistsWithTimeout()
            self.showOffersButton.yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func isListPresented() -> Self {
        XCTContext.runActivity(
            named: "Проверяем наличие элемента 'Список' на экране списка отчетов"
        ) { _ -> Void in
            self.tableView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapShowOffersButton() -> Self {
        XCTContext.runActivity(
            named: "Тапаем 'Найти' на экране списка отчетов"
        ) { _ -> Void in
            self.showOffersButton
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapBackButton() -> Self {
        XCTContext.runActivity(
            named: "Тапаем 'Назад' на экране списка отчетов"
        ) { _ -> Void in
            ElementsProvider.obtainBackButton()
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapShowOfferButtonOnSnippet(row: Int) -> Self {
        XCTContext.runActivity(
            named: "Нажимаем 'Открыть объявление' сниппета \(row) на экране списка отчетов"
        ) { _ -> Void in
            self.tapButtonOnSnippet(row: row, buttonIdentifier: Identifiers.PaidExcerptSnippet.showOfferButton)
        }

        return self
    }

    @discardableResult
    func tapShowExcerptButtonOnSnippet(row: Int) -> Self {
        XCTContext.runActivity(
            named: "Нажимаем 'Открыть отчет' сниппета \(row) на экране списка отчетов"
        ) { _ -> Void in
            self.tapButtonOnSnippet(row: row, buttonIdentifier: Identifiers.PaidExcerptSnippet.showExcerptButton)
        }

        return self
    }

    @discardableResult
    func tapPayAgainButtonOnSnippet(row: Int) -> Self {
        XCTContext.runActivity(
            named: "Нажимаем 'оплатить ещё раз' сниппета \(row) на экране списка отчетов"
        ) { _ -> Void in
            self.tapButtonOnSnippet(row: row, buttonIdentifier: Identifiers.PaidExcerptSnippet.payAgainButton)
        }

        return self
    }

    @discardableResult
    func scrollToSnippet(row: Int) -> Self {
        XCTContext.runActivity(
            named: "Скроллим к сниппету \(row) на экране списка отчетов"
        ) { _ -> Void in
            let snippet = ElementsProvider
                .obtainElement(identifier: Identifiers.snippetCellIdentifier(row: row), in: self.tableView)

            self.tableView
                .yreEnsureExistsWithTimeout()
                .scrollToElement(element: snippet, direction: .up, swipeLimits: 10)

            snippet
                .yreEnsureExistsWithTimeout()
                .yreEnsureVisible()
        }

        return self
    }

    private typealias Identifiers = PaidExcerptsListAccessibilityIdentifiers

    private func tapButtonOnSnippet(row: Int, buttonIdentifier: String) {
        let snippet = ElementsProvider
            .obtainElement(identifier: Identifiers.snippetCellIdentifier(row: row), in: self.tableView)
            .yreEnsureExistsWithTimeout()
        ElementsProvider
            .obtainButton(identifier: buttonIdentifier, in: snippet)
            .yreEnsureExists()
            .yreEnsureHittable()
            .yreTap()
    }
}
