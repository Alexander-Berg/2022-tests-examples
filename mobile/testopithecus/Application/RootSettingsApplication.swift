//
// Created by Elizaveta Y. Voronina on 12/12/19.
// Copyright (c) 2019 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class RootSettingsApplication: RootSettings, IOSRootSettings {
    private let messageListPage = MessageListPage()
    private let foldersListPage = FoldersListPage()
    private let rootSettingsPage = RootSettingsPage()

    public func openRootSettings() throws {
        XCTContext.runActivity(named: "Opening Root settings") { _ in
            self.foldersListPage.settingsButton.tap()
        }
    }

    public func closeRootSettings() throws {
        try XCTContext.runActivity(named: "Closing Root settings") { _ in
            if UIDevice.isIpad() {
                try self.rootSettingsPage.closeButton.tapCarefully()
                try FolderNavigatorApplication().openFolderList()
            } else {
                try self.rootSettingsPage.burgerButton.tapCarefully()
            }
        }
    }

    public func isAboutCellExists() throws -> Bool {
        XCTContext.runActivity(named: "Checking is about cell exists") { _ in
            self.rootSettingsPage.aboutButton.exists
        }
    }

    public func isHelpAndFeedbackCellExists() throws -> Bool {
        XCTContext.runActivity(named: "Checking is help and feedback cell exists") { _ in
            self.rootSettingsPage.feedbackButton.exists
        }
    }

    public func getAccounts() throws -> YSArray<String> {
        XCTContext.runActivity(named: "Getting accounts list") { _ in
            return YSArray(array: self.rootSettingsPage.loginsList)
        }
    }

    public func getTitle() throws -> String {
        XCTContext.runActivity(named: "Getting title") { _ in
            return self.rootSettingsPage.titleView.label
        }
    }

    public func isGeneralSettingsCellExists() throws -> Bool {
        XCTContext.runActivity(named: "Checking is general settings cell exists") { _ in
            self.rootSettingsPage.generalSettingsButton.exists
        }
    }
}
