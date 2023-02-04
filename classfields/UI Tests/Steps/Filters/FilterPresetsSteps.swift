//
//  FilterPresetsSteps.swift
//  UI Tests
//
//  Created by Alexey Salangin on 3/20/21.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YRETestsUtils
import YREAccessibilityIdentifiers

final class FilterPresetsSteps {
    typealias PresetType = FiltersPresetsAccessibilityIdentifiers.PresetType

    init(presetsCell: XCUIElement) {
        self.presetsCell = presetsCell
    }

    @discardableResult
    func tapOnPresetCell(type: PresetType, takeScreenshot: Bool = false) -> Self {
        XCTContext.runActivity(named: "Нажимаем на пресет \(type.rawValue)") { _ -> Void in
            let cell = ElementsProvider.obtainElement(
                identifier: FiltersPresetsAccessibilityIdentifiers.presetCell(type: type),
                type: .cell,
                in: self.presetsCell
            )

            self.presetsCell.scrollToElement(element: cell, direction: .left, swipeLimits: 2)

            if takeScreenshot {
                let screenshot = presetsCell.yreWaitAndScreenshot()
                Snapshot.compareWithSnapshot(image: screenshot, identifier: "filterPresets.\(type.rawValue)")
            }

            cell
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func isPresetNotExists(type: PresetType) -> Self {
        XCTContext.runActivity(named: "Проверяем, что отсутствует пресет \(type.rawValue)") { _ -> Void in
            let cell = ElementsProvider.obtainElement(
                identifier: FiltersPresetsAccessibilityIdentifiers.presetCell(type: type),
                type: .cell,
                in: self.presetsCell
            )

            self.presetsCell.scrollToElement(element: cell, direction: .left, swipeLimits: 2)

            cell.yreEnsureNotExists()
        }
        return self
    }

    private let presetsCell: XCUIElement
}
