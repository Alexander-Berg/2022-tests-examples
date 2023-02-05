//
// Created by Elizaveta Y. Voronina on 12/12/19.
// Copyright (c) 2019 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class AccountSettingsApplication: AccountSettings, IosAccountSettings {
    private let rootSettingsPage = RootSettingsPage()
    private let accountSettingsPage = AccountSettingsPage()
    private let filterListPage = FilterListPage()

    public func openAccountSettings(_ accountIndex: Int32) throws {
        try XCTContext.runActivity(named: "Opening account settings for account with index \(accountIndex)") { _ in
            try self.rootSettingsPage.personalSettingsButton(byIndex: Int(accountIndex)).tapCarefully()
        }
    }

    public func closeAccountSettings() throws {
        try XCTContext.runActivity(named: "Closing account settings and going to Root settings") { _ in
            sleep(1)
            try self.accountSettingsPage.backButton.tapCarefully()
        }
    }

    public func getThreadingSetting() -> Bool {
        XCTContext.runActivity(named: "Checking if thread mode in on") { _ in
            return self.accountSettingsPage.threadMode.value as! String == "1"
        }
    }

    public func switchGroupBySubject() throws {
        try XCTContext.runActivity(named: "Toggling thread mode setting") { _ in
            try self.accountSettingsPage.threadModeSwitcher.tapCarefully()
        }
    }
    
    public func getFolderToNotificationOption() throws -> YSMap<FolderName, NotificationOption> {
        let folderToNotificationOption: YSMap<FolderName, NotificationOption> = YSMap()
        try self.accountSettingsPage.folderList.forEach { folderName in
            try folderToNotificationOption.set(folderName, self.getNotificationOptionForFolder(folderName))
        }
        folderToNotificationOption.set(DefaultFolderName.inbox, try self.getNotificationOptionForFolder(DefaultFolderName.inbox))
        return folderToNotificationOption
    }
    
    public func isGroupBySubjectEnabled() -> Bool {
        XCTContext.runActivity(named: "Checking if group by subject is enabled") { _ in
            return self.accountSettingsPage.threadMode.value as! String == "1"
        }
    }
    
    private func isSortingEmailsByCategoryExists() -> Bool {
        XCTContext.runActivity(named: "Checking if sorting emails by category exists") { _ in
            if self.accountSettingsPage.sortEmailsByCategory.exists {
                return self.accountSettingsPage.sortEmailsByCategory.value as! String == "1"
            } else {
                return false
            }
        }
    }
    
    public func switchSortingEmailsByCategory() throws {
        try XCTContext.runActivity(named: "Switching sorting emails by category setting") { _ in
            if self.isSortingEmailsByCategoryExists() {
                try self.accountSettingsPage.sortEmailsByCategorySwitcher.tapCarefully()
            } else {
                YOXCTLogMessage("Tabs experiment isn't apllied")
            }
        }
    }
    
    public func isSortingEmailsByCategoryEnabled() -> Bool {
        XCTContext.runActivity(named: "Checking if sorting emails by category is enabled") { _ in
            if self.isSortingEmailsByCategoryExists() {
                return self.accountSettingsPage.sortEmailsByCategory.value as! String == "1"
            } else {
                return false
            }
        }
    }
    
    public func openMailingListsManager() throws {
        try XCTContext.runActivity(named: "Openning Mailing lists manager") { _ in
            try self.accountSettingsPage.unsubscribe.tapCarefully()
        }
    }
    
    public func openFilters() throws {
        try XCTContext.runActivity(named: "Openning Filters") { _ in
            try self.accountSettingsPage.filters.tapCarefully()
            self.filterListPage.tableView.yo_waitForExistence()
        }
    }
    
    public func getSignature() throws -> String {
        try XCTContext.runActivity(named: "Getting signature") { _ in
            /* In the self.accountSettingsPage.signature.value the line
             separator is changed from "\n" to " ", so we have to do it this way */
            try self.accountSettingsPage.signature.tapCarefully()
            let signature = self.accountSettingsPage.signatureTextView.value as! String
            try self.accountSettingsPage.backButton.tapCarefully()
            return signature
        }
    }
    
    public func changeSignature(_ newSignature: String) throws {
        try XCTContext.runActivity(named: "Changing signature to \(newSignature)") { _ in
            try self.accountSettingsPage.signature.tapCarefully()
            self.accountSettingsPage.signatureTextView.clearField()
            self.accountSettingsPage.signatureTextView.typeText(newSignature)
            try self.accountSettingsPage.backButton.tapCarefully()
        }
    }
    
    public func changePhoneNumber(_ newPhoneNumber: String) {
        XCTContext.runActivity(named: "Changing phone number to \(newPhoneNumber)") { _ in
            YOXCTFail("Method is not implemented")
        }
    }
    
    public func switchTheme() throws {
        try XCTContext.runActivity(named: "Switching theme setting") { _ in
            try self.accountSettingsPage.themesSwitcher.tapCarefully()
        }
    }
    
    public func isThemeEnabled() -> Bool {
        XCTContext.runActivity(named: "Checking if theme is enabled") { _ in
            return self.accountSettingsPage.themes.value as! String == "1"
        }
    }
    
    public func switchPushNotification() throws {
        try XCTContext.runActivity(named: "Switching push notification setting") { _ in
            try self.accountSettingsPage.pushNotificationsSwitcher.tapCarefully()
        }
    }
    
    public func isPushNotificationForAllEnabled() -> Bool {
        XCTContext.runActivity(named: "Checking if push notification is enabled") { _ in
            return self.accountSettingsPage.pushNotifications.value as! String == "1"
        }
    }
    
    public func getPushNotificationSound() throws -> NotificationSound {
        try XCTContext.runActivity(named: "Getting push notification sound") { _ in
            switch self.accountSettingsPage.pushSound.value as! String {
            case "Yandex Mail":
                return .yandexMail
            case "Standard":
                return .standard
            default:
                throw YSError("Unknown push notification sound")
            }
        }
    }
    
    public func setPushNotificationSound(_ sound: NotificationSound) throws {
        try XCTContext.runActivity(named: "Setting push notification sound. Sound: \(sound)") { _ in
            try self.accountSettingsPage.pushSound.tapCarefully()
            switch sound {
            case .yandexMail:
                try self.accountSettingsPage.pushSoundYandexMail.tapCarefully()
            case .standard:
                try self.accountSettingsPage.pushSoundStandard.tapCarefully()
            }
            try self.accountSettingsPage.backButton.tapCarefully()
        }
    }
    
    private func isPushNotificationEnabled(forFolder folder: FolderName) throws -> Bool {
        switch folder {
        case DefaultFolderName.inbox:
            return self.accountSettingsPage.pushNotificationsForInbox.value as! String == "1"
        default:
            return self.accountSettingsPage.pushNotificationForUserFolder(byName: folder).value as! String == "1"
        }
    }
    
    public func setNotificationOptionForFolder(_ folder: FolderName, _ option: NotificationOption) throws {
        try XCTContext.runActivity(named: "Setting notification option \(option) for folder \(folder)") { _ in
            switch option {
            case .syncAndNotifyMe:
                if try self.isPushNotificationEnabled(forFolder: folder) {
                    throw YSError("Notification for folder already enabled")
                }
            case .doNotSync:
                if !(try self.isPushNotificationEnabled(forFolder: folder)) {
                    throw YSError("Notification for folder already disabled")
                }
            case .syncWithoutNotification:
                throw YSError("There is no option syncWithoutNotification for push notification")
            }
            
            folder == DefaultFolderName.inbox
                ? try self.accountSettingsPage.pushNotificationsSwitcherForInbox.tapCarefully()
                : try self.accountSettingsPage.pushNotificationSwitcherForUserFolder(byName: folder).tapCarefully()
        }
    }
    
    public func getNotificationOptionForFolder(_ folder: FolderName) throws -> NotificationOption {
        try XCTContext.runActivity(named: "Getting notification for folder \(folder)") { _ in
            let value = folder == DefaultFolderName.inbox
                ? self.accountSettingsPage.pushNotificationsForInbox.value as! String
                : self.accountSettingsPage.pushNotificationForUserFolder(byName: folder).value as! String
            switch value {
            case "1":
                return .syncAndNotifyMe
            case "0":
                return .doNotSync
            default:
                throw YSError("Unknown NotificationOption value")
            }
        }
    }
}
