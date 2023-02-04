//
//  InAppServicesSteps.swift
//  UI Tests
//
//  Created by Alexander Kolovatov on 16.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import enum YREAccessibilityIdentifiers.InAppServicesAccessibilityIdentifiers

final class InAppServicesSteps {
    lazy var servicesCollection = ElementsProvider.obtainElement(
        identifier: Identifiers.collectionView,
        type: .collectionView,
        in: self.screenView
    )

    private typealias Identifiers = InAppServicesAccessibilityIdentifiers

    private lazy var screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)

    private lazy var paidExcerptsListCell = ElementsProvider.obtainElement(
        identifier: Identifiers.Services.excerpts,
        type: .cell,
        in: self.servicesCollection
    )

    private lazy var mortgageListCell = ElementsProvider.obtainElement(
        identifier: Identifiers.Services.mortgage,
        type: .cell,
        in: self.servicesCollection
    )
    
    private lazy var manualScreenCell = ElementsProvider.obtainElement(
        identifier: Identifiers.Services.manual,
        in: self.servicesCollection
    )

    private lazy var addOfferListCell = ElementsProvider.obtainElement(
        identifier: Identifiers.Services.addOffer,
        type: .cell,
        in: self.servicesCollection
    )
}

extension InAppServicesSteps {
    @discardableResult
    func tapOnPaidExcertpsList() -> Self {
        XCTContext.runActivity(named: "Открываем экран с разделом отчетов") { _ -> Void in
        self.paidExcerptsListCell
            .yreEnsureExists()
            .yreTap()
        }
        return self
    }
    
    @discardableResult
    func tapOnMortgageListCell() -> Self {
        XCTContext.runActivity(named: "Открываем экран с разделом ипотек") { _ -> Void in
            self.mortgageListCell
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnManualCell() -> Self {
        XCTContext.runActivity(named: "Открываем журнал недвижимости") { _ -> Void in
            self.manualScreenCell
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnUserOffersListSection() -> Self {
        XCTContext.runActivity(named: "Открываем список моих объявлений") { _ -> Void in
            InAppServicesUserOffersSteps()
                .isSectionPresented()
                .tapOnHeaderTitle()
        }
        return self
    }

    @discardableResult
    func tapOnAddOffer() -> Self {
        XCTContext.runActivity(named: "Открываем экран добавления оффера") { _ -> Void in
            InAppServicesUserOffersSteps()
                .isCreateSectionPresented()
                .tapOnCreateSection()
        }
        return self
    }
}

extension InAppServicesSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана 'Сервисы'") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isContentPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие контента на экране 'Сервисы'") { _ -> Void in
            self.servicesCollection.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isBannedInRealtyPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие нотификации \"Забанен в Недвижимости\"") { _ -> Void in
            let notification = ElementsProvider.obtainElement(identifier: Identifiers.Notifications.bannedUser, in: self.servicesCollection)
            notification
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnBannedInRealtyAction() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку в нотификации \"Забанен в Недвижимости\"") { _ -> Void in
            let notification = ElementsProvider.obtainElement(identifier: Identifiers.Notifications.bannedUser, in: self.servicesCollection)
            let actionButton = ElementsProvider.obtainElement(identifier: Identifiers.Notifications.actionButton, in: notification)

            actionButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func isErrorPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие нотификации ошибки") { _ -> Void in
            let notification = ElementsProvider.obtainElement(identifier: Identifiers.Notifications.error, in: self.servicesCollection)
            notification
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnFirstFlat() -> Self {
        InAppServicesFlatsSectionSteps()
            .isPresented()
            .tapOnFlat()
        return self
    }

    @discardableResult
    func tapOnRegion() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку с регионом") { _ -> Void in
            ElementsProvider.obtainElement(identifier: Identifiers.Region.view, in: self.screenView)
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnFlatShowings() -> Self {
        InAppServicesFlatsSectionSteps()
            .isPresented()
            .tapOnShowings()
        return self
    }
}
