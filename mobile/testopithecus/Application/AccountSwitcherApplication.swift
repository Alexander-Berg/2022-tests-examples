//
// Created by Elizaveta Y. Voronina on 4/26/20.
// Copyright (c) 2020 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class AccountSwitcherApplication: MultiAccount {

    private let foldersListPage = FoldersListPage()

    public func logoutFromAccount(_ login: Login) throws {
        try self.foldersListPage.avatarButtonsByLogin[login]!.longTap()
        guard let okButton = self.foldersListPage.alertLogoutButtonOK else {
            throw YSError("There is no OK button")
        }
        try okButton.tapCarefully()
    }

    public func getLoggedInAccountsList() throws -> YSArray<Login> {
        try XCTContext.runActivity(named: "Getting Logged In Account List") { _ in
            var accountList = [Login]()
            for account in self.foldersListPage.avatarButtonsByIndex {
                try account.forceTap()
                accountList.append(getCurrentAccount())
            }
            return YSArray(array: accountList)
        }
    }

    public func getCurrentAccount() -> Login {
        XCTContext.runActivity(named: "Getting current account Login") { _ in
            return formatAccountEmail(login: self.foldersListPage.currentAccountEmailLabel.label)
        }
    }

    public func getNumberOfAccounts() -> Int32 {
        XCTContext.runActivity(named: "Getting number of accounts") { _ in
            return self.foldersListPage.accountNumberCount
        }
    }

    public func addNewAccount() throws {
        try XCTContext.runActivity(named: "Adding new account in AccountSwitcher") { _ in
            try self.foldersListPage.addAccountButton.tapCarefully()
        }
    }

    public func switchToAccount(_ login: Login) throws {
        try XCTContext.runActivity(named: "Switching to account with login \(login)") { _ in
            guard let avatarButton = self.foldersListPage.avatarButtonsByLogin[login] else {
                throw YSError("Avatar button with login '\(login)' doesnt exist")
            }
            try avatarButton.forceTap()
        }
    }
}
