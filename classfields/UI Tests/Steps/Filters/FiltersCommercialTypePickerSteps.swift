//
//  Created by Alexey Aleshkov on 10/09/2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import XCTest
import YREAccessibilityIdentifiers

final class FiltersCommercialTypePickerSteps {
    enum CommercialType {
        // @coreshock: Order matters for readable values
        enum OtherType: Int, CaseIterable, Comparable {
            /// Офис
            case office
            /// Торговое помещение
            case retail
            /// Помещение свободного назначения
            case freePurpose
            /// Складское помещение
            case warehouse
            /// Производственное помещение
            case manufacturing
            /// Общепит
            case publicCatering
            /// Автосервис
            case autoRepair
            /// Гостиница
            case hotel
            /// Готовый бизнес
            case business
            /// Юридический адрес
            case legalAddress

            static func < (lhs: OtherType, rhs: OtherType) -> Bool {
                return lhs.rawValue < rhs.rawValue
            }
        }

        /// Неважно
        case noMatter
        /// Земельный участок
        case land
        /// Остальное
        case others([OtherType])
    }

    @discardableResult
    func select(commercialType: CommercialType) -> Self {
        let titles = Self.rowTitles(commercialType: commercialType)
        for title in titles {
            self.tapOnRow(title)
        }

        return self
    }

    @discardableResult
    func tapOnRow(_ value: String) -> Self {
        let listView = self.listView

        let cell = listView.cells
            .containing(.staticText, identifier: value)
            .element

        cell
            .yreEnsureExistsWithTimeout()
            .yreEnsureEnabled()

        listView.scroll(to: cell)

        cell
            .yreEnsureHittableWithTimeout()
            .yreTap()

        return self
    }

    @discardableResult
    func isScreenPresented() -> Self {
        self.rootView.yreEnsureExistsWithTimeout()
        return self
    }

    @discardableResult
    func isScreenClosed() -> Self {
        self.rootView.yreEnsureNotExistsWithTimeout()
        return self
    }

    @discardableResult
    func tapOnCloseButton() -> Self {
        self.closeButton
            .yreEnsureExistsWithTimeout()
            .yreEnsureHittable()
            .yreTap()

        return self
    }

    @discardableResult
    func tapOnApplyButton() -> Self {
        self.applyButton
            .yreEnsureExistsWithTimeout()
            .yreEnsureHittable()
            .yreTap()

        return self
    }

    private typealias Identifiers = FiltersCommercialTypePickerAccessibilityIdentifiers

    private lazy var rootView = ElementsProvider.obtainElement(identifier: Identifiers.rootViewIdentifier)
    private lazy var listView = ElementsProvider.obtainElement(identifier: Identifiers.listViewIdentifier, in: self.rootView)
    private lazy var closeButton = ElementsProvider.obtainElement(identifier: Identifiers.closeButtonIdentifier)
    private lazy var applyButton = ElementsProvider.obtainElement(identifier: Identifiers.applyButtonIdentifier, in: self.rootView)

    private static func rowTitles(commercialType: CommercialType) -> [String] {
        switch commercialType {
            case .noMatter, .land:
                return [commercialType.readableValue]

            case .others(let values):
                let texts = values.map({ $0.readableValue })
                return texts
        }
    }
}

extension FiltersCommercialTypePickerSteps.CommercialType {
    var readableValue: String {
        switch self {
            case .noMatter: return "Неважно"
            case .land: return "Земельный участок"
            case .others: return "Остальные"
        }
    }
}

extension FiltersCommercialTypePickerSteps.CommercialType.OtherType {
    var readableValue: String {
        switch self {
            case .office: return "Офис"
            case .retail: return "Торговое помещение"
            case .freePurpose: return "Помещение своб. назначения"
            case .warehouse: return "Складское помещение"
            case .manufacturing: return "Производ. помещение"
            case .publicCatering: return "Общепит"
            case .autoRepair: return "Автосервис"
            case .hotel: return "Гостиница"
            case .business: return "Готовый бизнес"
            case .legalAddress: return "Юридический адрес"
        }
    }
}
