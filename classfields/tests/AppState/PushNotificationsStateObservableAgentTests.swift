//
//  PushNotificationsStateObservableAgentTests.swift
//  YandexRealtyTests
//
//  Created by Arkady Smirnov on 11/13/19.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest
import struct YREAppState.PushNotificationsMask
import protocol YREAppState.YREPushNotificationsStateReader
import enum YREModelObjc.PushPermissionsState
import protocol YREServiceLayer.PushNotificationsStateObservable
import protocol YREServiceLayer.PushNotificationsStateObserver
import class YREServiceLayer.PushNotificationsStateObservableAgent

class PushNotificationsStateObservableAgentTests: XCTestCase {
func testPushNotificationsStateObservableAgentSubscription() {
        // PushNotificationsStateObservableAgent should observe notifications with names in a list below and notify its observers about 'pushNotificationsStateObservableDidUpdate' event.
        let notificationNames: [Notification.Name] = [
            .PushTokenWasChanged,
            .PushPermissionsWereChanged
        ]

        // Run test for each notification
        notificationNames.forEach { self.runSubscriptionsTestScenario(for: $0) }
    }

    private func runSubscriptionsTestScenario(for notificationName: Notification.Name) {
        // Set up expecations
        let unsubscriptionExpectation = self.expectation(description:
            "Observer shouldn't received notification if observation is deallocated.")
        unsubscriptionExpectation.isInverted = true

        let receivedExpectation = self.expectation(description:
            "Observer should received only one '\(notificationName)' notification.")
        receivedExpectation.expectedFulfillmentCount = 1
        receivedExpectation.assertForOverFulfill = true

        let receivedExpectationCallback: (PushNotificationsStateObservable) -> Void = { _ in
            // Commit to recieved notification
            receivedExpectation.fulfill()
        }
        let unsubscriptionExpectationCallback: (PushNotificationsStateObservable) -> Void = { _ in
            // Commit to recieved notification
            unsubscriptionExpectation.fulfill()
        }

        // Subjects to test
        let observer = TestPushNotificationsStateObserver(with: receivedExpectationCallback)
        let deadObserver = TestPushNotificationsStateObserver(with: unsubscriptionExpectationCallback)

        let reader = TestPushNotificationsStateReader()
        let authStateObservableAgent = PushNotificationsStateObservableAgent(pushNotificationsStateReader: reader)

        // Act
        // Step 1. Send notification before subscription
        NotificationCenter.default.post(name: notificationName, object: nil, userInfo: nil)

        // Step 2. Subscribe to observable
        var deadObservation = Optional(authStateObservableAgent.observe(by: deadObserver))
        let observation = authStateObservableAgent.observe(by: observer)
        // Step 3. Remove one of observers
        deadObservation = nil
        // Step 4. Send notification again
        NotificationCenter.default.post(name: notificationName, object: nil, userInfo: nil)

        // Asserts
        XCTAssertNotNil(observation)
        XCTAssertNil(deadObservation) // Hack to avoid warning - Variable 'deadObservation' was written to, but never read
        wait(for: [unsubscriptionExpectation, receivedExpectation], timeout: 0)
    }


    // MARK: Private helpers.

    private class TestPushNotificationsStateReader: NSObject, YREPushNotificationsStateReader {
        var enabledPushNotifications: PushNotificationsMask = .disabled
        var didDisplayPushPermissionsRequest: Bool = false
        // swiftlint:disable:next identifier_name
        var didDisplayPushNotificationsDisabledNotification: Bool = false
        var pushToken: String? = "testPushToken"
        var pushPermissionsState: PushPermissionsState = .disabled
    }

    private class TestPushNotificationsStateObserver: NSObject, PushNotificationsStateObserver {
        init(with callback: @escaping (PushNotificationsStateObservable) -> Void) {
            self.callback = callback
        }

        func pushNotificationsStateObservableDidUpdate(_ observable: PushNotificationsStateObservable) {
            self.callback(observable)
        }

        private let callback: ((PushNotificationsStateObservable) -> Void)
    }
}
