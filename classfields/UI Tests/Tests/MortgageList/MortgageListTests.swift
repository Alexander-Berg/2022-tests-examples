//
//  MortgageListTests.swift
//  UI Tests
//
//  Created by Fedor Solovev on 29.03.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig

final class MortgageListTests: BaseTestCase {
    func testOpensMortgageList() {
        MortgageListAPIStubConfigurator.setupMortgageProgramSearch(using: self.dynamicStubs)

        self.performCommonTests { mortgageListSteps in
            mortgageListSteps
                .isListNonEmpty()
                .tapOnSubmitButton()
        }
    }

    typealias StubKind = MortgageListAPIStubConfigurator.StubKind

    private func performCommonTests(specificTests: (MortgageListSteps) -> Void) {
        let appConfiguration = ExternalAppConfiguration.commonUITests
        appConfiguration.selectedTabItem = .home
        self.relaunchApp(with: appConfiguration)

        InAppServicesSteps()
            .isScreenPresented()
            .isContentPresented()
            .tapOnMortgageListCell()

        let mortgageListSteps = MortgageListSteps()
        specificTests(mortgageListSteps)
    }
}
