//
// Created by Timur Turaev on 2018-11-29.
// Copyright (c) 2018 Yandex. All rights reserved.
//

import XCTest

public final class AuthorizationTest: YOTestCase {
    func testAuthorization() {
        Allure.start(feature: .authorizationAndStartWizard, subFeature: .accountSwitcher, testPalmCaseID: 666)
        do {
            let plan = try PredefinedTestSteps.login(user: UsersPool.testUser1)
                .then { $0.openFoldersList }
            try ActionsRunner.performPlan(plan)
        } catch {
            XCTFail("Test failed with error: \(error)")
        }
    }
    
    func testAuthorizationViaGimapSimpleForm() {
        Allure.start(feature: .authorizationAndStartWizard, subFeature: .accountSwitcher, testPalmCaseID: 2822)

        do {
            let plan = try PredefinedTestSteps.login(user: UsersPool.testUserZoho)
                .then { $0.openFoldersList }
            try ActionsRunner.performPlan(plan)
        } catch {
            XCTFail("Test failed with error: \(error)")
        }
    }

    func testSWCSaving() {
        Allure.start(feature: .sharedWebCredentials, testPalmCaseID: 2334)

        do {
            let plan = try PredefinedTestSteps.login(user: UsersPool.testUser1, savePassword: true)
                .then { $0.openFoldersList }
                .then { $0.openAccountManager }
                .then { $0.openYandexLogin }
                .thenAlert { $0.dismiss() }
            try ActionsRunner.performPlan(plan)
        } catch {
            XCTFail("Test failed with error: \(error)")
        }
    }
}
