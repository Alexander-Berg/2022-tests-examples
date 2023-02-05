//
//  Created by Artem I. Novikov on 16/01/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import Foundation
import XCTest

public final class AboutTest: YOTestCase {
    func testApplicationVersion() {
        Allure.start(feature: .settings, subFeature: .mainSettings, testPalmCaseID: 1069)

        do {
            let plan = try PredefinedTestSteps.login(user: UsersPool.testUser1)
                .then { $0.openFoldersList }
                .then { $0.openSettings }
                .then { $0.openAbout }
            try ActionsRunner.performPlan(plan)
        } catch {
            XCTFail("Test failed with error: \(error)")
        }
    }
}
