//
//  AuthStateReaderMock.swift
//  YREServiceLayer-Unit-Tests
//
//  Created by Pavel Zhuravlev on 25.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import protocol YREAppState.YREAuthStateReader

final class AuthStateReaderMock: NSObject, YREAuthStateReader {
    var login: String? = nil
    var privateKey: String = "mock"
    var isAuthorized: Bool = false
    var autologinAttempted: Bool = false
    var token: String? = "testToken"
    var uuid: String? = "testUUID"
}
