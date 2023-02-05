//
//  MessagesTest.swift
//  YandexMobileMailAutoTests
//
//  Created by Anastasia Kononova on 09/07/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import Foundation
import XCTest

public final class PTRMessagesTest: NetworkTestCase {
    public override var launchArguments: [String] {
        return super.launchArguments + [CommandLineArguments.networkMetricsEventName, NetworkTestCase.UseCases.pullRoRefresh.rawValue]
    }

    func testPullToRefreshNetworkMetrics() {
        do {
            let plan = try PredefinedTestSteps.login(user: UsersPool.netTestUser1)
                .then { $0.pullToRefresh }
            try ActionsRunner.performPlan(plan)
        } catch {
            XCTFail("Test failed with error: \(error)")
        }
    }
}

public final class LoadMoreMessagesTest: NetworkTestCase {
    public override var launchArguments: [String] {
        return super.launchArguments + [CommandLineArguments.networkMetricsEventName, NetworkTestCase.UseCases.loadMore.rawValue]
    }

    func testLoadMoreNetworkMetrics() {
        do {
            let plan = try PredefinedTestSteps.login(user: UsersPool.netTestUser1)
                .then { $0.loadMore }
            try ActionsRunner.performPlan(plan)
        } catch {
            XCTFail("Test failed with error: \(error)")
        }
    }
}
