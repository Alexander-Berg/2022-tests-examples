//
//  InAppServicesUserOffersSteps.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 27.10.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//
import XCTest
import YRETestsUtils
import enum YREAccessibilityIdentifiers.InAppServicesAccessibilityIdentifiers

final class InAppServicesUserOffersSteps {
    @discardableResult
    func isSectionPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие секции \"Мои объявления\"") { _ -> Void in
            self.header
                .yreEnsureExistsWithTimeout()
            let sectionIsExists = self.collectionView.exists || self.listView.exists
            XCTAssertTrue(sectionIsExists)
        }
        return self
    }

    @discardableResult
    func isSectionNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем отсутсвие секции \"Мои объявления\"") { _ -> Void in
            self.header
                .yreEnsureNotExists()
            let sectionIsExists = self.collectionView.exists || self.listView.exists
            XCTAssertFalse(sectionIsExists)
        }
        return self
    }

    @discardableResult
    func isCreateSectionPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем наличие секции \"Добавить объявление\"") { _ -> Void in
            self.emptyView
                .yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func isSectionPresentedAsCollection() -> InAppServicesUserOffersCollectionSteps {
        InAppServicesUserOffersCollectionSteps().isPresented()
    }

    @discardableResult
    func isSectionPresentedAsList() -> InAppServicesUserOffersListSteps {
        return InAppServicesUserOffersListSteps().isPresented()
    }

    @discardableResult
    func tapOnCreateSection() -> Self {
        XCTContext.runActivity(named: "Нажимаем на секцию \"Добавить объявление\"") { _ -> Void in
            self.emptyView
                .yreEnsureExistsWithTimeout()
                .yreForceTap()
        }
        return self
    }

    @discardableResult
    func tapOnHeaderTitle() -> Self {
        XCTContext.runActivity(named: "Нажимаем на заголовок блока \"Мои объявления\"") { _ -> Void in
            self.headerTitle
                .yreEnsureExistsWithTimeout()
                .yreForceTap()
        }
        return self
    }

    @discardableResult
    func tapOnHeaderAction() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку добавления в блоке \"Мои объявления\"") { _ -> Void in
            self.headerAction
                .yreEnsureExistsWithTimeout()
                .yreForceTap()
        }
        return self
    }

    // MARK: - Private

    private typealias Identifiers = InAppServicesAccessibilityIdentifiers.UserOffers

    private lazy var header = ElementsProvider.obtainElement(identifier: Identifiers.header)
    private lazy var headerTitle = ElementsProvider.obtainElement(identifier: InAppServicesAccessibilityIdentifiers.Header.title)
    private lazy var headerAction = ElementsProvider.obtainElement(identifier: InAppServicesAccessibilityIdentifiers.Header.action)

    private lazy var collectionView = ElementsProvider.obtainElement(identifier: Identifiers.Collection.cell)
    private lazy var listView = ElementsProvider.obtainElement(identifier: Identifiers.listView)
    private lazy var emptyView = ElementsProvider.obtainElement(identifier: Identifiers.emptyView)
}
