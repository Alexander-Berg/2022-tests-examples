//
//  SavedSearchesWidgetPromoSteps.swift
//  UI Tests
//
//  Created by Anfisa Klisho on 17.09.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import enum YREAccessibilityIdentifiers.SavedSearchesWidgetPromoAccessibilityIdentifiers

final class SavedSearchesWidgetPromoSteps {
    @discardableResult
    func widgetPromoIsPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что промо показывается") { _ -> Void in
            self.promoView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func widgetPromoIsNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что промо не показывается") { _ -> Void in
            self.promoView.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapActionButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку Понятно") { _ -> Void in
            self.actionButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapCloseButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку Закрыть") { _ -> Void in
            self.closeButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    // MARK: - Private
    private typealias Identifiers = SavedSearchesWidgetPromoAccessibilityIdentifiers

    private lazy var promoView = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var closeButton = ElementsProvider.obtainBackButton(in: self.promoView) 
    private lazy var actionButton = ElementsProvider.obtainButton(identifier: Identifiers.actionButton, in: self.promoView)
}
