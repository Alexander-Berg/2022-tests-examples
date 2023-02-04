//
//  ResetGeoIntentAlertSteps.swift
//  UI Tests
//
//  Created by Aleksey Gotyanov on 10/27/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest

final class ResetGeoIntentAlertSteps: AnyAlertSteps {
    convenience init() {
        self.init(elementType: .alert, alertID: Identifiers.resetGeoIntentAlert)
    }

    @discardableResult
    func tapOnCancelAlertButton() -> FiltersSteps {
        self.tapOnButton(withID: Identifiers.cancelButton)
        return FiltersSteps()
    }

    func tapOnAcceptAlertButton() {
        self.tapOnButton(withID: Identifiers.okButton)
    }

    // MARK: Private

    private enum Identifiers {
        static let okButton = "ok"
        static let cancelButton = "cancel"
        static let resetGeoIntentAlert = "filters.resetGeoIntentAlert"
    }
}
