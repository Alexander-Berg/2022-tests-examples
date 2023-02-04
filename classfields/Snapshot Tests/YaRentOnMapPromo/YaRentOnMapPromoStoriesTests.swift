//
//  YaRentOnMapPromoStoriesTests.swift
//  Unit Tests
//
//  Created by Maksim Zenkov on 13.07.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import UIKit
import XCTest
@testable import YREStoriesPromoModule
import YREServerDrivenUI
import YREModel
import YREDesignKit
import YREStories

final class YaRentOnMapPromoStoriesTests: XCTestCase {
    
    func testYaRentOnMapPromo() {
        self.testPromo()
    }
    
    private func testPromo(function: String = #function) {
        let hostView = Self.hostView()
        let divPages = Self.divPages()
        
        for slide in 0..<divPages.count {
            hostView.setData(divPages[slide])
            self.assertSnapshot(hostView, function: function + "_slide_\(slide)")
        }
    }
}

extension YaRentOnMapPromoStoriesTests {
    private static func hostView() -> HostView {
        let hostView = HostView()
        hostView.frame = UIScreen.main.bounds
        return hostView
    }
    
    private static func divPages() -> [ViewModel] {
        let interactor = YaRentOnMapStoriesPromoInteractor()
        let resource = interactor.dataAsset.data.data
        return YREServerDrivenUI.ViewModelGenerator.viewModels(from: resource)
    }
}
