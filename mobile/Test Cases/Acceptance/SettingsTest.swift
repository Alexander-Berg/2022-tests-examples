//
//  Created by Artem I. Novikov on 27/12/2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

import Foundation
import XCTest

public final class SettingsTest: YOTestCase {
    func testSettings() {
        Allure.start(feature: .settings, subFeature: .mainSettings, testPalmCaseID: 883)

        do {
            let plan = try PredefinedTestSteps.login(user: UsersPool.testUser1)
                .then { $0.openFoldersList }
                .then { $0.openSettings }
            try ActionsRunner.performPlan(plan)
        } catch {
            XCTFail("Test failed with error: \(error)")
        }
    }
}
