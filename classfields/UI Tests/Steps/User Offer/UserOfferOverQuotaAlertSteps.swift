//
//  UserOfferOverQuotaAlertSteps.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 20.08.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils

final class UserOfferOverQuotaAlertSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие алерта 'Over quota'") { _ -> Void in
            self.screenView.yreEnsureVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func isScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие алерта 'Over quota'") { _ -> Void in
            self.screenView.yreEnsureNotVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnAcceptButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Понятно'") { _ -> Void in
            self.acceptButton.tap()
        }
        return self
    }

    @discardableResult
    func tapOnReadRulesButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Читать условия'") { _ -> Void in
            self.readRulesButton.tap()
        }
        return self
    }

    @discardableResult
    func tapOnCloseButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку 'Закрыть'") { _ -> Void in
            self.closeButton.tap()
        }
        return self
    }

    @discardableResult
    func compareWithScreenshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом алерта OverQuota") { _ -> Void in
            let screenshot = self.screenView.yreWaitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
        }
        return self
    }

    @discardableResult
    func browser() -> WrappedBrowserSteps {
        return WrappedBrowserSteps()
    }

    // MARK: Private

    private enum Identifiers {
        static let screen = "over_quota_promo"
        static let acceptButton = "over_quota_promo.accept"
        static let readRulesButton = "over_quota_promo.read_rules"
        static let closeButton = "navigation.closeButton"
    }
    
    private lazy var screenView: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.screen)
    private lazy var acceptButton: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.acceptButton,
                                                                                in: self.screenView)
    private lazy var readRulesButton: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.readRulesButton,
                                                                                   in: self.screenView)
    private lazy var closeButton: XCUIElement = ElementsProvider.obtainElement(identifier: Identifiers.closeButton,
                                                                               in: self.screenView)
}
