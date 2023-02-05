//
// Created by Timur Turaev on 2018-10-07.
// Copyright (c) 2018 Yandex. All rights reserved.
//

import Foundation
import XCTest

public class YOTestCase: XCTestCase {
    // swiftlint:disable:next test_case_accessibility
    public private(set) var application: XCUIApplication!

    // swiftlint:disable:next test_case_accessibility
    public var launchArguments: [String] {
        return []
    }

    // swiftlint:disable:next test_case_accessibility
    public var shouldDeleteAndCleanApplication: Bool {
        return true
    }

    public override func setUpWithError() throws {
        try super.setUpWithError()
        
        self.continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
        XCUIApplication.yandexMobileMail.terminate()
        XCUIApplication.springBoard.activate()

        if self.shouldDeleteAndCleanApplication {
            ApplicationState.shared.general.applicationInstalled = SpringboardPage().applicationIcon.exists

            try ActionsRunner.performPlan(PredefinedTestSteps.deleteApplication,
                                          in: TestContext.startContext.replacingComponent(to: SpringboardComponent()))
        }

        self.application = XCUIApplication.yandexMobileMail
        var launchArguments = self.launchArguments + [CommandLineArguments.autoTestsRunningKey]
        if self.shouldDeleteAndCleanApplication {
            launchArguments.append(CommandLineArguments.cleanApplicationKey)
        }
        launchArguments.append(contentsOf: AutoTestState.shared.savedToSWCAccounts.flatMap { [CommandLineArguments.savedToSWCAccount] + [$0] })

        self.application.launchArguments = launchArguments
        self.application.launch()
        ApplicationState.shared.general.applicationInstalled = true
    }

    public override func tearDown() {
        super.tearDown()
        XCUIApplication.yandexMobileMail.terminate()
        self.application = nil
    }
}
