//
//  BaseTestCase.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 18.01.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREAppConfig
import YRETestsUtils

/// The root of all test cases - you must subclass it in all of your test case classes
class BaseTestCase: XCTestCase {
    let dynamicStubs = HTTPDynamicStubs()
    let communicator = CommunicationAgent()

    override func setUp() {
        super.setUp()
        self.continueAfterFailure = TestConfigHelper.continueAfterFailure()
        self.dynamicStubs.setUp()
        self.communicator.setUp(usingRunningServer: self.dynamicStubs)
        AnalyticsAgent.shared.setUp(using: self.dynamicStubs)

        ImagesStubConfigurator.setupColors(using: self.dynamicStubs)
    }

    override func tearDown() {
        super.tearDown()
        self.dynamicStubs.tearDown()
    }

    /// Use this method to launch/relaunch app with some configuration
    /// For correct work should be called after setting up all necessary backend stubs
    ///
    /// `startWithPromo` must be set to `true` when some startup promo (like PushNotifications) is displayed
    final func relaunchApp(
        with externalAppConfiguration: ExternalAppConfiguration,
        startWithPromo: Bool = false
    ) {
        // TODO: enlist all the configuration into the report
        XCTContext.runActivity(named: "Перезапускаем приложение с заданной конфигурацией") { _ -> Void in
            let app = XCUIApplication()
            app.launchArguments += ["-AppleLanguages", "(ru)"]
            app.launchArguments += ["-AppleLocale", "ru_RU"]
            app.launchEnvironment["ExternalAppConfiguration"] = externalAppConfiguration.asJSON()
            app.launchEnvironment["TestTargetType"] = "ui"
            app.launch()
            // @l-saveliy: Sometimes simulator can freeze for a long time. Wait 30 minutes for sure
            self.waitAppReadyToInteract(timeout: 60 * 30, startWithPromo: startWithPromo)
        }
    }

    @discardableResult
    func compareWithScreenshot(identifier: String, timeout: TimeInterval = 1.0, file: StaticString = #file, line: UInt = #line) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом всего экрана") { _ -> Void in
            let app = XCUIApplication()
            let yre_ignoredEdges = app.yre_ignoredEdges()
            let screenshot = app.yreWaitAndScreenshot(timeout: timeout)
            Snapshot.compareWithSnapshot(
                image: screenshot,
                identifier: identifier,
                ignoreEdges: yre_ignoredEdges,
                file: file,
                line: line
            )
        }
        return self
    }

    final func runActivity(_ named: String, closure: () -> Void) {
        XCTContext.runActivity(named: named) { _ in
            closure()
        }
    }

    // MARK: Private

    private func waitAppReadyToInteract(timeout: TimeInterval, startWithPromo: Bool = false) {
        XCTContext.runActivity(named: "Ожидаем старт взаимодействия с приложением") { _ -> Void in
            let app = XCUIApplication()
            XCTAssertTrue(app.wait(for: .runningForeground, timeout: Constants.appStartTimeout), "App not in foreground state")

            if !startWithPromo {
                XCTContext.runActivity(named: "Проверка доступности приложения для дальнейших пользовательских действий") { _ -> Void in
                    ElementsProvider
                        .obtainNavigationContainer()
                        .yreEnsureExistsWithTimeout(timeout: timeout, message: "Main navigation container doesn't exists")
                }
            }
        }
    }
}
