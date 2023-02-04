//
//  PaidExcerptsSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 24.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
import enum YREAccessibilityIdentifiers.PaidExcerptsAccessibilityIdentifiers

final class PaidExcerptsSteps {
    lazy var viewController = ElementsProvider.obtainElement(identifier: Identifiers.viewController)
    
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана платного отчета") { _ -> Void in
            self.viewController.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapBackButton() -> Self {
        XCTContext.runActivity(
            named: "Тапаем 'Назад' на экране платного отчета"
        ) { _ -> Void in
            ElementsProvider.obtainBackButton()
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = PaidExcerptsAccessibilityIdentifiers
}
