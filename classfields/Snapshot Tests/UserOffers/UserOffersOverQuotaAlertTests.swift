//
//  UserOffersOverQuotaAlertTests.swift
//  Unit Tests
//
//  Created by Timur Guliamov on 22.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import UIKit
import XCTest
@testable import YREUserOffersModule

final class UserOffersOverQuotaAlertTests: XCTestCase {
    func testAlertView() {
        let presenter = UserOffersOverQuotaPresenter()
        let viewController = presenter.viewController
        self.assertSnapshot(viewController.view)
    }
}
