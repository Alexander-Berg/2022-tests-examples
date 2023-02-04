//
//  ElementsProvider.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 18.01.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

/// Convenient methods to obtain an element in UI hierarchy, no assertions here.
final class ElementsProvider {
    static func obtainElement(
        identifier: String,
        type: XCUIElement.ElementType = .any,
        in container: XCUIElement = XCUIApplication()
    ) -> XCUIElement {
        return container
            .descendants(matching: type)
            .matching(identifier: identifier)
            .element(boundBy: 0)
    }

    static func obtainButton(identifier: String, in container: XCUIElement = XCUIApplication()) -> XCUIElement {
        return self.obtainElement(identifier: identifier, type: .button, in: container)
    }

    static func obtainBackButton(in container: XCUIElement = XCUIApplication()) -> XCUIElement {
        guard let item = self.obtainBackButtonIfExists(in: container) else {
            XCTFail("Couldn't obtain back button")
            return container
        }
        return item
    }

    static func obtainBackButtonIfExists(in container: XCUIElement = XCUIApplication()) -> XCUIElement? {
        let identifier = "navigation.closeButton"

        guard let item = container
            .descendants(matching: .button)
            .matching(identifier: identifier)
            .allElementsBoundByAccessibilityElement
            .last(where: { $0.isHittable })
        else {
            return nil
        }
        return item
    }

    static func obtainTabBar(in container: XCUIElement = XCUIApplication()) -> XCUIElement {
        let identifier = "TabBar"
        return container
            .descendants(matching: .other)
            // @l-saveliy: if you know how make tabBar element belongs to .tabBar type, do it and uncomment next line
            // I try to change accessibilityTraits to .tabBar, but it didn't help
            // .descendants(matching: .tabBar)
            .matching(identifier: identifier)
            .element(boundBy: 0)
    }

    static func obtainFiltersButtonIfExists(in container: XCUIElement = XCUIApplication()) -> XCUIElement? {
        let identifier = "search-results.controls.filters-button"

        let item = container
            .descendants(matching: .button)
            .matching(identifier: identifier)
            .allElementsBoundByAccessibilityElement
            .last(where: { $0.isHittable })
        
        return item
    }

    static func obtainNavigationContainer(in container: XCUIElement = XCUIApplication()) -> XCUIElement {
        return XCTContext.runActivity(named: "Проверяем наличие навигационного контейнера") { _ -> XCUIElement in
            return self.obtainElement(identifier: "Navigation Container View", in: container)
        }
    }

    static func obtainNavigationBar(in container: XCUIElement? = nil) -> XCUIElement {
        return XCTContext.runActivity(named: "Проверяем наличие навигационной панели") { _ -> XCUIElement in
            let containerToUse = container ?? self.obtainNavigationContainer()
            return self.obtainElement(identifier: "mainNavigationBar", in: containerToUse)
        }
    }

    static func obtainNavigationBarContentStackView(in container: XCUIElement? = nil) -> XCUIElement {
        return XCTContext.runActivity(named: "Проверяем наличие стека элементов в навигационной панели") { _ -> XCUIElement in
            let containerToUse = container ?? self.obtainNavigationBar()
            return self.obtainElement(identifier: "navigation.contentStackView", in: containerToUse)
        }
    }

    static func obtainNavigationBarTitleView(in container: XCUIElement? = nil) -> XCUIElement {
        return XCTContext.runActivity(named: "Проверяем наличие заголовка в навигационной панели") { _ -> XCUIElement in
            let containerToUse = container ?? self.obtainNavigationBarContentStackView()
            return self.obtainElement(identifier: "navigation.centeringContentWrapperView", in: containerToUse)
        }
    }
}
