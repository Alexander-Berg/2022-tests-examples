//
//  SearchIdentifiersProviderMock.swift
//  ProjectedLibTestApp
//
//  Created by Alexander Shchavrovskiy on 10.03.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import YandexMapsCommonTypes

final class SearchIdentifiersProviderMock: SearchIdentifiersProvider {
    let directPageId: String = "3897"
    let advertisementPageId: String = "mobile_maps"
    let billboardPageId: String = "mobile_maps_route_pins_1"
    let categoriesAdvertPageId: String = "mobile_maps_menu_icon_1"
    let fakePageId: String = "fake"
    let gasStationsStationsPaymentCategoryId: String = "gasstation-payinapp"
    let couponsEnvironment: CouponsEnvironment = .prod
    
    func advertKind(forPageId: String) -> SearchAdvertKind {
        return .default
    }
    
    func menuPageId(forMenuPage menuPage: SearchMenuPage) -> String {
        switch menuPage {
        case .search: return "mobile_maps_search"
        case .automobileGuidance: return "mobile_maps_automobile_guidance"
        case .suggest: return "mobile_maps_suggest"
        case .historySuggest: return "mobile_maps_history_suggest"
        case .carplay: return "mobile_maps_carplay_search_datatesting"
        }
    }
}
