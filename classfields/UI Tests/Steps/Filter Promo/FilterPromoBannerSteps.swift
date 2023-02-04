//
//  FilterPromoBannerSteps.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 12.02.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import enum YREAccessibilityIdentifiers.MainOfferListAccessibilityIdentifiers

final class FilterPromoBannerSteps {
    init(kind: FilterPromoKind, in container: XCUIElement? = nil) {
        if let container = container {
            self.view = ElementsProvider.obtainElement(identifier: kind.accessibilityID, in: container)
        }
        else {
            self.view = ElementsProvider.obtainElement(identifier: kind.accessibilityID)
        }

        self.kind = kind
    }

    @discardableResult
    func isPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие баннера \"\(self.kind.title)\"") { _ -> Void in
            self.view.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие баннера \"\(self.kind.title)\"") { _ -> Void in
            self.view.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func toggle() -> Self {
        XCTContext.runActivity(named: "Переключаем фильтр") { _ -> Void in
            self.switchControl.yreForceTap()
        }
        return self
    }

    @discardableResult
    func tapOnHide() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Скрыть\"") { _ -> Void in
            self.hideButton.yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnInfo() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку \"Подробнее\"") { _ -> Void in
            self.infoButton.yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnBanner() -> Self {
        XCTContext.runActivity(named: "Нажимаем на баннер") { _ -> Void in
            self.view
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func compareWithScreenshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом промо-баннера \"\(self.kind.title)\"") { _ -> Void in
            let screenshot = self.view.yreWaitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
        }
        return self
    }

    // MARK: Private

    private let view: XCUIElement
    private let kind: FilterPromoKind

    private lazy var hideButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: MainOfferListAccessibilityIdentifiers.FiltersPromoBanner.hideButton
    )
    private lazy var infoButton: XCUIElement = ElementsProvider.obtainElement(
        identifier: MainOfferListAccessibilityIdentifiers.FiltersPromoBanner.infoButton
    )
    private lazy var switchControl: XCUIElement = ElementsProvider.obtainElement(
        identifier: MainOfferListAccessibilityIdentifiers.FiltersPromoBanner.filterSwitch
    )
}

extension FilterPromoKind {
    fileprivate var accessibilityID: String {
        switch self {
            case .yandexRent:
                return MainOfferListAccessibilityIdentifiers.FiltersPromoBanner.yandexRent
            case .notGrannys:
                return MainOfferListAccessibilityIdentifiers.FiltersPromoBanner.notGrannys
        }
    }
}
