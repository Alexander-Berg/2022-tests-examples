//
//  VillageOfferListSteps.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 09.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import class YREAccessibilityIdentifiers.VillageOfferListAccessibilityIdentifiers

final class VillageOfferListSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что экран списка офферов в КП показан") { _ -> Void in
            self.screen.yreEnsureExistsWithTimeout()
        }
        return self
    }

    // MARK: Private

    private typealias Identifiers = VillageOfferListAccessibilityIdentifiers

    private lazy var screen: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.view
    )

    private lazy var callButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: Identifiers.callButton,
        in: self.screen
    )
}

extension VillageOfferListSteps: CallButtonHandler {
    @discardableResult
    func tapOnCallButton() -> Self {
        XCTContext.runActivity(named: "Нажимает на кнопку Позвонить") { _ -> Void in
            self.callButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabled()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }
}
