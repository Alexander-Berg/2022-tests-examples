//
//  MapSteps.swift
//  UI Tests
//
//  Created by Rinat Enikeev on 6/26/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.MapScreenAccessibilityIdentifiers

final class MapScreenSteps {
    @discardableResult
    func pressCommuteButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на контрол 'Время на дорогу'") { _ -> Void in
            // @l-saveliy: check here enabled instead hittable
            // because of some unexpected crash in XCTest during isHittable check
            self.commuteButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureEnabledWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapMultihouseButton() -> Self {
        XCTContext.runActivity(named: "Имитируем нажатие на пин многодома") { _ -> Void in
            self.multihouseButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapSnippetButton() -> Self {
        XCTContext.runActivity(named: "Имитируем нажатие на пин оффера") { _ -> Void in
            self.snippetButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    // MARK: - Private

    private typealias AccessibilityIdentifiers = MapScreenAccessibilityIdentifiers

    private lazy var commuteButton = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.Map.Controls.commute
    )
    private lazy var multihouseButton = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.Map.multihouseButton
    )
    private lazy var snippetButton = ElementsProvider.obtainElement(
        identifier: AccessibilityIdentifiers.Map.snippetButton
    )
}
