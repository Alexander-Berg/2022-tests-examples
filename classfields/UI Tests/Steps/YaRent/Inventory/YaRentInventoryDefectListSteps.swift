//
//  YaRentInventoryDefectListSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 09.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import enum YREAccessibilityIdentifiers.YaRentInventoryAccessibilityIdentifiers

final class YaRentInventoryDefectListSteps {
    @discardableResult
    func ensureScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие экрана списка дефектов") { _ -> Void in
            self.screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapAddDefectButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку 'добавить дефект'") { _ -> Void in
            self.addDefectButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapDefect(index: Int) -> Self {
        XCTContext.runActivity(named: "Нажимаем на дефект по индексу \(index)") { _ -> Void in
            ElementsProvider
                .obtainElement(identifier: Identifiers.defectCell(index: index))
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapContinueButton() -> Self {
        XCTContext.runActivity(named: "Нажимаем кнопку 'Продолжить'") { _ -> Void in
            self.continueButton
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
        }

        return self
    }

    private typealias Identifiers = YaRentInventoryAccessibilityIdentifiers.DefectList

    private lazy var screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)

    private lazy var addDefectButton = ElementsProvider.obtainElement(identifier: Identifiers.addDefectButton)
    private lazy var continueButton = ElementsProvider.obtainElement(identifier: Identifiers.continueButton)
}
