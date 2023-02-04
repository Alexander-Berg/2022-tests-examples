//
//  FavouritesListTests.swift
//  Unit Tests
//
//  Created by Denis Mamnitskii on 13.07.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
@testable import YREFavoritesListModule

final class FavouritesListTest: XCTestCase {
    func testHeaderAuthorizationAlert() {
        let headerViewModel = FavoritesHeaderView.ViewModel(state: .needsAuthorization)

        let headerView = FavoritesHeaderView(viewModel: headerViewModel)
        headerView.frame = self.headerAlertFrame(headerViewModel)

        self.assertSnapshot(headerView)
    }

    func testHeaderNotificationSettingsAlert() {
        let headerViewModel = FavoritesHeaderView.ViewModel(state: .enableNotifications(.disabled))

        let headerView = FavoritesHeaderView(viewModel: headerViewModel)
        headerView.frame = self.headerAlertFrame(headerViewModel)

        self.assertSnapshot(headerView)
    }

    func testHeaderNotificationAllowAlert() {
        let headerViewModel = FavoritesHeaderView.ViewModel(state: .enableNotifications(.notRequested))

        let headerView = FavoritesHeaderView(viewModel: headerViewModel)
        headerView.frame = self.headerAlertFrame(headerViewModel)

        self.assertSnapshot(headerView)
    }

    func testHeaderNotificationEnableAlert() {
        let headerViewModel = FavoritesHeaderView.ViewModel(state: .enableNotifications(.turnedOff))

        let headerView = FavoritesHeaderView(viewModel: headerViewModel)
        headerView.frame = self.headerAlertFrame(headerViewModel)

        self.assertSnapshot(headerView)
    }

    private func headerAlertFrame(_ viewModel: FavoritesHeaderView.ViewModel) -> CGRect {
        let width = UIScreen.main.bounds.width
        let height = FavoritesHeaderView.height(viewModel: viewModel, width: width)
        
        return CGRect(origin: .zero, size: CGSize(width: width, height: height))
    }
}
