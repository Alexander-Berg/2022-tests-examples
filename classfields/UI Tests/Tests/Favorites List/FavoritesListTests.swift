//
//  FavoritesListTests.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 08.04.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest

final class FavoritesListTests: BaseTestCase {
    func testAuthorizationAlert() {
        FavoritesAPIStubConfigurator.setupListing(using: self.dynamicStubs)
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .common(.singleOffer))

        self.relaunchApp(with: .unauthorizedFavoritesTests)

        let favoritesListSteps = FavoritesListSteps()

        favoritesListSteps
            .screenIsPresented()
            .headerAlert()
            .isPresented()
            .isHittable()
            .isActionButtonHittable()
            .ensureButtonLabelHasText("Войти")
    }

    func testNotificationAlert() {
        FavoritesAPIStubConfigurator.setupListing(using: self.dynamicStubs)
        SnippetsListAPIStubConfigurator.setupOffersList(using: self.dynamicStubs, stubKind: .common(.singleOffer))

        self.relaunchApp(with: .favoritesTests)

        let favoritesListSteps = FavoritesListSteps()

        favoritesListSteps
            .screenIsPresented()
            .headerAlert()
            .isPresented()
            .isHittable()
            .isActionButtonHittable()
            .ensureButtonLabelHasText("Разрешить уведомления")
    }
}
