//
//  YaRentFlatTodoNotificationSteps.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 26.11.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import enum YREAccessibilityIdentifiers.YaRentFlatCardAccessibilityIdentifiers

final class YaRentFlatTodoNotificationSteps {
    enum TodoType: String {
        case ownerConfirmedTodo = "Подготовка документов"
        case ownerPaymentInfoTodo = "Данные для получения оплаты"
    }

    @discardableResult
    func isPresented(type: TodoType) -> YaRentFlatTypedTodoNotificationSteps {
        XCTContext.runActivity(named: "Проверяем наличие туду-нотификации \"\(type.rawValue)\"") { _ -> Void in
            type.obtainElement()
                .yreEnsureExistsWithTimeout()
        }
        return YaRentFlatTypedTodoNotificationSteps(type: type)
    }

    @discardableResult
    func isNotPresented(type: TodoType) -> Self {
        XCTContext.runActivity(named: "Проверяем отсутсвие туду-нотификации \"\(type.rawValue)\"") { _ -> Void in
            type.obtainElement()
                .yreEnsureExistsWithTimeout()
        }
        return self
    }
}

final class YaRentFlatTypedTodoNotificationSteps {
    init(type: YaRentFlatTodoNotificationSteps.TodoType) {
        self.type = type
    }

    @discardableResult
    func isErrorDescriptionExists(inItemAt index: Int) -> Self {
        XCTContext.runActivity(named: "Проверяем наличие сообщения об ошибке для \(index + 1)-го пункта туду-нотификации \"\(type.rawValue)\"") { _ -> Void in
            let itemElement = type.obtainItemElement(atIndex: index)
            itemElement.yreEnsureExists()

            let errorElement = ElementsProvider.obtainElement(
                identifier: Identifiers.Element.errorDescription,
                type: .any,
                in: itemElement
            )

            errorElement
                .yreEnsureExists()
                .yreEnsureVisible()
        }
        return self
    }

    @discardableResult
    func isErrorDescriptionNotExists(inItemAt index: Int) -> Self {
        XCTContext.runActivity(named: "Проверяем, что сообщения об ошибке нет для \(index + 1)-го пункта туду-нотификации \"\(self.type.rawValue)\"") { _ -> Void in
            let itemElement = self.type.obtainItemElement(atIndex: index)
            itemElement.yreEnsureExists()

            let errorElement = ElementsProvider.obtainElement(
                identifier: Identifiers.Element.errorDescription,
                type: .any,
                in: itemElement
            )

            errorElement
                .yreEnsureNotVisible()
        }
        return self
    }

    @discardableResult
    func isTodoItemDone(inItemAt index: Int) -> Self {
        XCTContext.runActivity(named: "Проверяем, что пункт \(index + 1) выполнен в туду-нотификации \"\(self.type.rawValue)\"") { _ -> Void in
            let itemElement = self.type.obtainItemElement(atIndex: index)
            itemElement.yreEnsureExists()

            let buttonElement = ElementsProvider.obtainElement(
                identifier: Identifiers.Element.actionButton,
                type: .any,
                in: itemElement
            )

            buttonElement
                .yreEnsureExists()
                .yreEnsureNotEnabled()
        }
        return self
    }

    @discardableResult
    func isTodoItemWaitingForAction(inItemAt index: Int) -> Self {
        XCTContext.runActivity(named: "Проверяем, что пункт \(index + 1) ещё не выполнен в туду-нотификации \"\(self.type.rawValue)\"") { _ -> Void in
            let itemElement = self.type.obtainItemElement(atIndex: index)
            itemElement.yreEnsureExists()

            let buttonElement = ElementsProvider.obtainElement(
                identifier: Identifiers.Element.actionButton,
                type: .any,
                in: itemElement
            )

            buttonElement
                .yreEnsureExists()
                .yreEnsureHittable()
                .yreEnsureEnabled()
        }
        return self
    }

    @discardableResult
    func tapOnActionButton(inItemAt index: Int) -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \(index + 1)-го пункта туду-нотификации \"\(self.type.rawValue)\"") { _ -> Void in
            let itemElement = self.type.obtainItemElement(atIndex: index)
            itemElement.yreEnsureExists()

            let buttonElement = ElementsProvider.obtainElement(
                identifier: Identifiers.Element.actionButton,
                type: .any,
                in: itemElement
            )

            buttonElement
                .yreEnsureExists()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = YaRentFlatCardAccessibilityIdentifiers.Notification.Todo

    private let type: YaRentFlatTodoNotificationSteps.TodoType
}

extension YaRentFlatTodoNotificationSteps.TodoType {
    fileprivate func obtainElement() -> XCUIElement {
        ElementsProvider.obtainElement(identifier: self.identifier)
    }

    fileprivate func obtainItemElement(atIndex index: Int) -> XCUIElement {
        self.obtainElement()
            .descendants(matching: .any)
            .matching(identifier: Identifiers.Element.item)
            .element(boundBy: index)
    }

    private var identifier: String {
        let identifiers = YaRentFlatCardAccessibilityIdentifiers.Notification.Todo.self
        switch self {
            case .ownerPaymentInfoTodo:
                return identifiers.ownerPaymentInfoTodo
            case .ownerConfirmedTodo:
                return identifiers.ownerConfirmedTodo
        }
    }

    private typealias Identifiers = YaRentFlatCardAccessibilityIdentifiers.Notification.Todo
}
