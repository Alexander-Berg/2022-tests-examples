//
//  HintAlertSteps.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 8/7/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import enum YREDesignKit.DrawerAccessibilityIdentifiers
import enum YREDesignKit.ModalActionsViewControllerAccessibilityIdentifiers
import YRETestsUtils
import XCTest

final class ModalActionsAlertSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана с алертом") { _ -> Void in
            let screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)
            screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие экрана с алертом") { _ -> Void in
            let screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)
            screenView.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOn(_ action: String) -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"\(action)\"") { _ -> Void in
            let actionIdentifier = ModalActionsViewControllerAccessibilityIdentifiers.actionButtonIdentifier(for: action)
            let button = ElementsProvider.obtainElement(identifier: actionIdentifier)
            button
                .yreEnsureExists()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func compareWithScreenshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Делаем скриншот экрана алерта") { _ -> Void in
            let screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)
            let screenshot = screenView.yreWaitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
        }
        return self
    }

    @discardableResult
    func tapOnCloseButton() -> Self {
        XCTContext.runActivity(named: "Закрываем экран с алертом") { _ -> Void in
            let screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)
            screenView.yreEnsureExistsWithTimeout()
            
            let button = ElementsProvider.obtainElement(identifier: DrawerAccessibilityIdentifiers.closeButton, in: screenView)
            button
                .yreEnsureExists()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    typealias Identifiers = ModalActionsViewControllerAccessibilityIdentifiers
}
