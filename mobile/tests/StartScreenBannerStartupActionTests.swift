//
//  StartScreenBannerStartupActionTests.swift
//  YandexMapsStartupActions-Unit-Tests
//
//  Created by Mikhail Kurenkov on 2/26/20.
//

import XCTest
import YandexMapsCommonTypes
import YandexMapsMocks
import YandexMapsRx
import RxSwift
import RxCocoa
import RxTest
@testable import YandexMapsStartupActions

class StartScreenBannerStartupActionTests: XCTestCase {

    func testWhenHasDiscoveryNotificationActionCanBePerformed() {
        let notification = makeNotification(ofKind: .discovery)
        let deps = StartScreenBannerStartupActionDepsMock(
            notification: notification
        )
        let testScheduler = TestScheduler(initialClock: 0)
        let action = StartScreenBannerStartupAction(deps: deps, scheduler: testScheduler)
        let observer = testScheduler.start {
            return action.canBePerformed.asObservable()
        }
        XCTAssert(observer.events[0].value == .next(true))
        XCTAssert(observer.events[1].value == .completed)
    }

    func testWhenHasBusinessNotificationActionCanBePerformed() {
        let notification = makeNotification(ofKind: .business)
        let deps = StartScreenBannerStartupActionDepsMock(
            notification: notification
        )
        let testScheduler = TestScheduler(initialClock: 0)
        let action = StartScreenBannerStartupAction(deps: deps, scheduler: testScheduler)
        let observer = testScheduler.start {
            return action.canBePerformed.asObservable()
        }
        XCTAssert(observer.events[0].value == .next(true))
        XCTAssert(observer.events[1].value == .completed)
    }


    func testWhenHasEmergencyNotificationActionCanNotBePerformedOnTimeout() {
        let notification = makeNotification(ofKind: .emergency)
        let deps = StartScreenBannerStartupActionDepsMock(
            notification: notification
        )
        let testScheduler = TestScheduler(initialClock: 0)
        let action = StartScreenBannerStartupAction(deps: deps, scheduler: testScheduler)
        let observer = testScheduler.start {
            return action.canBePerformed.asObservable()
        }
        XCTAssert(observer.events[0].value == .next(false))
        XCTAssert(observer.events[1].value == .completed)
    }


    private func makeNotification(ofKind kind: StartScreenNotification.Kind) -> StartScreenNotification {
        return StartScreenNotification(
            id: "",
            description: "",
            bannerUrlTemplate: "",
            actionUrl: nil,
            kind: kind
        )
    }

}

class StartScreenBannerStartupActionDepsMock: StartScreenBannerStartupActionDeps {

    let startupActionsExperimentsProvider: StartupActionsExperimentsProvider
    let startScreenBannerDisplayController: StartScreenBannerDisplayController
    let startScreenNotificationProvider: StartScreenNotificationProvider

    init(notification: StartScreenNotification?) {
        self.startupActionsExperimentsProvider = FakeStartupActionsExperimentsProvider()
        self.startScreenBannerDisplayController = FakeStartScreenBannerDisplayController()
        self.startScreenNotificationProvider = FakeStartScreenNotificationProvider(notification: notification)
    }

}

class FakeStartupActionsExperimentsProvider: StartupActionsExperimentsProvider {

    func startupActionsIntroMessageId() -> String? {
        return nil
    }

    func startupActionsIntroMessageEndTime() -> String? {
        return nil
    }

    func startupActionsIntroMessageBody() -> String? {
        return nil
    }

    func startupActionsIntroMessageActionButtonText() -> String? {
        return nil
    }

    func startupActionsIntroMessageActionButtonLink() -> String? {
        return nil
    }

    func startupActionsShowBookingsIntroscreen() -> Bool {
        return false
    }

    func startupActionsNaviIntroSheet() -> Bool {
        return false
    }

    func startupActionsShowBackendDrivenIntroscreen() -> Bool {
        return false
    }
}
