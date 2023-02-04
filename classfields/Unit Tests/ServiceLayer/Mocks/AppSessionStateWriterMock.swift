//
//  AppSessionStateWriterMock.swift
//  Unit Tests
//
//  Created by Timur Guliamov on 10.12.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import YREAppState
import YREFiltersModel

final class AppSessionStateWriterMock: NSObject, YREAppSessionStateWriter {
    var firstLaunchDate: Date? { nil }
    var isFirstLaunch: Bool { false }
    var launchCount: UInt { 0 }
    var sessionStartDate: Date? { nil }
    var lastDeepLinkInfo: DeepLinkInfo? { nil }
    var utmLink: URL? { nil }
    var appVersion: UInt { 0 }

    func updateFirstLaunchDate(_ date: Date) { }
    func updateIsFirstLaunch(_ isFirstLaunch: Bool) { }
    func updateLaunchCount(_ count: UInt) { }
    func updateSessionStart(_ date: Date) { }
    func updateAppVersion(_ appVersion: UInt) { }
    func updateLast(_ deepLinkInfo: DeepLinkInfo?) { }
    func updateUTMLink(_ utmLink: URL) { }
}
