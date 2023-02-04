//
//  AuthStateObservableAgentTests.swift
//  YandexRealtyTests
//
//  Created by Arkady Smirnov on 11/12/19.
//  Copyright © 2019 Yandex. All rights reserved.
//

import XCTest
import protocol YREServiceLayer.AuthStateObserver
import protocol YREServiceLayer.AuthStateObservable
import class YREServiceLayer.AuthStateObservableAgent

class AuthStateObservableAgentTests: XCTestCase {
    func testAuthStateObservableAgentSubscription() {
        // AuthStateObservableAgent should observe notifications with names in a list below and notify its observers about 'authStateObservableDidUpdate' event.
        let notificationNames: [Notification.Name] = [
            .kYREAuthDidStartNewSessionNotification,
            .kYREAuthDidInvalidateCurrentSessionNotification,
            .AuthStateReaderUUIDWasChangedNotification
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

        let receivedExpectationCallback: (AuthStateObservable) -> Void = { _ in
            // Commit to recieved notification
            receivedExpectation.fulfill()
        }

        let unsubscriptionExpectationCallback: (AuthStateObservable) -> Void = { _ in
            // Commit to recieved notification
            unsubscriptionExpectation.fulfill()
        }

        // Subjects to test
        let observer = TestAuthStateObserver(with: receivedExpectationCallback)
        let deadObserver = TestAuthStateObserver(with: unsubscriptionExpectationCallback)

        let reader = AuthStateReaderMock()
        let authStateObservableAgent = AuthStateObservableAgent(authStateReader: reader)

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

    // MARK: Private helpers

    private class TestAuthStateObserver: NSObject, AuthStateObserver {
        init(with callback: @escaping (AuthStateObservable) -> Void) {
            self.callback = callback
        }

        func authStateObservableDidUpdate(_ observable: AuthStateObservable) {
            self.callback(observable)
        }

        private let callback: ((AuthStateObservable) -> Void)
    }
}
