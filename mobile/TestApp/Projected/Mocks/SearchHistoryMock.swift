//
//  SearchHistoryMock.swift
//  ProjectedLibTestApp
//
//  Created by Alexander Shchavrovskiy on 26.02.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import YandexMapsSearch
import YandexMapsCommonTypes
import RxSwift

final class SearchHistoryMock: SearchHistory {
    lazy var items: [SearchHistoryItem] = {
        [
            SearchHistoryItem(
                id: UUID().uuidString,
                searchText: "поесть",
                displayText: "Где поесть",
                uri: nil,
                lastUsed: Date()
            )
        ]
    }()
    
    let isClearing: Bool = false
    var updates: Observable<Void> = .never()
    
    func addItems(_ items: [SearchHistorySummaryItem]) {}
    func deleteItem(id: String) {}
    func clear(_ completion: (() -> Void)?) {}
    func sync() {}
}
