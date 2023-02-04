//
//  YaRentInventoryObjectDetailsSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 09.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.YaRentInventoryAccessibilityIdentifiers

final class YaRentInventoryObjectDetailsSteps {
    @discardableResult
    func ensureScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана деталей объекта") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapPhoto(index: Int) -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана деталей объекта") { _ -> Void in
            ElementsProvider
                .obtainElement(identifier: Identifiers.photo(index: index))
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = YaRentInventoryAccessibilityIdentifiers.ObjectDetails

    private lazy var screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)
}
