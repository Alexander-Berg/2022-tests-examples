//
//  AppUpdateStubSteps.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 23.02.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class AppUpdateStubSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        self.screenView.yreEnsureExistsWithTimeout()
        return self
    }

    @discardableResult
    func isUpdateButtonTappable() -> Self {
        self.updateAppButton.yreEnsureExists()
        XCTAssertTrue(self.updateAppButton.isEnabled)
        XCTAssertTrue(self.updateAppButton.isHittable)
        return self
    }

    // MARK: Private

    private lazy var screenView: XCUIElement = ElementsProvider.obtainElement(identifier: "appUpdateStub.view")
    private lazy var updateAppButton: XCUIElement = ElementsProvider.obtainElement(identifier: "appUpdateStub.updateButton")
}
