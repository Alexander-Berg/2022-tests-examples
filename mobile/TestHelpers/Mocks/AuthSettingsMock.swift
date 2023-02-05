//
//  AuthSettingsMock.swift
//  30.11.2021
//

import Foundation
#if !DEV_TEST
@testable import YandexDisk
#endif

class AuthSettingsMock: AuthSettingsProtocol {
    var login: String? { return UUID().uuidString }
    var uid: NSNumber? { return NSNumber(value: 0) }
    var token: String? { return UUID().uuidString }
    var isAuthorized: Bool { mockIsAuthorized }
    var hasPlus: Bool { false }
    var displayName: String? { nil }
    var fullName: String? { nil }
    var avatarURL: String? { nil }
    var isYandexTeamUser: Bool { false }
    var isStaff: Bool { false }

    var mockIsAuthorized: Bool = true
}
