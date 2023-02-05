//
//  ComposeTest.swift
//  YandexMobileMailAutoTests
//
//  Created by Anastasia Kononova on 09/07/2019.
//  Copyright © 2019 Yandex. All rights reserved.
//

import Foundation
import XCTest

public final class SendMessageComposeTest: NetworkTestCase {
    public override var launchArguments: [String] {
        return super.launchArguments + [CommandLineArguments.networkMetricsEventName, NetworkTestCase.UseCases.sendMessage.rawValue]
    }
    
    func testSendMessage() {
        do {
            let plan = try PredefinedTestSteps.login(user: UsersPool.netTestUser1)
                .then { $0.openCompose }
                .then { $0.sendMessage(to: "iosmailautotestnet02@yandex.ru", subject: "test", body: "test") }
            try ActionsRunner.performPlan(plan)
        } catch {
            XCTFail("Test failed with error: \(error)")
        }
    }
}

public final class AttachUploadingComposeTest: NetworkTestCase {
    public override var launchArguments: [String] {
        return super.launchArguments + [CommandLineArguments.networkMetricsEventName, NetworkTestCase.UseCases.attachmentUploading.rawValue]
    }

    func testAttachUploading() {
        do {
            let plan = try PredefinedTestSteps.login(user: UsersPool.netTestUser1)
                .then { $0.openCompose }
                .then { $0.attachFile }
                .thenAlertMaybe { $0.dismiss(accepting: true) }
                .then { $0.selectAttachment }
            try ActionsRunner.performPlan(plan)
        } catch {
            XCTFail("Test failed with error: \(error)")
        }
    }
}
