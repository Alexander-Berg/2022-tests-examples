//
//  FilterPromoKind.swift
//  UI Tests
//
//  Created by Pavel Zhuravlev on 15.02.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation

enum FilterPromoKind {
    case yandexRent
    case notGrannys

    var title: String {
        switch self {
            case .yandexRent:
                return "Яндекс.Аренда"
            case .notGrannys:
                return "Современный ремонт"
        }
    }
}
