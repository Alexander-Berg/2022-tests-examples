//
//  YaRentTenantShowingsSteps.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 06.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import enum YREAccessibilityIdentifiers.YaRentTenantShowingsAccessibilityIdentifiers

final class YaRentTenantShowingsSteps {
    @discardableResult
    func isContentPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие контента на экране \"Мои предложения\"") { _ -> Void in
            self.view
                .yreEnsureExistsWithTimeout()
                .yreEnsureVisibleWithTimeout()

            self.emptyView.yreEnsureNotVisible()
            self.errorView.yreEnsureNotVisible()
        }
        return self
    }

    @discardableResult
    func isEmptyPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие заглушки на экране \"Мои предложения\"") { _ -> Void in
            self.emptyView
                .yreEnsureExistsWithTimeout()
                .yreEnsureVisibleWithTimeout()

            self.view.yreEnsureNotVisible()
            self.errorView.yreEnsureNotVisible()
        }
        return self
    }

    @discardableResult
    func isErrorPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие ошибки на экране \"Мои предложения\"") { _ -> Void in
            self.errorView
                .yreEnsureExistsWithTimeout()
                .yreEnsureVisibleWithTimeout()

            self.view.yreEnsureNotVisible()
            self.emptyView.yreEnsureNotVisible()
        }
        return self
    }

    @discardableResult
    func isAppUpdateNotificationPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что нотификация \"Обновись\" отображается в хедере") { _ -> Void in
            let element = ElementsProvider.obtainElement(identifier: Identifiers.Notification.appUpdate)
            element
                .yreEnsureExistsWithTimeout()
                .yreEnsureVisible()
        }
        return self
    }

    @discardableResult
    func isFooterHidden() -> Self {
        XCTContext.runActivity(named: "Проверяем, что кнопка просмотра других предложений не отображается") { _ -> Void in
            self.footer
                .yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func tapOnFooter() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку просмотра других предложений") { _ -> Void in
            self.footer
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func showing(at index: Int) -> ShowingSteps {
        XCTContext.runActivity(named: "Проверяем, что показ отображается (индекс: \(index))") { _ in
            let element = self.view
                .descendants(matching: .any)
                .matching(identifier: Identifiers.Showing.view)
                .element(boundBy: index)
                .yreEnsureExistsWithTimeout()

            return ShowingSteps(with: element)
        }
    }

    private typealias Identifiers = YaRentTenantShowingsAccessibilityIdentifiers

    private lazy var view = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var emptyView = ElementsProvider.obtainElement(identifier: Identifiers.emptyView)
    private lazy var errorView = ElementsProvider.obtainElement(identifier: Identifiers.errorView)

    private lazy var footer = ElementsProvider.obtainElement(identifier: Identifiers.footer)
}

extension YaRentTenantShowingsSteps {
    final class ShowingSteps {
        fileprivate init(with element: XCUIElement) {
            self.view = element
        }

        @discardableResult
        func tapOnSnippet() -> Self {
            XCTContext.runActivity(named: "Нажимаем на сниппет объявления") { _ -> Void in
                self.snippet
                    .yreEnsureExists()
                    .yreTap()
            }
            return self
        }

        @discardableResult
        func isRoommatesPresented() -> Self {
            XCTContext.runActivity(named: "Проверяем, что список сожителей отображается") { _ -> Void in
                self.roommates
                    .yreEnsureExists()
            }
            return self
        }

        @discardableResult
        func isRoommatesNotPresented() -> Self {
            XCTContext.runActivity(named: "Проверяем, что список сожителей не отображается") { _ -> Void in
                self.roommates
                    .yreEnsureNotExists()
            }
            return self
        }

        @discardableResult
        func tapOnRoommates() -> Self {
            XCTContext.runActivity(named: "Нажимаем на список сожителей") { _ -> Void in
                self.roommates
                    .yreTap()
            }
            return self
        }

        @discardableResult
        func isNotificationPresented() -> Self {
            XCTContext.runActivity(named: "Проверяем, что нотификация отображается") { _ -> Void in
                self.notification
                    .yreEnsureExists()
            }
            return self
        }

        @discardableResult
        func isNotificationPresented(with buttonText: String) -> Self {
            XCTContext.runActivity(named: "Проверяем, что нотификация с кнопкой \"\(buttonText)\" отображается") { _ -> Void in
                self.notification
                    .yreEnsureExists()

                let button = self.notificationAction
                    .yreEnsureExists()

                XCTAssertEqual(button.label, buttonText)
            }
            return self
        }

        @discardableResult
        func isNotificationNotPresented() -> Self {
            XCTContext.runActivity(named: "Проверяем, что нотификация не отображается") { _ -> Void in
                self.notification
                    .yreEnsureNotExists()
            }
            return self
        }

        @discardableResult
        func tapOnNotificationAction() -> Self {
            XCTContext.runActivity(named: "Нажимаем на СТА нотификации") { _ -> Void in
                self.notificationAction
                    .yreEnsureExists()
                    .yreTap()
            }
            return self
        }

        private typealias Identifiers = YaRentTenantShowingsAccessibilityIdentifiers.Showing

        private let view: XCUIElement

        private lazy var snippet = ElementsProvider.obtainElement(
            identifier: Identifiers.snippet,
            type: .any,
            in: self.view
        )
        private lazy var roommates = ElementsProvider.obtainElement(
            identifier: Identifiers.roommates,
            type: .any,
            in: self.view
        )
        private lazy var notification = ElementsProvider.obtainElement(
            identifier: Identifiers.Notification.view,
            type: .other,
            in: self.view
        )
        private lazy var notificationAction = ElementsProvider.obtainElement(
            identifier: Identifiers.Notification.action,
            type: .button,
            in: self.notification
        )
    }
}

final class YaRentShowingsDatePickerSteps {
    @discardableResult
    func isPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие пикера даты на экране") { _ -> Void in
            self.view.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что пикер даты не отображается на экране") { _ -> Void in
            self.view.yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func sendSelectedDate() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Выбрать дату\"") { _ -> Void in
            self.actionButton
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = YaRentTenantShowingsAccessibilityIdentifiers.DatePicker

    private lazy var view = ElementsProvider.obtainElement(identifier: Identifiers.view)

    private lazy var actionButton = ElementsProvider.obtainElement(
        identifier: Identifiers.action,
        type: .button,
        in: self.view
    )
}
