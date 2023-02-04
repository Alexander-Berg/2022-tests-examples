//
//  InAppServicesUserOffersCollectionSteps.swift
//  UI Tests
//
//  Created by Denis Mamnitskii on 29.10.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import XCTest
import YRETestsUtils
import enum YREAccessibilityIdentifiers.InAppServicesAccessibilityIdentifiers

final class InAppServicesUserOffersCollectionSteps {
    enum SwipeDirection: String {
        case left = "Влево"
        case right = "Вправо"
    }

    enum SnippetType: String {
        case common = "Обычный"
        case rentPromo = "Промо"
    }

    @discardableResult
    func isPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что \"Мои объявления\" отображаются в виде горзонтального списка") { _ -> Void in
            self.collectionView.yreEnsureExistsWithTimeout()
        }
        return self
    }

    @discardableResult
    func swipe(_ direction: SwipeDirection) -> Self {
        XCTContext.runActivity(named: "Свайпаем по списку объявлений \(direction.rawValue.lowercased())") { _ -> Void in
            let collection = self.collectionView

            collection
                .yreEnsureExists()
                .yreEnsureHittable()

            switch direction {
                case .left:
                    collection.swipeLeft()
                case .right:
                    collection.swipeRight()
            }
        }
        return self
    }

    @discardableResult
    func tapOnOffer() -> Self {
        XCTContext.runActivity(named: "Нажимаем на текущее объявление") { _ -> Void in
            let cell = self.getCurrentCell()

            cell.yreEnsureExists()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func tapOnOfferAction() -> Self {
        XCTContext.runActivity(named: "Нажимаем на кнопку действия в текущем объявлении") { _ -> Void in
            let cell = self.getCurrentCell()
            let button = ElementsProvider.obtainButton(
                identifier: Identifiers.Components.action,
                in: cell
            )
            button
                .yreEnsureExists()
                .yreEnsureHittable()
                .yreTap()
        }
        return self
    }

    @discardableResult
    func ensureTypeOfSnippet(_ type: SnippetType) -> Self {
        XCTContext.runActivity(named: "Проверям, что тип текущего объявления - \(type.rawValue)") { _ -> Void in
            let cell = self.getCurrentCell()
            cell.yreEnsureExistsWithTimeout()

            let identifier: String
            switch type {
                case .common:
                    identifier = Identifiers.snippetView
                case .rentPromo:
                    identifier = Identifiers.promoView
            }

            ElementsProvider
                .obtainElement(identifier: identifier, in: cell)
                .yreEnsureExists()
        }
        return self
    }

    @discardableResult
    func isViewsHistogramPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что график просмотров отображается в текущем объявлении") { _ -> Void in
            let cell = self.getCurrentCell()

            let histogram = ElementsProvider.obtainElement(
                identifier: Identifiers.Components.histogram,
                in: cell
            )

            histogram
                .yreEnsureExists()
        }
        return self
    }

    @discardableResult
    func isViewsHistogramNotPresented() -> Self {
        XCTContext.runActivity(named: "Проверяем, что график просмотров не показывается в текущем объявлении") { _ -> Void in
            let cell = self.getCurrentCell()

            let histogram = ElementsProvider.obtainElement(
                identifier: Identifiers.Components.histogram,
                in: cell
            )

            histogram
                .yreEnsureNotExists()
        }
        return self
    }

    // MARK: - Private

    private typealias Identifiers = InAppServicesAccessibilityIdentifiers.UserOffers

    private lazy var view = ElementsProvider.obtainElement(identifier: Identifiers.Collection.cell)
    private lazy var collectionView = ElementsProvider.obtainElement(
        identifier: Identifiers.Collection.collectionView,
        type: .collectionView,
        in: self.view
    )

    private func getCurrentCell() -> XCUIElement {
        let cells = self.collectionView.cells
        let visibleCells = cells.allElementsBoundByIndex

        let collectionFrame = self.collectionView.frame
        let currentCell = visibleCells.first(where: { cell in
            let cellMidPoint: CGPoint = .init(x: cell.frame.midX, y: collectionFrame.midY)
            return collectionFrame.contains(cellMidPoint)
        })

        return currentCell ?? cells.firstMatch
    }
}
