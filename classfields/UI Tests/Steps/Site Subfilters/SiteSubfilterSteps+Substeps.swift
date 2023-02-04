//
//  SiteSubfilterSteps+Substeps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 25.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation

extension SiteSubfilterSteps {
    enum RoomsTotal: CaseIterable {
        case studio
        case rooms1
        case rooms2
        case rooms3
        case rooms4Plus
    }

    enum PriceParameter {
        /// Цена
        case totalPrice
    }

    enum SingleSelectParameter {
        /// Санузел
        case bathroomType
        /// Срок сдачи
        case deliveryDate
    }

    enum NumberRangeParameter {
        // "Общая площадь"
        case totalArea
        // "Площадь кухни"
        case kitchenArea
        // "Этаж"
        case floor
    }
}

extension SiteSubfilterSteps.RoomsTotal {
    var accessibilityIdentifier: String {
        let id: String
        switch self {
            case .studio: id = "Студия"
            case .rooms1: id = "1"
            case .rooms2: id = "2"
            case .rooms3: id = "3"
            case .rooms4Plus: id = "4+"
        }
        return "YRESegmentedControl-" + id
    }

    var readableName: String {
        switch self {
            case .studio: return "Студия"
            case .rooms1: return "1 комната"
            case .rooms2: return "2 комнаты"
            case .rooms3: return "3 комнаты"
            case .rooms4Plus: return "4+ комнат"
        }
    }
}

extension SiteSubfilterSteps.PriceParameter {
    var accessibilityIdentifier: String {
        let id: String
        switch self {
            case .totalPrice: id = "price"
        }

        return "filters.cell." + id
    }

    var readableName: String {
        switch self {
            case .totalPrice: return "Цена"
        }
    }
}


extension SiteSubfilterSteps.SingleSelectParameter {
    var accessibilityIdentifier: String {
        let id: String
        switch self {
            case .bathroomType: id = "bathroomType"
            case .deliveryDate: id = "deliveryDate"
        }
        return "filters.cell." + id
    }

    var readableName: String {
        switch self {
            case .bathroomType: return "Санузел"
            case .deliveryDate: return "Срок сдачи"
        }
    }
}

extension SiteSubfilterSteps.NumberRangeParameter {
    var accessibilityIdentifier: String {
        let id: String
        switch self {
            case .totalArea: id = "areaTotal"
            case .kitchenArea: id = "areaKitchen"
            case .floor: id = "floor"
        }
        return "filters.cell." + id
    }

    var readableName: String {
        switch self {
            case .totalArea:    return "Общая площадь"
            case .kitchenArea:  return "Площадь кухни"
            case .floor:        return "Этаж"
        }
    }
}
