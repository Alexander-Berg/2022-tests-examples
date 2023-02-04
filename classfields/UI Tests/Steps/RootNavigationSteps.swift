//
//  RootNavigationSteps.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 18.01.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

/// RootRouter and YREParallelNavigationController related stuff
final class RootNavigationSteps {
    @discardableResult
    func dismissModallyPresented() -> Self {
        XCTContext.runActivity(named: "Скрываем модальный экран") { _ -> Void in
            let navigationContainer = ElementsProvider.obtainNavigationContainer()

            let closeButton = ElementsProvider.obtainBackButton(in: navigationContainer)
            closeButton
                .yreEnsureExistsWithTimeout()
                .tap()
            sleep(UInt32(Constants.timeout))
        }
        return self
    }
}

/// TabBar
extension RootNavigationSteps {
    @discardableResult
    func tabBarTapOnSearchItem() -> Self {
        return XCTContext.runActivity(named: "Нажимаем на таб 'Поиск'") { _ -> Self in
            return self.tabBarTapOnItem("tabbar_search")
        }
    }

    @discardableResult
    func tabBarTapOnFavoriteItem() -> Self {
        return XCTContext.runActivity(named: "Нажимаем на таб 'Избранное'") { _ -> Self in
            return self.tabBarTapOnItem("tabbar_favorite")
        }
    }

    @discardableResult
    func tabBarTapOnComunicationItem() -> Self {
        return XCTContext.runActivity(named: "Нажимаем на таб 'Общение'") { _ -> Self in
            return self.tabBarTapOnItem("tabbar_communication")
        }
    }

    @discardableResult
    func tabBarTapOnHomeItem() -> Self {
        return XCTContext.runActivity(named: "Нажимаем на таб 'Сервисы'") { _ -> Self in
            return self.tabBarTapOnItem("tabbar_home")
        }
    }

    @discardableResult
    func tabBarTapOnProfileItem() -> Self {
        return XCTContext.runActivity(named: "Нажимаем на таб 'Профиль'") { _ -> Self in
            return self.tabBarTapOnItem("tabbar_profile")
        }
    }

    @discardableResult
    func tabBarTapOnItem(_ tabBarItemID: String) -> Self {
        XCTContext.runActivity(named: "Нажимаем на таб с идентификатором '\(tabBarItemID)'") { _ -> Void in
            let searchTabBarItem = ElementsProvider.obtainElement(identifier: tabBarItemID)
            searchTabBarItem
                .yreEnsureExistsWithTimeout()
                .tap()
        }
        return self
    }

    @discardableResult
    func tabBarFavoriteItemHasBadge() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие бейджа на табе 'Избранное'") { _ -> Void in
            let searchTabBarItem = ElementsProvider.obtainElement(identifier: "tabbar_favorite")
            let badge = ElementsProvider.obtainElement(identifier: "tabbar.item.badge", type: .any, in: searchTabBarItem)
            badge.yreEnsureExistsWithTimeout()
        }
        return self
    }
}
