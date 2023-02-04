//
//  DeeplinkSteps.swift
//  UITests
//
//  Created by Pavel Zhuravlev on 18.01.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

/// By using this class you can simulate universal deeplink opening via the `Message App`.
/// However at this moment this class is not recommended to use by the following reasons:
/// – the `Message App` has breaking changes with almost each major iOS release;
/// – this approach depends on network connection and apple-app-site-association files content on each related web host;
///
/// For now the recommended approach is to use `CommunicationAgent` which allows to trigger `openURL` method inside the app.
final class DeeplinkSteps {
    @discardableResult
    func open(_ deeplink: String, shouldWaitInBackground: Bool = true) -> Self {
        let expectedAppTitle = "Недвижимость"

        let app = XCUIApplication()
        let messageApp = MessageApp.launch()

        MessageApp.open(URLString: deeplink, inMessageApp: messageApp, shouldWaitInBackground: shouldWaitInBackground)

        app.yreEnsureExistsWithTimeout()
           .yreEnsureLabelEqual(to: expectedAppTitle, message: "Could not open application with \(deeplink)")
        XCTAssertTrue(app.wait(for: .runningForeground, timeout: Constants.timeout), "App not in foreground state")

        return self
    }
}
