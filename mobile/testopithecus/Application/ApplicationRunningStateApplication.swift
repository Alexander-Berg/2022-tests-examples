//
//  ApplicationStateApplication.swift
//  YandexMobileMailAutoTests
//
//  Created by Artem I. Novikov on 22.05.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import testopithecus
import XCTest

public final class ApplicationRunningStateApplication: ApplicationRunningState {
    public func getApplicationRunningState() -> AppRunningState {
        XCTContext.runActivity(named: "Getting application state") { _ in
            switch XCUIApplication.yandexMobileMail.state {
            case .notRunning:
                return AppRunningState.notRunning
            case .runningBackground:
                return AppRunningState.runningBackground
            case .runningBackgroundSuspended:
                return AppRunningState.runningBackgroundSuspended
            case .runningForeground:
                return AppRunningState.runningForeground
            case .unknown:
                return AppRunningState.unknown
            default:
                YOXCTFail("Unknown application state")
            }
        }
    }
    
    public func changeApplicationRunningState(_ state: AppRunningState) {
        XCTContext.runActivity(named: "Changing application state to \(state)") { _ in
            switch state {
            case .notRunning:
                YOXCTFail("Not implemented")
            case .runningBackground:
                XCUIDevice.shared.press(.home)
                sleep(2)
            case .runningBackgroundSuspended:
                YOXCTFail("Not implemented")
            case .runningForeground:
                XCUIApplication.yandexMobileMail.activate()
            case .unknown:
                YOXCTFail("Not implemented")
            }
        }
    }
}
