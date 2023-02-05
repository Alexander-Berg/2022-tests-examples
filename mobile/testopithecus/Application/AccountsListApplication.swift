//
// Created by Elizaveta Y. Voronina on 6/10/20.
// Copyright (c) 2020 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class AccountsListApplication: AccountsList {

    private let accountsListPage = AccountsListPage()

    public func choseAccountFromAccountsList(_ account: UserAccount) throws {
        try XCTContext.runActivity(named: "Choosing account with login=\(account.login) from accounts list ") { _ in
            try self.accountsListPage.getAccountButton(account.login).tapCarefully()
        }
    }

    public func getAccountsList() -> YSArray<UserAccount> {
        YOXCTFail("Method has not been implemented yet")
    }
}
