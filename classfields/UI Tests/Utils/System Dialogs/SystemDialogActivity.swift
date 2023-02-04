//
//  Created by Alexey Aleshkov on 22/04/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

enum SystemDialogs {
}

protocol SystemDialogActivityHandlerProtocol: SystemDialogMonitorHandlerProtocol {
    associatedtype Button

    var button: Button? { get set }
}

final class SystemDialogActivity<Handler: SystemDialogActivityHandlerProtocol> {
    @discardableResult
    func tapOnButton(_ button: Handler.Button, from application: XCUIApplication = XCUIApplication()) -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку '\(button)' системного алерта") { _ -> Void in
            self.handler.button = button
            self.intercept(in: application)
        }
        return self
    }

    @discardableResult
    func required() -> Self {
        self.monitor.isRequired = true
        return self
    }

    @discardableResult
    func optional() -> Self {
        self.monitor.isRequired = false
        return self
    }

    @discardableResult
    func activate() -> Self {
        XCTContext.runActivity(named: "Активируем монитор системных алертов") { _ -> Void in
            self.monitor.activate()
        }
        return self
    }

    @discardableResult
    func deactivate() -> Self {
        XCTContext.runActivity(named: "Деактивируем монитор системных алертов") { _ -> Void in
            self.monitor.deactivate()
        }
        return self
    }

    init(testCase: XCTestCase, handler: Handler) {
        self.handler = handler
        self.monitor = .init(testCase: testCase, handler: handler)
    }

    private let handler: Handler
    private let monitor: SystemDialogMonitor

    private func intercept(in application: XCUIApplication = XCUIApplication()) {
        let point = application.coordinate(withNormalizedOffset: CGVector(dx: 0, dy: 0))
        let frame = application.frame
        let tapPoint = point.withOffset(CGVector(dx: frame.size.width, dy: 0))
        tapPoint.tap()
    }
}
