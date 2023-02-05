//
// Created by Fedor Amosov on 08/11/2019.
// Copyright (c) 2019 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class YandexLoginApplication: YandexLogin {
    public func loginWithYandexAccount(_ account: UserAccount) throws {
        try XCTContext.runActivity(named: "Log in with Yandex account \(account.login)") { _ in
            let userCredentials = UserCredentials(login: account.login, password: account.password)
            let user = User(credentials: userCredentials, inboxUnreadCounter: -1, name: account.login, email: account.login, mailService: .yandex, allMessages: -1)
            let plan = try PredefinedTestSteps.login(user: user)
            try ActionsRunner.performPlan(plan, in: TestContext.startContext.noValidate)
        }
    }
}
