//
//  BaseConnectionManagerMock.swift
//  10.02.2022
//

import Foundation
@testable import YandexDisk

class BaseConnectionManagerMock: YDConnectionManager {
    let authSettingsMock: AuthSettingsMock

    init(
        authSettingsMock: AuthSettingsMock = AuthSettingsMock(),
        settings: YOSettings = YOSettings()
    ) {
        self.authSettingsMock = authSettingsMock
        super.init(authSettings: authSettingsMock, settings: settings)
    }
}
