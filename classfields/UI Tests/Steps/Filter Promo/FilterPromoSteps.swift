//
//  FilterPromoSteps.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 12.02.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import YREAccessibilityIdentifiers

final class FilterPromoSteps {
    init(kind: FilterPromoKind) {
        self.kind = kind
    }

    @discardableResult
    func isPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие промо \"\(self.kind.title)\"") { _ -> Void in
            self.view.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnOpenFilters() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Перейти в параметры\"") { _ -> Void in
            self.openFiltersButton.yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnClose() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Понятно\"") { _ -> Void in
            self.closeButton.yreTap()
        }
        return self
    }

    @discardableResult
    func compareWithScreenshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом промо \"\(self.kind.title)\"") { _ -> Void in
            let screenshot = self.view.yreWaitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
        }
        return self
    }

    // MARK: Private

    private let kind: FilterPromoKind

    private lazy var view: XCUIElement = ElementsProvider.obtainElement(
        identifier: self.kind.accessibilityID
    )
    private lazy var openFiltersButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: MapScreenAccessibilityIdentifiers.FiltersPromo.openFiltersButton
    )
    private lazy var closeButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: MapScreenAccessibilityIdentifiers.FiltersPromo.closeButton
    )
}

extension FilterPromoKind {
    fileprivate var accessibilityID: String {
        switch self {
            case .yandexRent:
                return MapScreenAccessibilityIdentifiers.FiltersPromo.yandexRent
            case .notGrannys:
                return MapScreenAccessibilityIdentifiers.FiltersPromo.notGrannys
        }
    }
}
