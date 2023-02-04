//
//  PhoneQuerySteps.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 24.03.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREAccessibilityIdentifiers

final class PhoneQuerySteps {
    @discardableResult
    func isPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что \"\(self.screenTitle)\" показан") { _ -> Void in
            self.view.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что \"\(self.screenTitle)\" отсутствует") { _ -> Void in
            self.view.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func hasOptions(_ options: [String]) -> Self {
        let optionsString = options.joined(separator: ", ")
        XCTContext.runActivity(named: "Проверяем, что \"\(self.screenTitle)\" имеет доступные опции \(optionsString)") { _ -> Void in
            for option in options {
                let item = self.optionItem(option)
                item.yreEnsureExists()
            }
        }
        return self
    }

    @discardableResult
    func close() -> Self {
        XCTContext.runActivity(named: "Закрываем \"\(self.screenTitle)\"") { _ -> Void in
            self.closeButton
                .yreEnsureExists()
                .yreTap()
        }

        return self
    }

    // MARK: Private

    private typealias Identifiers = PhoneQueryAccessibilityIdentifiers

    private lazy var view = ElementsProvider.obtainElement(
        identifier: Identifiers.view
    )
    private lazy var closeButton = ElementsProvider.obtainElement(
        identifier: "\(Identifiers.optionPrefix).Отмена",
        in: self.view
    )

    private let screenTitle: String = "Пикер телефонов"

    private func optionItem(_ title: String) -> XCUIElement {
        return ElementsProvider.obtainElement(
            identifier: "\(Identifiers.optionPrefix).\(title)",
            in: self.view
        )
    }
}
