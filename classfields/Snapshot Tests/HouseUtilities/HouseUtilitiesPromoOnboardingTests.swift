//
//  HouseUtilitiesPromoTenantTests.swift
//  Unit Tests
//
//  Created by Maksim Zenkov on 24.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import UIKit
import XCTest
@testable import YREInAppServicesModule
@testable import YREStoriesPromoModule
import YREServerDrivenUI
import YREModel
import YREDesignKit
import YREStories

final class HouseUtilitiesPromoOnboardingTests: XCTestCase {
    
    func testHouseUtilitesPromoTenant() {
        self.testPromo(userRole: .tenant)
    }
    
    func testHouseUtilitesPromoOwner() {
        self.testPromo(userRole: .owner)
    }
    
    private func testPromo(userRole: HouseUtilitiesUserRole, function: String = #function) {
        let hostView = Self.hostView()
        let divPages = Self.divPages(userRole: userRole)
        
        for slide in 0..<divPages.count {
            hostView.setData(divPages[slide])
            self.assertSnapshot(hostView, function: function + "_slide_\(slide)")
        }
    }
}

extension HouseUtilitiesPromoOnboardingTests {
    private static func hostView() -> HostView {
        let hostView = HostView()
        hostView.frame = UIScreen.main.bounds
        return hostView
    }
    
    private static func divPages(userRole: HouseUtilitiesUserRole) -> [ViewModel] {
        let interactor = HouseUtilitiesStoriesPromoInteractor(userRole: userRole)
        let resource = interactor.dataAsset.data.data
        return YREServerDrivenUI.ViewModelGenerator.viewModels(from: resource)
    }
}
