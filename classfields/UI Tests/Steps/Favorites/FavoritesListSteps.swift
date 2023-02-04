//
//  FavoritesListSteps.swift
//  UI Tests
//
//  Created by Rinat Enikeev on 5/19/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREAccessibilityIdentifiers

final class FavoritesListSteps {
    @discardableResult
    func screenIsPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана Избранное") { _ -> Void in
            self.viewController.yreEnsureExistsWithTimeout()
        }
        return self
    }

    func withOfferList() -> OfferListSteps {
        return OfferListSteps(screen: self.viewController, customTimeout: LocalConstants.timeout)
    }

    func withSiteList() -> SiteListSteps {
        return SiteListSteps(screen: self.viewController, customTimeout: LocalConstants.timeout)
    }

    func withVillageList() -> VillageListSteps {
        return VillageListSteps(screen: self.viewController, customTimeout: LocalConstants.timeout)
    }

    func headerAlert() -> HeaderAlertSteps {
        return XCTContext.runActivity(named: "Получаем текущее уведомление над списком") { _ -> HeaderAlertSteps in
            let header = self.tableHeaderView
            header.yreEnsureExistsWithTimeout(timeout: LocalConstants.timeout)

            return HeaderAlertSteps(element: header)
        }
    }

    @discardableResult
    func tapOnSitesTag() -> Self {
        return self.tapOnTag(name: "Новостройки")
    }

    @discardableResult
    func tapOnVillagesTag() -> Self {
        return self.tapOnTag(name: "Коттеджные посёлки")
    }

    @discardableResult
    func сonfirmRemoving() -> Self {
        XCTContext.runActivity(named: "Подтверждаем удаление") { _ -> Void in
            let alert = AnyAlertSteps(
                elementType: .alert,
                alertID: FavoritesListAccessibilityIdentifiers.RemovingAttemptAlert.view
            )
            alert
                .screenIsPresented()
                .tapOnButton(withID: FavoritesListAccessibilityIdentifiers.RemovingAttemptAlert.confirmAction)
        }
        return self
    }

    // MARK: - Private

    private enum LocalConstants {
        // TODO: remove timeout. @arkadysmirnov: i don't know why snippets loaded so long time
        static let timeout: TimeInterval = 20
    }

    private lazy var viewController = ElementsProvider.obtainElement(identifier: FavoritesListAccessibilityIdentifiers.list)
    private lazy var tableView = ElementsProvider.obtainElement(identifier: FavoritesListAccessibilityIdentifiers.table)
    private lazy var tableHeaderView = ElementsProvider.obtainElement(identifier: FavoritesListAccessibilityIdentifiers.headerAlert)

    private func tapOnTag(name: String) -> Self {
        return XCTContext.runActivity(named: "Нажимаем на вкладку '\(name)'") { _ -> Self in
            let tagsContainer = ElementsProvider.obtainElement(identifier: FavoritesListAccessibilityIdentifiers.tags)
            let chip = ElementsProvider.obtainElement(identifier: name, in: tagsContainer)
            chip
                .yreEnsureExistsWithTimeout(timeout: LocalConstants.timeout)
                .yreTap()
            return self
        }
    }
}
