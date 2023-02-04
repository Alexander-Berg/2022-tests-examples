//
//  AnyAlertSteps.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 20.04.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils

open class AnyAlertSteps {
    init(elementType: XCUIElement.ElementType,
         alertID: String) {
        self.elementType = elementType
        self.alertID = alertID
    }

    private let alertID: String
    private let elementType: XCUIElement.ElementType

    private lazy var alert = ElementsProvider.obtainElement(
        identifier: self.alertID,
        type: self.elementType
    )
}

extension AnyAlertSteps {
    @discardableResult
    func screenIsPresented() -> Self {
        self.alert.yreEnsureExistsWithTimeout()
        return self
    }

    @discardableResult
    func screenIsDismissed() -> Self {
        self.alert.yreEnsureNotExistsWithTimeout()
        return self
    }

    @discardableResult
    func tapOnButton(withID id: String) -> Self {
        let button = self.obtainButton(withID: id)
        button
            .yreEnsureExistsWithTimeout()
            .yreTap()
        return self
    }

    func obtainButton(withID id: String) -> XCUIElement {
        let result = ElementsProvider.obtainElement(
            identifier: id,
            type: .button,
            in: self.alert
        )
        return result
    }

    @discardableResult
    func compareWithScreenshot(identifier: String) -> Self {
        XCTContext.runActivity(named: "Сравниваем с имеющимся скриншотом") { _ -> Void in
            let screenshot = self.alert.yreWaitAndScreenshot()
            Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
        }
        return self
    }
}
