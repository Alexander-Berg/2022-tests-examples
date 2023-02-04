//
//  PushNotificationsIntroSteps.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 22.04.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.RootAccessibilityIdentifiers

final class PushNotificationsIntroSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана '\(Self.screenTitle)'") { _ -> Void in
            self.screen.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие экрана '\(Self.screenTitle)'") { _ -> Void in
            self.screen.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func close() -> Self {
        XCTContext.runActivity(named: "Закрываем экран '\(Self.screenTitle)'") { _ -> Void in
            self.closeButton
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    // MARK: - Private

    private typealias AccessibilityIdentifiers = RootAccessibilityIdentifiers

    private lazy var screen = ElementsProvider.obtainElement(identifier: AccessibilityIdentifiers.pushIntro)
    private lazy var closeButton = ElementsProvider.obtainBackButton(in: self.screen)

    private static let screenTitle: String = "Запрос разрешения на push-уведомления"
}
