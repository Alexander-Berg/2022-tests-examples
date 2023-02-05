//
// Created by Timur Turaev on 2018-12-02.
// Copyright (c) 2018 Yandex. All rights reserved.
//

import Foundation
import XCTest

public final class MaillistTest: YOTestCase {
    func testAuthorization() {
        Allure.start(feature: .maillist, subFeature: .maillistView, testPalmCaseID: 874)
        do {
            let plan = try PredefinedTestSteps.login(user: UsersPool.testUser1)
            try ActionsRunner.performPlan(plan)
        } catch {
            XCTFail("Test failed with error: \(error)")
        }
    }
    
    func testGroupMode() {
        Allure.start(feature: .activeOperationsInMaillist, subFeature: .groupOperations, testPalmCaseID: 1064)

        do {
            let plan = try PredefinedTestSteps.login(user: UsersPool.testUser1)
                .then { $0.tapOnAvatarOfMessage(withIndex: 0) }
            try ActionsRunner.performPlan(plan)
        } catch {
            XCTFail("Test failed with error: \(error)")
        }
    }
    
    func testExitGroupMode() {
        Allure.start(feature: .activeOperationsInMaillist, subFeature: .groupOperations, testPalmCaseID: 1130)

        do {
            let plan = try PredefinedTestSteps.login(user: UsersPool.testUser1)
                .then { $0.tapOnAvatarOfMessage(withIndex: 0) }
                .then { $0.selectMessageInGroupMode(withIndex: 1) }
                .then { $0.selectMessageInGroupMode(withIndex: 2) }
                .then { $0.selectMessageInGroupMode(withIndex: 1) }
                .then { $0.selectMessageInGroupMode(withIndex: 0) }
                .then { $0.selectMessageInGroupMode(withIndex: 2) }
            try ActionsRunner.performPlan(plan)
        } catch {
            XCTFail("Test failed with error: \(error)")
        }
    }
}
