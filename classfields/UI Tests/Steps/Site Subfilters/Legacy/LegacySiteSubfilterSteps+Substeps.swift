//
//  LegacySiteSubfilterSteps+Substeps.swift
//  UI Tests
//
//  Created by Dmitry Barillo on 25.12.2020.
//  Copyright © 2020 Yandex. All rights reserved.
//

import Foundation

extension LegacySiteSubfilterSteps {
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

    enum MultipleSelectParameter {
        /// Количество комнат
        case totalsRooms
    }
}

extension LegacySiteSubfilterSteps.SingleSelectParameter {
    var accessibilityIdentifier: String {
        let id: String
        switch self {
            case .bathroomType: id = "bathroomType"
            case .deliveryDate: id = "deliveryDate"
        }
        return "filters.cell." + id
    }
}

extension LegacySiteSubfilterSteps.NumberRangeParameter {
    var accessibilityIdentifier: String {
        let id: String
        switch self {
            case .totalArea: id = "areaTotal"
            case .kitchenArea: id = "areaKitchen"
            case .floor: id = "floor"
        }
        return "filters.cell." + id
    }
}
