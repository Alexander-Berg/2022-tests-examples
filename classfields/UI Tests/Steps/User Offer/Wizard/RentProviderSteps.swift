//
//  RentProviderSteps.swift
//  UI Tests
//
//  Created by Alexey Salangin on 17.06.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YREAccessibilityIdentifiers

final class RentProviderSteps {
    @discardableResult
    func tapYandexRentButton() -> YaRentOwnerApplicationSteps {
        XCTContext.runActivity(named: #"Нажимаем на кнопку "Хочу сдать с вами""#) { _ in
            let rentButton = ElementsProvider.obtainElement(identifier: Identifiers.rentProviderYandexRent)
            rentButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
            return YaRentOwnerApplicationSteps()
        }
    }

    @discardableResult
    func tapPrivatePersonButton() -> Self {
        XCTContext.runActivity(named: #"Нажимаем на кнопку "Сдам самостоятельно""#) { _ in
            let privatePersonButton = ElementsProvider.obtainElement(identifier: Identifiers.rentProviderPrivatePerson)
            privatePersonButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
            return self
        }
    }

    private typealias Identifiers = UserOfferWizardAccessibilityIdentifiers
}
