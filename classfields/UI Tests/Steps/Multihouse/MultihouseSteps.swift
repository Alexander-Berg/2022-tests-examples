//
//  MultihouseSteps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 31.03.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import YREAccessibilityIdentifiers

final class MultihouseSteps {
    @discardableResult
    func isPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие многодома") { _ -> Void in
            self.view.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tap() -> SiteCardSteps {
        XCTContext.runActivity(named: "Нажимаем на блок многодома") { _ -> Void in
            self.view
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabledWithTimeout()
                .yreEnsureHittableWithTimeout()
                .yreTap()
        }
        return SiteCardSteps()
    }

    // MARK: Private

    private lazy var view: XCUIElement = ElementsProvider.obtainElement(
        identifier: MainOfferListAccessibilityIdentifiers.Multihouse.view
    )
}
