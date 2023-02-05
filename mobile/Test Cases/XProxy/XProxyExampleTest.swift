//
// Created by Fedor Amosov on 10.12.2018.
// Copyright (c) 2018 Yandex. All rights reserved.
//

import XCTest

public final class XProxyExampleTest: XProxyTestCase {
    override var configuration: XProxyConfiguration {
        return .empty
    }

    func testExample() {
        do {
            let plan = try PredefinedTestSteps.login(user: UsersPool.testUser1)
            try ActionsRunner.performPlan(plan)
        } catch {
            XCTFail("Test failed with error: \(error)")
        }
    }
}
