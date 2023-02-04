//
//  SavedSearchesListSteps.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 5/6/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import enum YREAccessibilityIdentifiers.SavedSearchesListAccessibilityIdentifiers

final class SavedSearchesListSteps {
    @discardableResult
    func screenIsPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана Сохраненные поиски") { _ -> Void in
            self.viewController.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func widgetPromoBannerIsShown() -> Self {
        XCTContext.runActivity(named: "Проверяем, что промо баннер показывается") { _ -> Void in
            self.widgetPromoBanner.yreEnsureVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func widgetPromoBannerIsHidden() -> Self {
        XCTContext.runActivity(named: "Проверяем, что промо баннер скрыт") { _ -> Void in
            self.widgetPromoBanner.yreEnsureNotVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapWidgetPromoBannerActionButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку Понятно на баннере") { _ -> Void in
            self.widgetPromoBannerActionButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapWidgetPromoBannerCloseButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку Закрыть на баннере") { _ -> Void in
            self.widgetPromoBannerCloseButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func performPullToRefresh() -> Self {
        XCTContext.runActivity(named: "Выполняем Pull-to-refresh на экране Сохраненные поиски") { _ -> Void in
            self.viewController
                .yreEnsureExistsWithTimeout()
                .yrePullToRefresh()
        }
        return self
    }

    @discardableResult
    func pullToRefreshBecomeHidden(timeout: TimeInterval = Constants.timeout) -> Self {
        XCTContext.runActivity(named: "Проверяем, что индикация загрузки на экране \"Сохранённые поиски\" скрыта") { _ -> Void in
            self.refreshControl.yreEnsureNotExistsWithTimeout(timeout: timeout)
        }
        return self
    }

    @discardableResult
    func swipeLeftFirstRow() -> Self {
        XCTContext.runActivity(named: "Выполняем свайп влево для первой ячейки в списке") { _ -> Void in
            self.firstRow
                .yreEnsureExistsWithTimeout()
                .swipeLeft()
        }
        return self
    }

    @discardableResult
    func tapOnFirstRow() -> Self {
        XCTContext.runActivity(named: "Нажимаем на первую ячейку в списке") { _ -> Void in
            self.firstRow
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func firstRowHasCounter() -> Self {
        XCTContext.runActivity(named: "Проверяем что первая ячейка содержит бейдж с количеством новых офферов") { _ -> Void in
            self.firstRowOffersCounter
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func firstRowCounterHidden() -> Self {
        XCTContext.runActivity(named: "Проверяем что первая ячейка НЕ содержит бейдж с количеством новых офферов") { _ -> Void in
            self.firstRowOffersCounter
                .yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func segmentHasCount() -> Self {
        XCTContext.runActivity(named: "Проверяем что на сегменте отображается количеством новых офферов") { _ -> Void in
            self.firstRow
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnDelete() -> Self {
        XCTContext.runActivity(named: "Нажимаем на действие Удалить для первой ячейки") { _ -> Void in
            self.firstRowDeleteAction
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func confirmToDelete() -> Self {
        XCTContext.runActivity(named: "Подтверждаем удаление") { _ -> Void in
            self.deleteComfirmButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func emptyViewIsPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем что список пуст") { _ -> Void in
            self.emptyView
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func compareWidgetPromoBannerWithSnapshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем промо баннер виджета со снапшотом") { _ in
            self.widgetPromoBanner
                .yreWaitAndCompareScreenshot(identifier: identifier)
        }
        return self
    }

    @discardableResult
    func compareEmptyViewWithSnapshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем заглушку пустого списка со снапшотом") { _ in
            self.emptyView
                .yreEnsureExistsWithTimeout()
                .yreWaitAndCompareScreenshot(identifier: identifier)
        }
        return self
    }

    @discardableResult
    func emptyViewIsHidden() -> Self {
        XCTContext.runActivity(named: "Проверяем что список НЕ пуст") { _ -> Void in
            self.emptyView
                .yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func errorViewIsPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем что отображается ошибка") { _ -> Void in
            self.errorView
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func appUpdateViewIsPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем что отображается заглушка 'Обновите приложение'") { _ -> Void in
            self.appUpdateView
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func emptyViewActionIsExist() -> Self {
        XCTContext.runActivity(named: "Проверяем, что на пустом списке есть кнопка авторизации") { _ -> Void in
            self.emptyViewAction
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnEmptyViewAction() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку авторизации") { _ -> Void in
            self.emptyViewAction
                .yreTap()
        }
        return self
    }

    @discardableResult
    func emptyViewActionIsHidden() -> Self {
        XCTContext.runActivity(named: "Проверяем, что на пустом списке отсутвует кнопка авторизации") { _ -> Void in
            self.emptyViewAction
                .yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func alertViewIsExist() -> Self {
        XCTContext.runActivity(named: "Проверяем, что на экране есть алерт авторизации") { _ -> Void in
            self.authTableHeader
                .yreEnsureVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func compareAlertViewWithSnapshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем алерт авторизации со снапшотом") { _ in
            self.authTableHeader
                .yreEnsureExistsWithTimeout()
                .yreWaitAndCompareScreenshot(identifier: identifier)
        }
        return self
    }

    @discardableResult
    func tapOnAlertAction() -> Self {
        XCTContext.runActivity(named: "Нажимаем на алерт авторизации") { _ -> Void in
            self.authTableHeader
                .yreForceTap()
        }
        return self
    }

    @discardableResult
    func alertViewIsHidden() -> Self {
        XCTContext.runActivity(named: "Проверяем, что на экране нет алерта") { _ -> Void in
            self.authTableHeader
                .yreEnsureNotVisibleWithTimeout()
        }
        return self
    }

    private typealias Identifiers = SavedSearchesListAccessibilityIdentifiers

    private lazy var viewController = ElementsProvider.obtainElement(identifier: "savedSearch.list")
    private lazy var firstRow = viewController.tables.firstMatch.cells.firstMatch
    private lazy var firstRowDeleteAction = ElementsProvider.obtainElement(identifier: "Удалить", type: .any, in: firstRow)
    private lazy var deleteComfiramation = ElementsProvider.obtainElement(identifier: "savedSearch.list.popup.confirmDelete")
    private lazy var deleteComfirmButton = ElementsProvider.obtainElement(
        identifier: "Удалить подписку",
        type: .any,
        in: deleteComfiramation
    )
    private lazy var emptyView = ElementsProvider.obtainElement(identifier: "savedSearch.list.empty", type: .any, in: viewController)
    private lazy var emptyViewAction = ElementsProvider.obtainElement(identifier: "savedSearch.list.empty.auth", type: .any, in: emptyView)
    private lazy var appUpdateView = ElementsProvider.obtainElement(
        identifier: "savedSearch.list.appUpdate",
        type: .any,
        in: viewController
    )
    
    private lazy var errorView = ElementsProvider.obtainElement(
        identifier: "savedSearch.list.error",
        type: .any,
        in: viewController
    )

    private lazy var authTableHeader = ElementsProvider.obtainElement(identifier: "savedSearch.list.alert",
                                                                      type: .any,
                                                                      in: viewController)
    private lazy var authTableHeaderAction = ElementsProvider.obtainElement(identifier: "savedSearch.alert.action",
                                                                            type: .any,
                                                                            in: authTableHeader)
    private lazy var authTableHeaderTitle = ElementsProvider.obtainElement(identifier: "savedSearch.alert.title",
                                                                           type: .any,
                                                                           in: authTableHeader)

    private lazy var firstRowOffersCounter = ElementsProvider.obtainElement(identifier: "99+ НОВЫХ", type: .any, in: firstRow)

    private lazy var widgetPromoBanner = ElementsProvider.obtainElement(
        identifier: Identifiers.widgetPromoBanner,
        in: self.viewController
    )
    private lazy var widgetPromoBannerCloseButton = ElementsProvider.obtainButton(
        identifier: Identifiers.widgetPromoBannerCloseButton,
        in: self.widgetPromoBanner
    )
    private lazy var widgetPromoBannerActionButton = ElementsProvider.obtainButton(
        identifier: Identifiers.widgetPromoBannerActionButton,
        in: self.widgetPromoBanner
    )

    lazy var refreshControl = ElementsProvider.obtainElement(
        identifier: "savedSearch.list.refreshControl",
        type: .other,
        in: self.viewController
    )
}
