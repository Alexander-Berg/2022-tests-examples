//
//  IterfaceOrientationControllerTest.swift
//  UtilsTests
//
//  Created by Aleksey Makhutin on 25.11.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import XCTest
@testable import UtilsUI

internal final class IterfaceOrientationControllerTest: XCTestCase {

    func testUpdateBlockExecuteWhenOrientationChangeWhenViewIsNotAppear() {
        var blockExecuted = false
        let orientationProvider = MockOrientationProvider()
        let orientationController = InterfaceOrientationController(interfaceProvider: orientationProvider) {
            blockExecuted = true
        }
        orientationController.viewWillAppear()
        orientationController.viewDidLayoutSubviews()
        orientationController.viewDidAppear()
        XCTAssertFalse(blockExecuted, "not need executed when view first appear")

        orientationProvider.mockOrientation = .landscapeLeft
        orientationController.saveInterfaceOrientationIfNeeded(viewOnWindow: false)
        XCTAssertFalse(blockExecuted, "not need executed when view is not on window")

        orientationController.viewWillAppear()
        orientationController.viewDidLayoutSubviews()
        orientationController.viewDidAppear()
        XCTAssertTrue(blockExecuted, "need executed when orientation change, when view is not appear")

        blockExecuted = false

        orientationController.viewWillAppear()
        orientationController.viewDidLayoutSubviews()
        orientationController.viewDidAppear()

        XCTAssertFalse(blockExecuted, "not need executed a second time, when orientation not change")
    }

    func testUpdateBlockNotExecuteWhenOrientationChangeWhenViewIsAppear() {
        var blockExecuted = false
        let orientationProvider = MockOrientationProvider()
        let orientationController = InterfaceOrientationController(interfaceProvider: orientationProvider) {
            blockExecuted = true
        }
        orientationController.viewWillAppear()
        orientationController.viewDidLayoutSubviews()
        orientationController.viewDidAppear()
        XCTAssertFalse(blockExecuted, "not need executed when view first appear")

        orientationProvider.mockOrientation = .landscapeLeft
        orientationController.saveInterfaceOrientationIfNeeded(viewOnWindow: true)
        orientationProvider.mockOrientation = .landscapeRight
        orientationController.saveInterfaceOrientationIfNeeded(viewOnWindow: true)

        orientationController.viewDidLayoutSubviews()
        XCTAssertFalse(blockExecuted, "not need executed when orientation change on view that is appear")

        orientationController.viewWillAppear()
        orientationController.viewDidLayoutSubviews()
        orientationController.viewDidAppear()
        XCTAssertFalse(blockExecuted, "not need executed because orientation not change when view is not appear")
    }

    func testUpdateBlockNotExecuteWhenOrientationNotChange() {
        var blockExecuted = false
        let orientationProvider = MockOrientationProvider()
        let orientationController = InterfaceOrientationController(interfaceProvider: orientationProvider) {
            blockExecuted = true
        }

        orientationController.viewWillAppear()
        orientationController.viewDidLayoutSubviews()
        orientationController.viewDidAppear()
        XCTAssertFalse(blockExecuted, "not need executed when view is first appear")

        orientationController.viewWillAppear()
        orientationController.viewDidLayoutSubviews()
        orientationController.viewDidAppear()
        XCTAssertFalse(blockExecuted, "not need executed when orientation not change")
    }
}

private final class MockOrientationProvider: CurrentInterfaceOrientationProviding {
    var mockOrientation: UIInterfaceOrientation?

    var сurrentInterfaceOrientation: UIInterfaceOrientation {
        return self.mockOrientation ?? .portrait
    }
}
