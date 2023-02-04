//
//  EnablePushTests.swift
//  UITests
//
//  Created by Alexander Malnev on 2/8/21.
//

import XCTest
import Snapshots

class EnablePushTests: BaseTest {
    lazy var mainSteps = MainSteps(context: self)

    override var appSettings: [String: Any] {
        var value = super.appSettings
        value["enablePushReminderCooldownTimestamp"] = nil
        return value
    }

    override func setUp() {
        server.forceLoginMode = .forceLoggedIn

        setupServer(server)

        try! server.start()

        super.setUp()

        launch()
    }

    override func launch() {
        app.launchArguments.append("--UITests")
        app.launch()
        mainSteps.handleSystemAlertIfNeeded(allowButtons: ["Allow While Using App", "Allow"])

        _ = SettingsAppSteps(context: self)
            .launch()
            .tapAppCell()
            .tapNotifications()
            .disableNotificationsSwitch()
            .terminate()

        super.launch()
    }

    func setupServer(_ server: StubServer) {
        server.addHandler("POST /device/hello") { (request, _) -> Response? in
            return Response.okResponse(fileName: "hello_ok", userAuthorized: true)
        }
    }
}
