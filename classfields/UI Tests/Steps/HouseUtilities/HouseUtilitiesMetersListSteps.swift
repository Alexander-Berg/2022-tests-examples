//
//  HouseUtilitiesMetersListSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 11.01.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import enum YREAccessibilityIdentifiers.HouseUtilitiesMetersAccessibilityIdentifiers
import XCTest

final class HouseUtilitiesMetersListSteps {
    @discardableResult
    func ensureListPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана списка счётчиков") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func ensureListContentPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие списка счётчиков на экране") { _ -> Void in
            self.contentView
                .yreEnsureExistsWithTimeout()
                .yreEnsureVisibleWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnCell(at index: Int) -> Self {
        XCTContext.runActivity(named: "Нажимаем на счётчик по индексу \(index)") { _ -> Void in
            let cellID = Identifiers.cell(with: index)
            ElementsProvider
                .obtainElement(identifier: cellID)
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    private typealias Identifiers = HouseUtilitiesMetersAccessibilityIdentifiers.MetersList

    private lazy var screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)
    private lazy var contentView = ElementsProvider.obtainElement(identifier: Identifiers.contentView)
}
