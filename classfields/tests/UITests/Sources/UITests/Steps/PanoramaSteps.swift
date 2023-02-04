//
//  PanoramaSteps.swift
//  UITests
//
//  Created by Dmitry Sinev on 8/4/20.
//

import XCTest
import Snapshots

class PanoramaSteps: BaseSteps {
    func onPanoramaScreen() -> PanoramaScreen {
        return baseScreen.on(screen: PanoramaScreen.self)
    }

    func closeHelp() -> PanoramaSteps {
        onPanoramaScreen().bigCloseHelpButton.tap()
        return self
    }

    func tapRecordButton() -> PanoramaSteps {
        onPanoramaScreen().recordPanoramaButton.tap()
        return self
    }

    func rotateToPortrait() -> PanoramaSteps {
        XCUIDevice.shared.orientation = .portrait
        return self
    }

    func rotateToLandscape() -> PanoramaSteps {
        XCUIDevice.shared.orientation = .landscapeRight
        return self
    }
}
