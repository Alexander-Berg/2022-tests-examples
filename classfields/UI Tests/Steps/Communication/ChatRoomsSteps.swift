//
//  ChatRoomsSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 18.05.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.ChatRoomsAccessibilityIdentifiers

final class ChatRoomsSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана Список чатов") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isUnauthorizedViewPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие вьюшки незарегистрированного пользователя") { _ -> Void in
            self.unauthorizedView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isErrorViewPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие вьюшки ошибки") { _ -> Void in
            self.errorView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isContentViewPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие списка чатов") { _ -> Void in
            self.contentView
                .yreEnsureExistsWithTimeout()
                // Chat rooms content view always exists. Make sure that it's really visible
                .yreEnsureVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func isLoginButtonHittable() -> Self {
        XCTContext.runActivity(named: "Проверяем нажимаемость кнопки Войти") { _ -> Void in
            self.loginButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
        }
        return self
    }

    // MARK: - Actions

    @discardableResult
    func tapRetryButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку Повторить") { _ -> Void in
            self.retryButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnCell(row: Int) -> Self {
        XCTContext.runActivity(named: "Нажимаем на чат \(row)") { _ -> Void in
            self.obtainCell(with: Identifiers.chatRoomCellID(row: row))
                .yreTap()
        }
        return self
    }

    // MARK: Private

    typealias Identifiers = ChatRoomsAccessibilityIdentifiers

    private lazy var screenView = ElementsProvider.obtainElement(identifier: Identifiers.screenView)
    private lazy var unauthorizedView = ElementsProvider.obtainElement(
        identifier: Identifiers.unauthorizedView,
        in: self.screenView
    )
    private lazy var errorView = ElementsProvider.obtainElement(
        identifier: Identifiers.errorView,
        in: self.screenView
    )
    private lazy var loginButton = ElementsProvider.obtainElement(
        identifier: Identifiers.loginButton,
        in: self.unauthorizedView
    )
    private lazy var retryButton = ElementsProvider.obtainElement(
        identifier: Identifiers.retryButton,
        in: self.errorView
    )
    private lazy var contentView = ElementsProvider.obtainElement(
        identifier: Identifiers.contentView,
        in: self.screenView
    )
    private lazy var tabBar = ElementsProvider.obtainTabBar()

    private func obtainCell(with identifier: String) -> XCUIElement {
        XCTContext.runActivity(named: "Скролим к ячейке с id \(identifier)") { _ -> XCUIElement in
            let cell = ElementsProvider
                .obtainElement(identifier: identifier, in: self.contentView)
                .yreEnsureExistsWithTimeout()
            let tabBar = self.tabBar.yreEnsureExists()
            self.contentView.scroll(
                to: cell,
                adjustInteractionFrame: { $0.yreSubtract(tabBar.frame, from: .maxYEdge) }
            )
            return cell
        }
    }
}
