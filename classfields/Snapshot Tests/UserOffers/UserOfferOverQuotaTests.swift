//
//  UserOfferOverQuotaTests.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 8/25/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
@testable import YREUserOffersModule

final class UserOfferOverQuotaTests: XCTestCase {
    func testAlertLayout() {
        let presenter = UserOffersOverQuotaPresenter()
        let viewController = presenter.viewController
        self.assertSnapshot(viewController.view)
    }
}
