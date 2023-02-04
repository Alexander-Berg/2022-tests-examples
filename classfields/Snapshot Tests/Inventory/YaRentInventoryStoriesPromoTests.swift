//
//  YaRentInventoryStoriesPromoTests.swift
//  Unit Tests
//
//  Created by Leontyev Saveliy on 05.07.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest
import YREModel
import YREServerDrivenUI
@testable import YREStoriesPromoModule

final class YaRentInventoryStoriesPromoTests: XCTestCase {
    func testOwnerPromo() {
        self.testPromo(userRole: .owner)
    }

    func testTenantPromo() {
        self.testPromo(userRole: .tenant)
    }

    private func testPromo(userRole: YaRentInventoryUserRole, function: String = #function) {
        let hostView = Self.hostView()
        let divPages = Self.divPages(userRole: userRole)

        for slide in 0..<divPages.count {
            hostView.setData(divPages[slide])
            self.assertSnapshot(hostView, function: function + "_slide_\(slide)")
        }
    }
}

extension YaRentInventoryStoriesPromoTests {
    private static func hostView() -> HostView {
        let hostView = HostView()
        hostView.frame = UIScreen.main.bounds
        return hostView
    }

    private static func divPages(userRole: YaRentInventoryUserRole) -> [ViewModel] {
        let interactor = InventoryStoriesPromoInteractor(userRole: userRole)
        let resource = interactor.dataAsset.data.data
        return YREServerDrivenUI.ViewModelGenerator.viewModels(from: resource)
    }
}
