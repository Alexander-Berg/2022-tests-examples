//
//  UserOffersListSteps.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 22.02.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils

final class UserOffersListSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана ЛК") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isListPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие элемента 'Список' на экране ЛК") { _ -> Void in
            self.listView.yreEnsureExistsWithTimeout(message: "List view should exist")
        }
        return self
    }

    @discardableResult
    func isListNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие элемента 'Список' на экране ЛК") { _ -> Void in
            self.listView.yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func isAuthViewPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие элемента 'Неавторизованный пользователь' на экране ЛК") { _ -> Void in
            self.emptyListView.yreEnsureExistsWithTimeout()
            self.loginButton.yreEnsureHittable()
        }
        return self
    }

    @discardableResult
    func isAuthViewNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие элемента 'Неавторизованный пользователь' на экране ЛК") { _ -> Void in
            self.loginButton.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isNothingFoundViewPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие элемента 'Ничего не найдено' на экране ЛК") { _ -> Void in
            self.emptyListView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isNothingFoundViewNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие элемента 'Ничего не найдено' на экране ЛК") { _ -> Void in
            self.emptyListView.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isAddOfferNavbarButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие кнопки 'Добавить' в навигационной панели на экране ЛК") { _ -> Void in
            self.addOfferNavbarButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabledWithTimeout()
                .yreEnsureHittableWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapAddOfferNavbarButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Добавить' в навигационной панели на экране ЛК") { _ -> Void in
            self.addOfferNavbarButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabledWithTimeout()
                .yreEnsureHittableWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func isAddOfferNavbarButtonNotExists() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие кнопки 'Добавить' в навигационной панели на экране ЛК") { _ -> Void in
            self.addOfferNavbarButton.yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func isFiltersButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие кнопки 'Параметры' на экране ЛК") { _ -> Void in
            self.filtersButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabledWithTimeout()
                .yreEnsureHittableWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapFiltersButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Параметры' на экране ЛК") { _ -> Void in
            self.filtersButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabledWithTimeout()
                .yreEnsureHittableWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func isFiltersButtonNotExists() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие кнопки 'Параметры' на экране ЛК") { _ -> Void in
            self.filtersButton.yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func isBannedUserViewPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие элемента 'Забаненный пользователь' на экране ЛК") { _ -> Void in
            self.bannedUserView.yreEnsureExistsWithTimeout(message: "BannedUserView should exist")
        }
        return self
    }

    @discardableResult
    func isBannedUserHelpButtonTappable() -> Self {
        XCTContext.runActivity(named: "Проверяем доступность кнопки 'Техподдержка' на экране ЛК") { _ -> Void in
            self.bannedUserHelpButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabledWithTimeout()
                .yreEnsureHittableWithTimeout()
        }
        return self
    }

    @discardableResult
    func isSuccessNotificationViewPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие уведомления 'Успешная операция' на экране ЛК") { _ -> Void in
            self.notificationView.yreEnsureVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func isSuccessNotificationViewDismissedAfterDelay(_ delay: TimeInterval = Constants.timeout) -> Self {
        XCTContext.runActivity(
            named: "Проверяем скрытие уведомления 'Успешная операция' на экране ЛК после таймаута \(delay) с"
        ) { _ -> Void in
            self.notificationView.yreEnsureNotVisibleWithTimeout(timeout: delay)
        }
        return self
    }

    @discardableResult
    func isListNonEmpty() -> Self {
        XCTContext.runActivity(named: "Проверяем, что список на экране ЛК имеет хотя бы один элемент") { _ -> Void in
            self.cellsQuery
                .firstMatch
                // Standard interval is not sufficient to check the existence of the list
                .yreEnsureExistsWithTimeout(timeout: Const.longTimeout, message: "List shouldn't be empty")
        }
        return self
    }

    @discardableResult
    func compareWithScreenshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом списка в ЛК") { _ -> Void in
            let screenshot = self.screenView.yreWaitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
        }
        return self
    }

    func cell(withIndex index: Int) -> UserOfferSnippetSteps {
        return XCTContext.runActivity(named: "Получаем элемент списка под индексом \(index)") { _ -> UserOfferSnippetSteps in
            let item = self.cellsQuery.element(boundBy: index)
            item.yreEnsureExistsWithTimeout()
            return UserOfferSnippetSteps(element: item, scrollView: self.listView)
        }
    }

    func chatsPromo() -> UserOfferChatsPromoSteps {
        return UserOfferChatsPromoSteps(element: self.promoBannerView)
    }

    func errorAlert() -> AnyAlertSteps {
        return AnyAlertSteps(elementType: .alert,
                             alertID: Identifiers.errorAlert)
    }

    func overQuotaAlert() -> UserOfferOverQuotaAlertSteps {
        return UserOfferOverQuotaAlertSteps()
    }

    @discardableResult
    func pullToRefresh() -> Self {
        XCTContext.runActivity(named: "Обновляем список офферов") { _ -> Void in
            self.listView.yrePullToRefresh()
        }
        return self
    }

    // MARK: Private

    private struct Identifiers {
        static let screenView = "userOffers.list.view"
        static let listView = "userOffers.list.tableView"
        static let addOfferNavbarButton = "userOffers.list.navbar.addOfferButton"
        static let filtersButton = "userOffers.list.filtersButton"
        static let bannedUserView = "userOffers.bannedUser.view"
        static let bannedUserHelpButton = "userOffers.bannedUser.helpButton"
        static let emptyListView = "userOffers.emptyList.view"
        static let addOfferButton = "userOffers.emptyList.addOfferButton"
        static let loginButton = "userOffers.emptyList.loginButton"
        static let snippetCell = "userOffers.list.snippetCell"

        static let notificationView = "userOffers.list.notificationView"
        static let promoBannerView = "PromoBanner.view"
        static let errorAlert = "userOffers.list.errorAlert"
    }

    private struct Const {
        static let longTimeout: TimeInterval = 10
    }

    private lazy var screenView: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.screenView)
    private lazy var listView: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.listView)
    private lazy var addOfferNavbarButton: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.addOfferNavbarButton)
    private lazy var filtersButton: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.filtersButton)

    private lazy var bannedUserView: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.bannedUserView)
    private lazy var bannedUserHelpButton: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.bannedUserHelpButton)

    private lazy var emptyListView: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.emptyListView)
    private lazy var addOfferButton: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.addOfferButton)
    private lazy var loginButton: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.loginButton)

    private lazy var notificationView: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.notificationView)
    private lazy var promoBannerView: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.promoBannerView)

    private lazy var cellsQuery: XCUIElementQuery = self.listView.cells.matching(identifier: Identifiers.snippetCell)
}
