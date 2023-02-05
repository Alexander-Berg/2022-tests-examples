//
//  FoldersTest.swift
//  YandexMobileMailAutoTests
//
//  Created by Anastasia Kononova on 10/07/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import Foundation
import XCTest

public final class LoadInboxFoldersTest: NetworkTestCase {
    public override var launchArguments: [String] {
        return super.launchArguments + [CommandLineArguments.networkMetricsEventName, NetworkTestCase.UseCases.loadInbox.rawValue]
    }
    
    public func testLoadInbox() {
        do {
            let plan = try PredefinedTestSteps.login(user: UsersPool.netTestUser1)
            try ActionsRunner.performPlan(plan)
        } catch {
            XCTFail("Test failed with error: \(error)")
        }
    }
}

public final class LoadFolderFoldersTest: NetworkTestCase {
    public override var launchArguments: [String] {
        return super.launchArguments + [CommandLineArguments.networkMetricsEventName, NetworkTestCase.UseCases.loadFolder.rawValue]
    }

    public func testUnloadedFolder() {
        do {
            let plan = try PredefinedTestSteps.login(user: UsersPool.netTestUser1)
                .then { $0.openFoldersList }
                .then { $0.openUnread }
            try ActionsRunner.performPlan(plan)
        } catch {
            XCTFail("Test failed with error: \(error)")
        }
    }
}
