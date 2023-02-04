//
//  RentProviderWizardStepTests.swift
//  Unit Tests
//
//  Created by Alexey Salangin on 16.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import XCTest
@testable import YREUserOfferWizardModule
import YREDesignKit

final class RentProviderWizardStepTests: XCTestCase {
    func testLayout() throws {
        let fakeOutput = FakeOutput()
        let viewController = RentProviderWizardStepViewController(geoSearchObject: nil, output: fakeOutput)
        viewController.view.frame.size = UIScreen.main.bounds.size
        self.assertSnapshot(viewController.view)
    }

    private final class FakeOutput: RentProviderWizardStepViewOutput {
        func rentProviderWizardStepViewOptionTapped(_ option: RentProviderStepItem) {}
        func rentProviderWizardStepViewBackTaped() {}
    }
}
