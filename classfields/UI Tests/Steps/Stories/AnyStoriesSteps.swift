//
//  AnyStoriesSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 06.07.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest
import enum YREAccessibilityIdentifiers.StoriesPromoAccessibilityIdentifiers

class AnyStoriesSteps {
    @discardableResult
    func ensureScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана сторисов") { _ -> Void in
            self.view.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func ensureScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие экрана сторисов") { _ -> Void in
            self.view.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapLeft() -> Self {
        XCTContext.runActivity(named: "Тапаем на сторис слева") { _ -> Void in
            self.view
                .yreEnsureExistsWithTimeout()
                .coordinate(withNormalizedOffset: .init(dx: 0.1, dy: 0.5))
                .tap()
        }
        return self
    }

    @discardableResult
    func tapRight() -> Self {
        XCTContext.runActivity(named: "Тапаем на сторис справа") { _ -> Void in
            self.view
                .yreEnsureExistsWithTimeout()
                .coordinate(withNormalizedOffset: .init(dx: 0.9, dy: 0.5))
                .tap()
        }
        return self
    }

    @discardableResult
    func tapCloseButton() -> Self {
        XCTContext.runActivity(named: "Тапаем на крестик в сторис") { _ -> Void in
            ElementsProvider
                .obtainBackButton(in: self.view)
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = StoriesPromoAccessibilityIdentifiers

    private lazy var view = ElementsProvider.obtainElement(identifier: Identifiers.view)
}
