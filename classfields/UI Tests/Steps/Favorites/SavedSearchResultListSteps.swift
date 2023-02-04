//
//  SavedSearchResultListSteps.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 4/28/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREAccessibilityIdentifiers

final class SavedSearchResultListSteps {
    @discardableResult
    func screenIsPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана Выдачи результа сохраненного поиска") { _ -> Void in
            self.viewController.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func newOffersPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем что секция с новыми офферами отображается") { _ -> Void in
            self.newOffersSectionHeader
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    func newOffersNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем что секция с новыми офферами НЕ отображается") { _ -> Void in
            self.newOffersSectionHeader
                .yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func tapOnSearchParamsButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на параметры сохраненного поиска") { _ -> Void in
            self.searchParams
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnCloseButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку Закрыть") { _ -> Void in
            let navigationContainer = ElementsProvider.obtainNavigationContainer()
            let closeButton = ElementsProvider.obtainBackButton(in: navigationContainer)
            closeButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    func withOfferList() -> OfferListSteps {
        return OfferListSteps(screen: self.viewController)
    }

    func withSiteList() -> SiteListSteps {
        return SiteListSteps(screen: self.viewController)
    }

    func withVillageList() -> VillageListSteps {
        return VillageListSteps(screen: self.viewController)
    }

    // MARK: - Private
    
    private lazy var viewController = ElementsProvider.obtainElement(identifier: "savedSearch.results")
    private lazy var searchParams = ElementsProvider.obtainElement(
        identifier: "savedSearch.results.button.showParams",
        type: .any,
        in: self.viewController
    )
    
    private lazy var newOffersSectionHeader = self.viewController.staticTexts["НОВЫЕ ОБЪЯВЛЕНИЯ"].firstMatch

    private func listStepsProvider(cellID: String) -> AnyOfferListStepsProvider {
        return AnyOfferListStepsProvider(
            container: self.viewController,
            cellID: cellID
        )
    }
}
