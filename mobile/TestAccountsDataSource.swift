//
//  TestAccountsDataSource.swift
//  YandexMobileMail
//
//  Created by Karim Amanov on 01/08/2017.
//  Copyright © 2017 Yandex. All rights reserved.
//

import UIKit

final class AccountsDataSourceMock: NSObject, YOAccountsDataSource {
    typealias AccountMockInfo = (login: String, uid: NSNumber, valid: Bool)
    var accounts: [AccountMockInfo] =  []
    var activeLoginIndex = 0
    var setActiveAccountLoginBlock: ((String) -> Void)?
    var mailServiceForLogin: [String: YOMailService] = [:]
    
    var numberOfAccounts: UInt {
        return numericCast(accounts.count)
    }
    
    func login(at index: UInt) -> String {
        return accounts[numericCast(index)].login
    }
    
    func login(withUID UID: NSNumber) -> String? {
        return self.accounts.first(where: { UID.isEqual(to: $0.uid) })?.login
    }
    
    func uid(withLogin login: String) -> NSNumber? {
        return self.accounts.first(where: { login == $0.login })?.uid
    }
    
    func mailService(forLogin login: String) -> YOMailService {
        return self.mailServiceForLogin[login] ?? .yandex
    }

    var storedAccountLogins: [String] {
        return self.accounts.compactMap { $0.valid ? $0.login : nil }
    }
    
    var validAccountLogins: [String] {
        return self.accounts.compactMap { $0.valid ? $0.login : nil }
    }
    
    func isValid(at index: UInt) -> Bool {
        return self.accounts[numericCast(index)].valid
    }

    func isLoginExistsAndExpired(_ login: String) -> Bool {
        guard let account = self.accounts.first(where: { login == $0.login }) else {
            return false
        }
        return !account.valid
    }
    
    var activeAccountLogin: String? {
        return self.accounts[activeLoginIndex].login
    }
    
    var activeAccountIndex: UInt {
        return numericCast(activeLoginIndex)
    }
    
    func setActiveAccountLogin(_ login: String, isNewAccount: Bool) -> Bool {
        self.activeLoginIndex = self.accounts.firstIndex(where: { $0.login == login }) ?? 0
        self.setActiveAccountLoginBlock?(login)
        return true
    }

    func updateUserInfo(forLogin login: String, completionBlock: ((Bool) -> Void)?) {
        completionBlock?(true)
    }
}
