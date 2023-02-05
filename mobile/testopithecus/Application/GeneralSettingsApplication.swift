//
// Created by Elizaveta Y. Voronina on 12/12/19.
// Copyright (c) 2019 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class GeneralSettingsApplication: GeneralSettings, IosGeneralSettings {
    private let rootSettingsPage = RootSettingsPage()
    private let generalSettingsPage = GeneralSettingsPage()

    public func setActionOnSwipe(_ action: ActionOnSwipe) throws {
        try XCTContext.runActivity(named: "Setting action on swipe: \(action.toString())") { _ in
            try self.generalSettingsPage.swipeAction.tapCarefully()
            switch action {
            case .delete:
                try self.generalSettingsPage.swipeActionDelete.tapCarefully()
            case .archive:
                try self.generalSettingsPage.swipeActionArchive.tapCarefully()
            }
            try self.generalSettingsPage.backButton.tapCarefully()
        }
    }

    public func getActionOnSwipe() throws -> ActionOnSwipe {
        try XCTContext.runActivity(named: "Getting current action on swipe") { _ in
            if self.generalSettingsPage.swipeAction.value as! String == "Delete" {
                return .delete
            } else if self.generalSettingsPage.swipeAction.value as! String == "Archive" {
                return .archive
            } else {
                throw YSError("There is no selected swipe action")
            }
        }
    }

    public func clearCache() throws {
        try XCTContext.runActivity(named: "Clearing cache") { _ in
            try self.generalSettingsPage.deleteCache.tapCarefully()
            self.generalSettingsPage.cacheDeletedLabel.yo_waitForExistence(timeout: 4.0)
        }
    }

    public func openGeneralSettings() throws {
        try XCTContext.runActivity(named: "Opening General settings from Root settings") { _ in
            try self.rootSettingsPage.generalSettingsButton.tapCarefully()
        }
    }

    public func closeGeneralSettings() throws {
        try XCTContext.runActivity(named: "Closing General settings and getting back to Root settings") { _ in
            sleep(1)
            try self.generalSettingsPage.backButton.tapCarefully()
        }
    }

    public func switchIconBadgeForActiveAccount() throws {
        try XCTContext.runActivity(named: "Switching badge for active account") { _ in
            try self.generalSettingsPage.iconBadgeActiveAccountSwitcher.tapCarefully()
        }
    }

    public func isIconBadgeForActiveAccountEnabled() throws -> Bool {
        try XCTContext.runActivity(named: "Checking if active account badge is enabled") { _ in
            try self.generalSettingsPage.iconBadge.tapCarefully()
            let isIconBadgeEnabled = self.generalSettingsPage.iconBadgeActiveAccount.value as! String == "1"
            try self.generalSettingsPage.backButton.tapCarefully()
            return isIconBadgeEnabled
        }
    }

    public func isLoginUsingPasswordEnabled() -> Bool {
        XCTContext.runActivity(named: "Checking if login using password is enabled") { _ in
            return self.generalSettingsPage.pin.value as! String == "On"
        }
    }

    public func switchCompactMode() throws {
        try XCTContext.runActivity(named: "Switching compact mode") { _ in
            try self.generalSettingsPage.compactModeSwitcher.tapCarefully()
            ApplicationState.shared.settingsState.isCompactModeEnabled.toggle()
        }
    }

    public func isCompactModeEnabled() -> Bool {
        XCTContext.runActivity(named: "Checking if compact mode is enabled") { _ in
            return self.generalSettingsPage.compactMode.value as! String == "1"
        }
    }

    public func switchSystemThemeSync() throws {
        try XCTContext.runActivity(named: "Switching system theme sync") { _ in
            try self.generalSettingsPage.syncThemeSwitcher.tapCarefully()
        }
    }

    public func isSystemThemeSyncEnabled() -> Bool {
        XCTContext.runActivity(named: "Checking if system theme sync is enabled") { _ in
            return self.generalSettingsPage.syncTheme.value as! String == "1"
        }
    }

    public func switchDarkTheme() throws {
        try XCTContext.runActivity(named: "Switching dark theme sync") { _ in
            try self.generalSettingsPage.darkThemeSwitcher.tapCarefully()
        }
    }

    public func isDarkThemeEnabled() -> Bool {
        XCTContext.runActivity(named: "Checking if dark theme is enabled") { _ in
            if !self.isSystemThemeSyncEnabled() {
                return self.generalSettingsPage.darkTheme.value as! String == "1"
            } else {
                YOXCTLogMessage("Device theme sync is turned on")
                return false
            }
        }
    }

    public func openLinksIn(_ browser: Browser) throws {
        try XCTContext.runActivity(named: "Setting \(browser.toString()) browser for opening links") { _ in
            switch browser {
            case .yandexBrowser:
                try self.generalSettingsPage.yandexBrowser.tapCarefully()
            case .safari:
                try self.generalSettingsPage.mobileSafari.tapCarefully()
            case .builtIn:
                try self.generalSettingsPage.embeddedSafari.tapCarefully()
            }
        }
    }

    public func getSelectedBrowser() throws -> Browser {
        try XCTContext.runActivity(named: "Getting browser for opening links") { _ in
            if self.generalSettingsPage.yandexBrowser.exists && self.generalSettingsPage.yandexBrowser.isSelected {
                return .yandexBrowser
            } else if self.generalSettingsPage.mobileSafari.isSelected {
                return .safari
            } else if self.generalSettingsPage.embeddedSafari.isSelected {
                return .builtIn
            } else {
                throw YSError("There is no selected browser")
            }
        }
    }

    public func switchVoiceControl() throws {
        try XCTContext.runActivity(named: "Switching voice control") { _ in
            try self.generalSettingsPage.voiceControlSwitcher.tapCarefully()
        }
    }

    public func isVoiceControlEnabled() -> Bool {
        XCTContext.runActivity(named: "Checking if voice control is enabled") { _ in
            return self.generalSettingsPage.voiceControl.value as! String == "1"
        }
    }

    public func setVoiceControlLanguage(_ language: Language) throws {
        try XCTContext.runActivity(named: "Setting \(language.toString()) language for voice control") { _ in
            try self.generalSettingsPage.voiceControlLanguage.tapCarefully()

            switch language {
            case .russian:
                try self.generalSettingsPage.voiceControlLanguageRussian.tapCarefully()
            case .english:
                try self.generalSettingsPage.voiceControlLanguageEnglish.tapCarefully()
            case .ukrainian:
                try self.generalSettingsPage.voiceControlLanguageUkrainian.tapCarefully()
            case .turkish:
                try self.generalSettingsPage.voiceControlLanguageTurkish.tapCarefully()
            }
        }
    }

    public func getVoiceControlLanguage() throws -> Language {
        try XCTContext.runActivity(named: "Getting voice control language") { _ in
            if self.generalSettingsPage.voiceControlLanguage.value as! String == "Russian" {
                return .russian
            } else if self.generalSettingsPage.voiceControlLanguage.value as! String == "English" {
                return .english
            } else if self.generalSettingsPage.voiceControlLanguage.value as! String == "Ukrainian" {
                return .ukrainian
            } else if self.generalSettingsPage.voiceControlLanguage.value as! String == "Turkish" {
                return .turkish
            } else {
                throw YSError("There is no selected language")
            }
        }
    }

    public func switchSmartReplies() throws {
        try XCTContext.runActivity(named: "Switching smart replies") { _ in
            try self.generalSettingsPage.smartRepliesSwitcher.tapCarefully()
        }
    }

    public func isSmartRepliesEnabled() -> Bool {
        XCTContext.runActivity(named: "Checking if smart replies is enabled") { _ in
            return self.generalSettingsPage.smartReplies.value as! String == "1"
        }
    }

    public func setCancelSendingEmail(_ option: CancelSendingOption) throws {
        try XCTContext.runActivity(named: "Setting cancel sending option: \(option.toString())") { _ in
            try self.generalSettingsPage.cancelSending.tapCarefully()

            switch option {
            case .turnOff:
                try self.generalSettingsPage.cancelSendingTurnOff.tapCarefully()
            case .threeSeconds:
                try self.generalSettingsPage.cancelSendingThreeSeconds.tapCarefully()
            case .fiveSeconds:
                try self.generalSettingsPage.cancelSendingFiveSeconds.tapCarefully()
            case .tenSeconds:
                try self.generalSettingsPage.cancelSendingTenSeconds.tapCarefully()
            }
        }
    }

    public func getCancelSendingEmail() throws -> CancelSendingOption {
        try XCTContext.runActivity(named: "Getting cancel sending option") { _ in
        if self.generalSettingsPage.cancelSending.value as! String == "Off" {
                return .turnOff
            } else if self.generalSettingsPage.cancelSending.value as! String == "3 sec" {
                return .threeSeconds
            } else if self.generalSettingsPage.cancelSending.value as! String == "5 sec" {
                return .fiveSeconds
            } else if self.generalSettingsPage.cancelSending.value as! String == "10 sec" {
                return .tenSeconds
            } else {
                throw YSError("There is no selected cancel sending option")
            }
        }
    }
}
