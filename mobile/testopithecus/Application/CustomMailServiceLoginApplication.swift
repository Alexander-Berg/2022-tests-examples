//
// Created by Elizaveta Y. Voronina on 6/15/20.
// Copyright (c) 2020 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class CustomMailServiceLoginApplication: CustomMailServiceLogin {

    var imapLoginPage = IMAPLoginPage()

    public func loginWithCustomMailServiceAccount(_ account: UserAccount) throws {
        try XCTContext.runActivity(named: "Log in with External IMAP account \(account.login)") { _ in
            let userCredentials = UserCredentials(login: account.login, password: account.password)
            let user = User(credentials: userCredentials, inboxUnreadCounter: -1, name: account.login, email: account.login, mailService: .IMAP, allMessages: -1)
            let plan = try PredefinedTestSteps.login(user: user)
            try ActionsRunner.performPlan(plan, in: TestContext.startContext.noValidate)
        }
    }
}
