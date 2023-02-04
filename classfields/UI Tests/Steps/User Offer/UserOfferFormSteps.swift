//
//  UserOfferFormSteps.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 8/10/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import class YREAccessibilityIdentifiers.UserOfferFormAccessibilityIdentifiers
import YRETestsUtils

final class UserOfferFormSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что форма подачи объявления отображается") { _ -> Void in
            let screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)
            screenView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isScreenNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что форма подачи объявления закрыта") { _ -> Void in
            let screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)
            screenView.yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func compareWithScreenshot(identifier: String) -> Self {
        let screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)
        let screenshot = screenView.yreWaitAndScreenshot()
        Snapshot.compareWithSnapshot(image: screenshot, identifier: identifier)
        return self
    }

    @discardableResult
    func tapOnCloseButton() -> Self {
        // FIXME: Implement method after user offer form would be done.
        // https://st.yandex-team.ru/VSAPPS-6894
         XCTFail("`tapOnCloseButton` not implemented")
        return self
    }

    typealias Identifiers = UserOfferFormAccessibilityIdentifiers
}
