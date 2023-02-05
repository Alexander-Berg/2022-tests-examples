//
// Created by Fariza Zhiyenbayeva on 2019-01-30.
// Copyright (c) 2019 Yandex. All rights reserved.
//

import Foundation
import XCTest

public final class GeneralSettingsTest: YOTestCase {
    func testGeneralSettings() {
        Allure.start(feature: .settings, subFeature: .mainSettings, testPalmCaseID: 1370)

        do {
            let plan = try PredefinedTestSteps.login(user: UsersPool.testUser1)
                .then { $0.openFoldersList }
                .then { $0.openSettings }
                .then { $0.openGeneralSettings }
            try ActionsRunner.performPlan(plan)
        } catch {
            XCTFail("Test failed with error: \(error)")
        }
    }
}
