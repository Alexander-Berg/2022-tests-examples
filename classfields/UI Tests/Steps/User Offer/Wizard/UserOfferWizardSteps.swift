//
//  UserOfferWizardSteps.swift
//  UI Tests
//
//  Created by Arkady Smirnov on 8/7/20.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation
import YREAccessibilityIdentifiers
import YRETestsUtils
import XCTest

final class UserOfferWizardSteps {
    @discardableResult
    func isScreenPresented() -> Self {
        XCTContext.runActivity(named: #"Проверяем, что визард формы подачи показан"#) { _ in
            let screenView = ElementsProvider.obtainElement(identifier: Identifiers.view)
            screenView.yreEnsureExistsWithTimeout()
            return self
        }
    }

    typealias Identifiers = UserOfferWizardAccessibilityIdentifiers
}

extension UserOfferWizardSteps {
    @discardableResult
    func isOfferTypeStepPresented() -> Self {
        XCTContext.runActivity(named: #"Проверяем, что шаг с типами сделки показан"#) { _ in
            let screenView = ElementsProvider.obtainElement(identifier: Identifiers.offerTypeStepView)
            screenView.yreEnsureExistsWithTimeout()
            return self
        }
    }

    @discardableResult
    func tapOnOfferType(_ name: String) -> Self {
        XCTContext.runActivity(named: "Нажимаем на тип сделки \(name)") { _ in
            let stepView = ElementsProvider.obtainElement(identifier: Identifiers.offerTypeStepView)
            let cell = ElementsProvider.obtainElement(identifier: Identifiers.offerTypeCellPrefix + name, in: stepView)
            cell
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
            return self
        }
    }
}

extension UserOfferWizardSteps {
    @discardableResult
    func isRealtyTypeStepPresented() -> Self {
        XCTContext.runActivity(named: #"Проверяем, что шаг с типами недвижимости показан"#) { _ in
            let screenView = ElementsProvider.obtainElement(identifier: Identifiers.realtyTypeStepView)
            screenView.yreEnsureExistsWithTimeout()
            return self
        }
    }

    @discardableResult
    func tapOnRealtyType(_ name: String) -> Self {
        XCTContext.runActivity(named: "Выбираем тип недвижимости \(name)") { _ in
            let stepView = ElementsProvider.obtainElement(identifier: Identifiers.realtyTypeStepView)
            let cell = ElementsProvider.obtainElement(identifier: Identifiers.realtyTypeCellPrefix + name, in: stepView)
            cell
                .yreEnsureExistsWithTimeout()
                .yreEnsureHittable()
                .yreTap()
            return self
        }
    }
}

extension UserOfferWizardSteps {
    @discardableResult
    func isAddressScreenPresented() -> PlacePickerSteps {
        XCTContext.runActivity(named: #"Проверяем, что шаг с адресом показан"#) { _ in
            let screenView = ElementsProvider.obtainElement(identifier: PlacePickerAccessibilityIdentifiers.viewController)
            screenView.yreEnsureExistsWithTimeout()
            return PlacePickerSteps()
        }
    }
}

extension UserOfferWizardSteps {
    @discardableResult
    func isRentProviderStepPresented() -> RentProviderSteps {
        XCTContext.runActivity(named: #"Проверяем, что шаг с Я Арендой показан"#) { _ in
            let screenView = ElementsProvider.obtainElement(identifier: Identifiers.rentProviderTypeStepView)
            screenView.yreEnsureExistsWithTimeout()
            return RentProviderSteps()
        }
    }
}
