//
//  AccountManagerWrapperMock.swift
//  YandexDisk
//
//  Created by Mariya Kachalova on 06/12/2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

@testable import YandexDisk

final class AccountManagerWrapperMock: AccountManagerWrapper {
    var testIsAuthorized = false
    private(set) var handleUrl: URL?

    override var currentAccount: YALAccount? {
        guard testIsAuthorized else { return nil }
        let account = YALMutableAccount()
        account.token = "token"
        return account
    }

    override func handleOpen(
        _ url: URL,
        withApplication _: UIApplication,
        sourceApplication _: String?,
        annotation _: Any?,
        viewController _: UIViewController?
    ) -> Bool {
        handleUrl = url
        return true
    }
}
