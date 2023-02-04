//
//  InAppServicesUserOffersListSteps.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 29.10.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import enum YREAccessibilityIdentifiers.InAppServicesAccessibilityIdentifiers

final class InAppServicesUserOffersListSteps {
    @discardableResult
    func isPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что \"Мои объявления\" отображаются в виде вертикального списка") { _ -> Void in
            self.listView
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnOffer(at index: Int) -> Self {
        XCTContext.runActivity(named: "Нажимаем на \(index + 1) объявление") { _ -> Void in
            let element = listView
                .descendants(matching: .any)
                .matching(identifier: Identifiers.Components.header)
                .element(boundBy: index)

            element
                .yreEnsureExistsWithTimeout()
                .yreForceTap()
        }
        return self
    }

    @discardableResult
    func ensureStatusIsEqual(to text: String, at index: Int) -> Self {
        XCTContext.runActivity(named: "Проверяем, что статус \(index + 1)-го объявления - \"\(text)\"") { _ -> Void in
            let element = listView
                .descendants(matching: .staticText)
                .matching(identifier: Identifiers.Components.headerStatus)
                .element(boundBy: index)

            XCTAssertEqual(element.label, text)
        }
        return self
    }

    // MARK: - Private

    private typealias Identifiers = InAppServicesAccessibilityIdentifiers.UserOffers
    
    private lazy var listView = ElementsProvider.obtainElement(identifier: Identifiers.listView)
}
