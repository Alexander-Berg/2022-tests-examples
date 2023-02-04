//
//  NotificationSettingsTests.swift
//  UI Tests
//
//  Created by Timur Guliamov on 28.03.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import Foundation
import YREAppConfig

final class NotificationSettingsTests: BaseTestCase {
    func testOpenSavedSearches() {
        let profileMenu = ProfileMenuSteps()
        let notificationSettings = NotificationSettingsSteps()
        let savedSearchesList = SavedSearchesListSteps()

        NotificationSettingsAPIStubConfigurator.setupUserNotifications(using: self.dynamicStubs)

        self.relaunchApp(with: .profileTests)

        profileMenu
            .screenIsPresented()
            .tapOnNotificationSettings()

        notificationSettings
            .isScreenPresented()
            .enablePushNotificationsIfNeeded()
            .tapOnSavedSearchesCell()
        
        savedSearchesList
            .screenIsPresented()
    }
}
