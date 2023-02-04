//
//  InventoryPromoStoriesSteps.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 06.07.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import YREAccessibilityIdentifiers
import XCTest

final class InventoryOwnerPromoStoriesSteps: AnyStoriesSteps {
    @discardableResult
    func ensureSlidePresented(slide: Int) -> Self {
        XCTContext.runActivity(named: "Проверяем наличие слайда описи соба \(slide)") { _ -> Void in
            let elementID = Identifiers.viewID(slide: slide)
            ElementsProvider
                .obtainElement(identifier: elementID)
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    private typealias Identifiers = StoriesPromoAccessibilityIdentifiers.YaRentInventory.Owner
}

final class InventoryTenantPromoStoriesSteps: AnyStoriesSteps {
    @discardableResult
    func ensureSlidePresented(slide: Int) -> Self {
        XCTContext.runActivity(named: "Проверяем наличие слайда описи жильца \(slide)") { _ -> Void in
            let elementID = Identifiers.viewID(slide: slide)
            ElementsProvider
                .obtainElement(identifier: elementID)
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    private typealias Identifiers = StoriesPromoAccessibilityIdentifiers.YaRentInventory.Tenant
}
