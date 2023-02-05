//
//  UnsubscribeTest.swift
//  YandexMobileMailAutoTests
//
//  Created by Anastasia Kononova on 10/07/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import Foundation
import XCTest

public final class UnsubscribeTest: NetworkTestCase {
    public override var launchArguments: [String] {
        return super.launchArguments + [CommandLineArguments.networkMetricsEventName, NetworkTestCase.UseCases.unsubscribe.rawValue]
    }

    public func testLoadInbox() {
        do {
            let plan = try PredefinedTestSteps.login(user: UsersPool.netTestUser1)
                .then { $0.openFoldersList }
                .then { $0.openSettings }
                .then { $0.openAccountSettings(forEmail: UsersPool.netTestUser1.email) }
                .then { $0.openUnsubscribeSettings }
                .then { $0.loadUnsubscribe }
            try ActionsRunner.performPlan(plan)
        } catch {
            XCTFail("Test failed with error: \(error)")
        }
    }
}
