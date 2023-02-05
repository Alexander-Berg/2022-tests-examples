//
// Created by Elizaveta Y. Voronina on 12/12/19.
// Copyright (c) 2019 Yandex. All rights reserved.
//

import Foundation
import testopithecus
import XCTest

public final class RotatableApplication: Rotatable {

    public func isInLandscape() -> Bool {
        XCTContext.runActivity(named: "Checking if the device is in the landscape") { _ in
            return XCUIDevice.shared.orientation.isLandscape
        }
    }

    public func rotateToLandscape() {
        XCTContext.runActivity(named: "Rotating to landscape") { _ in
            XCUIDevice.shared.orientation = .landscapeRight
        }
    }

    public func rotateToPortrait() {
        XCTContext.runActivity(named: "Rotating to portrait") { _ in
            XCUIDevice.shared.orientation = .portrait
        }
    }
}
