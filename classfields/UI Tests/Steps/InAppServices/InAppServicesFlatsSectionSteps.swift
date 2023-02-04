//
//  InAppServicesFlatsSectionSteps.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 05.04.2022.
//  Copyright © 2022 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import enum YREAccessibilityIdentifiers.InAppServicesAccessibilityIdentifiers

final class InAppServicesFlatsSectionSteps {
    @discardableResult
    func isPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие блока квартир") { _ -> Void in
            self.flatSection
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутствие блока квартир") { _ -> Void in
            self.flatSection
                .yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnFlat(at index: Int = 0) -> Self {
        XCTContext.runActivity(named: "Тапаем на квартиру в списке (под индексом \(index))") { _ -> Void in
            let snippetElement = self.flatSection
                .descendants(matching: .any)
                .matching(identifier: Identifiers.Flat.view)
                .element(boundBy: index)

            snippetElement
                .yreEnsureExistsWithTimeout()
                .yreForceTap()
        }
        return self
    }

    @discardableResult
    func ensureFlatStatus(_ status: String, at index: Int = 0) -> Self {
        XCTContext.runActivity(named: "Проверяем, что состояние квартиры (под индексом \(index)) - \(status)") { _ -> Void in
            let snippetElement = self.flatSection
                .descendants(matching: .any)
                .matching(identifier: Identifiers.Flat.view)
                .element(boundBy: index)

            let statusElement = ElementsProvider.obtainElement(
                identifier: Identifiers.Flat.statusLabel,
                type: .staticText,
                in: snippetElement
            )

            XCTAssertEqual(statusElement.label, status)
        }
        return self
    }

    @discardableResult
    func isCreationAvailable() -> Self {
        XCTContext.runActivity(named: "Проверяем доступность создания арендной заявки собственника") { _ -> Void in
            let flatHeader = ElementsProvider.obtainElement(
                identifier: Identifiers.Flat.sectionHeader,
                in: self.flatSection
            )
            let actionButton = ElementsProvider.obtainElement(
                identifier: Identifiers.Header.action,
                type: .any,
                in: flatHeader
            )

            actionButton
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isCreationNotAvailable() -> Self {
        XCTContext.runActivity(named: "Проверяем, что создание арендной заявки собственника невозможно") { _ -> Void in
            let flatHeader = ElementsProvider.obtainElement(
                identifier: Identifiers.Flat.sectionHeader,
                in: self.flatSection
            )
            let actionButton = ElementsProvider.obtainElement(
                identifier: Identifiers.Header.action,
                type: .any,
                in: flatHeader
            )

            actionButton
                .yreEnsureNotExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func tapOnFormCreation() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку создания арендной заявки собственника") { _ -> Void in
            let flatHeader = ElementsProvider.obtainElement(
                identifier: Identifiers.Flat.sectionHeader,
                in: self.flatSection
            )
            let actionButton = ElementsProvider.obtainElement(
                identifier: Identifiers.Header.action,
                type: .any,
                in: flatHeader
            )

            actionButton
                .yreEnsureExistsWithTimeout()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func isShowingsAvailable() -> Self {
        XCTContext.runActivity(named: "Проверяем, что \"Поиск квартиры\" отображается") { _ -> Void in
            self.showingsEntry
                .yreEnsureExists()
        }
        return self
    }

    @discardableResult
    func isShowingsNotAvailable() -> Self {
        XCTContext.runActivity(named: "Проверяем, что \"Поиск квартиры\" не отображается") { _ -> Void in
            self.showingsEntry
                .yreEnsureNotExists()
        }
        return self
    }

    @discardableResult
    func tapOnShowings() -> Self {
        XCTContext.runActivity(named: "Нажимаем на \"Поиск квартиры\"") { _ -> Void in
            self.showingsEntry
                .yreEnsureExists()
                .yreTap()
        }
        return self
    }

    // MARK: - Private

    private typealias Identifiers = InAppServicesAccessibilityIdentifiers

    private lazy var flatSection = ElementsProvider.obtainElement(identifier: Identifiers.Flat.section)
    private lazy var showingsEntry = ElementsProvider.obtainElement(identifier: Identifiers.Flat.searchView, in: self.flatSection)
}
