//
//  AbuseSteps.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 24.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.AbuseAccessibilityIdentifiers

final class AbuseSteps {
    @discardableResult
    func isOfferAbuseScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие на экране списка жалоб на объявление") { _ -> Void in
            self.offerAbuseView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isOfferAbuseScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие на экране списка жалоб на объявление") { _ -> Void in
            self.offerAbuseView.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnCloseButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Закрыть\"") { _ -> Void in
            self.offerAbuseCloseButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    // MARK: - Private

    private typealias AccessibilityIdentifiers = AbuseAccessibilityIdentifiers

    private lazy var offerAbuseView = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.Offer.screen.stringValue,
        type: .other
    )

    private lazy var offerAbuseCloseButton = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.Offer.closeButton.stringValue,
        type: .button,
        in: self.offerAbuseView
    )
}
