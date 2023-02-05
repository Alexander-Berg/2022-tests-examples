//
//  PinApplication.swift
//  YandexMobileMailAutoTests
//
//  Created by Artem I. Novikov on 22.05.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class PinApplication: Pin {
    private let generalSettingsPage = GeneralSettingsPage()
    private let pinPage = PinPage()
    
    public func waitForPinToTrigger() throws {
        return
    }
    
    private func isPinActivated() -> Bool {
        return self.generalSettingsPage.pinActivate.value as! String == "1"
    }
    
    public func isLoginUsingPasswordEnabled() -> Bool {
        XCTContext.runActivity(named: "Checking if login using password is enabled") { _ in
            return ApplicationState.shared.settingsState.isLoginUsingPasswordEnabled
        }
    }
    
    public func enterPassword(_ password: String) throws {
        for char in password {
            try self.pinPage.getButton(withValue: char).tapCarefully()
        }
    }
    
    public func resetPassword() throws {
        try self.pinPage.resetPassword.tapCarefully()
        try self.pinPage.alertOKButton.tapCarefully()
        self.dropPasswordInApplicationState()
        ApplicationState.shared.accountState.accounts.removeAll()
    }

    public func turnOnLoginUsingPassword(_ password: String) throws {
        try XCTContext.runActivity(named: "Turning on login using password. Password: \(password)") { _ in
            try self.generalSettingsPage.pin.tapCarefully()
            guard !self.isPinActivated() else {
                throw YSError("Login using password toggle already turned on")
            }
            
            try self.generalSettingsPage.pinActivateSwitcher.tapCarefully()
            try self.enterPassword(password)
            sleep(1)
            try self.enterPassword(password)
            
            self.setPasswordInApplicationState(password)
            try self.generalSettingsPage.backButton.tapCarefully()
        }
    }

    public func turnOffLoginUsingPassword() throws {
        try XCTContext.runActivity(named: "Turning off login using password") { _ in
            try self.generalSettingsPage.pin.tapCarefully()
            guard self.isPinActivated() else {
                throw YSError("Login using password toggle already turned off")
            }
            try self.generalSettingsPage.pinActivateSwitcher.tapCarefully()
            self.dropPasswordInApplicationState()
            sleep(1)
            try self.generalSettingsPage.backButton.tapCarefully()
        }
    }

    public func changePassword(_ newPassword: String) throws {
        try XCTContext.runActivity(named: "Changing password to \(newPassword)") { _ in
            try self.generalSettingsPage.pin.tapCarefully()
            guard self.isPinActivated() else {
                throw YSError("Login using password toggle turned off")
            }
            
            try self.generalSettingsPage.pinChangePassword.tapCarefully()
            
            try self.enterPassword(newPassword)
            sleep(1)
            try self.enterPassword(newPassword)
            try self.generalSettingsPage.backButton.tapCarefully()
        }
    }
    
    private func dropPasswordInApplicationState() {
        ApplicationState.shared.settingsState.isLoginUsingPasswordEnabled = false
        ApplicationState.shared.settingsState.password = ""
    }
    
    private func setPasswordInApplicationState(_ password: String) {
        ApplicationState.shared.settingsState.isLoginUsingPasswordEnabled = true
        ApplicationState.shared.settingsState.password = password
    }
}
