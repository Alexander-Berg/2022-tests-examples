//
//  StartupPromoTests.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 12/28/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
@testable import YRERootModule

final class StartupPromoTests: XCTestCase {
    func testStartupPromoLayout() {
        let presenter = StartupPromoPresenter()
        let viewController = presenter.viewController
        self.assertSnapshot(viewController.view)
    }

    func testFavouritesPromoLayout() {
        let presenter = FavouritesPromoPresenter()
        let viewController = presenter.viewController
        self.assertSnapshot(viewController.view)
    }
}
