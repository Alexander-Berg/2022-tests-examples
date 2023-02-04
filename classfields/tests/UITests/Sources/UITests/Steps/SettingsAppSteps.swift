//
//  SettingsAppStep.swift
//  UITests
//
//  Created by Roman Bevza on 2/18/21.
//
import XCTest
import Snapshots

final class SettingsAppScreen: BaseScreen, Scrollable {
    lazy var scrollableElement: XCUIElement = app.scrollViews.firstMatch
    lazy var appCell = app.tables.cells.staticTexts["Auto.ru"].firstMatch
    lazy var notificationsRuButton = app.buttons.containingText("Уведомления").firstMatch
    lazy var notificationsEnButton = app.buttons.containingText("Notifications").firstMatch
    lazy var notificationsSwitchRu = app.switches.containingText("Допуск уведомлений").firstMatch
    lazy var notificationsSwitchEn = app.switches.containingText("Allow Notifications").firstMatch
}

final class SettingsAppSteps: BaseSteps {

    required init(context: StepsContext, root: XCUIElement? = nil) {
        super.init(context: context, root: root)
        self.app = XCUIApplication(bundleIdentifier: "com.apple.Preferences")
        self.baseScreen = BaseScreen(app)
        self.context = context
    }

    func onSettingScreen() -> SettingsAppScreen {
        return SettingsAppScreen(app)
    }

    func launch() -> Self {
        app.launch()
        return self
    }

    func terminate() -> Self {
        app.terminate()
        return self
    }
    func tapAppCell() -> Self {
        onSettingScreen().appCell.tap()
        return self
    }

    func tapNotifications() -> Self {
        if onSettingScreen().notificationsRuButton.exists {
            onSettingScreen().notificationsRuButton.tap()
        } else if onSettingScreen().notificationsEnButton.exists {
            onSettingScreen().notificationsEnButton.tap()
        }
        return self
    }

    func disableNotificationsSwitch() -> Self {
        if onSettingScreen().notificationsSwitchRu.exists {
            if onSettingScreen().notificationsSwitchRu.value as? String == "1" {
                onSettingScreen().notificationsSwitchRu.tap()
            }
        } else if onSettingScreen().notificationsSwitchEn.exists {
            if onSettingScreen().notificationsSwitchEn.value as? String == "1" {
                onSettingScreen().notificationsSwitchEn.tap()
            }
        }
        return self
    }
}
