//
//  AnySnippetStepsProvider.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 25.09.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

/// Convenient methods to assert the most common cases for a snippet of any offers (YREOffer, YRESite, YREVillage).
/// Not intended to be used directly from tests.
final class AnySnippetStepsProvider {
    let cell: XCUIElement

    init(cell: XCUIElement) {
        self.cell = cell
    }

    func callButton(_ callButton: XCUIElement, labelStartsWith prefix: String) {
        XCTContext.runActivity(named: "Проверяем наличие префикса \(prefix) у кнопки звонка") { _ -> Void in
            callButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureLabelStarts(with: prefix)
        }
    }

    func isCallButtonTappable(_ callButton: XCUIElement) {
        XCTContext.runActivity(named: "Проверяем доступность кнопки \"Позвонить\"") { _ -> Void in
            callButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
        }
    }

    func isFavoritesButtonTappable(_ favoritesButton: XCUIElement) {
        XCTContext.runActivity(named: "Проверяем доступность кнопки \"Избранного\"") { _ -> Void in
            favoritesButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
        }
    }
}
