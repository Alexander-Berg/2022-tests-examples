//
//  PushNotificationsRegistrationStartupActionTests.swift
//  YandexMapsStartupActions-Unit-Tests
//
//  Created by Mikhail Kurenkov on 2/26/20.
//

import XCTest
import YandexMapsCommonTypes
import YandexMapsMocks
@testable import YandexMapsStartupActions

class PushNotificationsRegistrationStartupActionTests: XCTestCase {

    func testRegistered() {
        let deps = PushNotificationsRegistrationStartupActionDepsMock(isRegisteredForPushNotifications: true)
        let action = PushNotificationsRegistrationStartupAction(deps: deps)
        XCTAssert(!action.canBePerformed)
    }

    func testNotRegistered() {
        let deps = PushNotificationsRegistrationStartupActionDepsMock(isRegisteredForPushNotifications: false)
        let action = PushNotificationsRegistrationStartupAction(deps: deps)
        XCTAssert(action.canBePerformed)
    }

     func testRegisteredAndShownInCurrentVersion() {
         let deps = PushNotificationsRegistrationStartupActionDepsMock(isRegisteredForPushNotifications: true)

         deps.startupActionsRepository.markActionAsShown(StartupActionKeys.pushNotificationsRegistration,
                                                         in: deps.applicationInfoProvider.info.currentVersion)

         let action = PushNotificationsRegistrationStartupAction(deps: deps)
         XCTAssert(!action.canBePerformed)
     }

     func testNotRegisteredAndShownInCurrentVersion() {
         let deps = PushNotificationsRegistrationStartupActionDepsMock(isRegisteredForPushNotifications: false)

         deps.startupActionsRepository.markActionAsShown(StartupActionKeys.pushNotificationsRegistration,
                                                         in: deps.applicationInfoProvider.info.currentVersion)

         let action = PushNotificationsRegistrationStartupAction(deps: deps)
         XCTAssert(!action.canBePerformed)
    }

    func testRegisteredAndShownInPreviousVersion() {
        let deps = PushNotificationsRegistrationStartupActionDepsMock(isRegisteredForPushNotifications: true)

        deps.startupActionsRepository.markActionAsShown(StartupActionKeys.pushNotificationsRegistration,
                                                        in: "previous version")

        let action = PushNotificationsRegistrationStartupAction(deps: deps)
        XCTAssert(!action.canBePerformed)
    }

     func testNotRegisteredAndShownInPreviousVersion() {
         let deps = PushNotificationsRegistrationStartupActionDepsMock(isRegisteredForPushNotifications: false)

         deps.startupActionsRepository.markActionAsShown(StartupActionKeys.pushNotificationsRegistration,
                                                         in: "previous version")

         let action = PushNotificationsRegistrationStartupAction(deps: deps)
         XCTAssert(!action.canBePerformed)
     }

}

class PushNotificationsRegistrationStartupActionDepsMock: PushNotificationsRegistrationStartupActionDeps {

    let pushNotificationsRegistrationInteractor: PushNotificationsRegistrationInteractor
    let startupActionsRepository: StartupActionsRepository
    let applicationInfoProvider: ApplicationInfoProvider

    init(isRegisteredForPushNotifications: Bool) {
        self.pushNotificationsRegistrationInteractor
            = FakePushNotificationsRegistrationInteractor(isRegistered: isRegisteredForPushNotifications)
        self.startupActionsRepository = FakeStartupActionsRepository()
        self.applicationInfoProvider = FakeApplicationInfoProvider(launchType: .anotherLaunch)
    }

}
