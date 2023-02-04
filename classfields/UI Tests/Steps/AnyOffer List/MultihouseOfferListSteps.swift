//
//  MultihouseOfferListSteps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 14.04.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import class YREAccessibilityIdentifiers.AnyOfferListAccessibilityIdentifiers

final class MultihouseOfferListSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что многодом показан") { _ -> Void in
            self.screen.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func multihouseHeader() -> MultihouseSteps {
        return MultihouseSteps()
    }

    // MARK: Private

    private lazy var screen: XCUIElement = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.multihouseList
    )

    private typealias AccessibilityIdentifiers = AnyOfferListAccessibilityIdentifiers
}
