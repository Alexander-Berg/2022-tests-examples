//
//  Created by Alexey Aleshkov on 20/04/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

protocol SystemDialogMonitorHandlerProtocol: AnyObject {
    var name: String { get }
    func handleDialog(_ element: XCUIElement) -> Bool
}

final class SystemDialogMonitor {
    init(testCase: XCTestCase, handler: SystemDialogMonitorHandlerProtocol) {
        self.testCase = testCase
        self.handler = handler
        self.token = nil
        self.isRequired = true
    }

    deinit {
        self.deactivate()
    }

    var isRequired: Bool

    func activate() {
        XCTAssertNil(
            self.token,
            "It looks like you are trying to add same \"\(self.handler.name)\" handler twice."
        )

        let name = self.handler.name
        self.token = self.testCase.addUIInterruptionMonitor(
            withDescription: name,
            handler: { [weak self] element -> Bool in
                guard let strongSelf = self else { return false }
                let isHandled = strongSelf.handler.handleDialog(element)
                if isHandled == false && strongSelf.isRequired {
                    XCTFail("\"\(name)\" not handled")
                    return true
                }

                return isHandled
            }
        )
    }

    func deactivate() {
        if let token = self.token {
            self.testCase.removeUIInterruptionMonitor(token)
            self.token = nil
        }
    }

    private let testCase: XCTestCase
    private let handler: SystemDialogMonitorHandlerProtocol
    private var token: NSObjectProtocol?
}
