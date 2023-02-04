//
//  YaRentInventoryPromoTests.swift
//  UI Tests
//
//  Created by Leontyev Saveliy on 06.07.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import YREAppConfig
import XCTest

final class YaRentInventoryPromoTests: BaseTestCase {
    override func setUp() {
        super.setUp()

        InAppServicesStubConfigurator.setupEmptyServiceInfo(using: self.dynamicStubs)
        RentAPIStubConfiguration.setupFlatsWithOneFlat(using: self.dynamicStubs)
    }
    
    func testShowOwnerPromoAndThenCloseAfterLastSlide() {
        RentAPIStubConfiguration.setupOwnerNeedToFillOutInventory(using: self.dynamicStubs)

        let config = ExternalAppConfiguration.inAppServicesTests
        config.yaRentInventoryOwnerPromoWasShown = false
        self.relaunchApp(with: config)

        InAppServicesSteps()
            .isScreenPresented()
            .tapOnFirstFlat()

        let storiesSteps = InventoryOwnerPromoStoriesSteps()

        for slide in 0..<3 {
            storiesSteps
                .ensureSlidePresented(slide: slide)
                .tapRight()
        }
        storiesSteps
            .ensureScreenNotPresented()
    }

    func testShowTenantPromoAndThenCloseByCloseButton() {
        RentAPIStubConfiguration.setupTenantNeedToConfirmInventory(using: self.dynamicStubs)

        let config = ExternalAppConfiguration.inAppServicesTests
        config.yaRentInventoryTenantPromoWasShown = false
        self.relaunchApp(with: config)

        InAppServicesSteps()
            .isScreenPresented()
            .tapOnFirstFlat()

        let storiesSteps = InventoryTenantPromoStoriesSteps()

        for slide in 0..<2 {
            storiesSteps
                .ensureSlidePresented(slide: slide)
                .tapRight()
        }
        storiesSteps
            .ensureSlidePresented(slide: 2)
            .tapCloseButton()
            .ensureScreenNotPresented()
    }

    func testNotShowPromosIfNoNotifications() {
        RentAPIStubConfiguration.setupOwnerFlatWithNoNotifications(using: self.dynamicStubs)

        let config = ExternalAppConfiguration.inAppServicesTests
        config.yaRentInventoryTenantPromoWasShown = false
        config.yaRentInventoryOwnerPromoWasShown = false
        self.relaunchApp(with: config)

        InAppServicesSteps()
            .isScreenPresented()
            .tapOnFirstFlat()

        YaRentFlatCardSteps()
            .isScreenPresented()

        InventoryTenantPromoStoriesSteps()
            .ensureScreenNotPresented()

        InventoryOwnerPromoStoriesSteps()
            .ensureScreenNotPresented()
    }
}
