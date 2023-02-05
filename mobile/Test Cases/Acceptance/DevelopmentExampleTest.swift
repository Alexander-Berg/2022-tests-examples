//
//  Created by Timur Turaev on 07.10.2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

import XCTest

public final class DevelopmentExampleTest: YOTestCase {
    public func test_Example1() {
        Allure.start(feature: .authorizationAndStartWizard, testPalmCaseID: 0)

        do {
            let plan = try PredefinedTestSteps.login(user: UsersPool.testUser1)
                .then { $0.openFoldersList }
                .then { $0.openAccountManager }
                .then { $0.openYandexLogin }
                .assertNoAlert()

            let context = try ActionsRunner.performPlan(plan)

            try ActionsRunner.performPlan(RandomStepStrategy(maximumStepsCount: 20), in: context)
        } catch {
            XCTFail("Test failed with error: \(error)")
        }
    }
}
