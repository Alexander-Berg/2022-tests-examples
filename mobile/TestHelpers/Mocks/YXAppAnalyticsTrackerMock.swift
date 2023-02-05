//
//  YXAppAnalyticsTrackerMock.swift
//  YandexDiskTests
//
//  Created by Denis Kharitonov on 15.06.2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

final class YXAppAnalyticsTrackerMock: NSObject, YXAppAnalyticsTracker {
    var startSessionCalled = false
    func startSession() {
        startSessionCalled = true
    }

    func startSession(options _: [AnyHashable: Any]! = [:]) {
        // Do nothing
    }

    var endSessionCalled = false
    func endSession() {
        endSessionCalled = true
    }

    var trackedEvents = [String]()
    func trackEvent(_ eventName: String!) {
        trackedEvents.append(eventName)
    }

    var trackedEventWithOptions: (String, [AnyHashable: Any])?
    func trackEvent(_ eventName: String!, options: [AnyHashable: Any]! = [:]) {
        trackedEventWithOptions = (eventName, options)
    }
}
