//
//  SearchHistoryViewModel.swift
//  YandexGeoSync
//
//  Created by Ilya Lobanov on 03/02/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation


class SearchHistoryViewModel: DatabaseViewModel {
    
    private let database: SearchHistoryDatabase
    private var snapshot: SearchHistorySnapshot? = nil
    private var items: [SearchHistoryItem] = []
    
    init(database: SearchHistoryDatabase) {
        self.database = database
    }
    
    // MARK: DatabaseViewModel

    var onDidUpdate: (() -> Void)? = nil
    var numberOfItems: Int { return items.count }
    var headerTitle: String { return "Search history (\(numberOfItems))" }
    
    func update() {
        database.sync { [weak self] error in
            guard let slf = self else { return }
            
            if let error = error {
                print(error.localizedDescription)
                slf.snapshot = nil
                slf.updateWithCurrentSnapshot()
            }
            
            slf.database.openSnapshot { snapshot, error in
                if let error = error {
                    print(error.localizedDescription)
                }
                slf.snapshot = snapshot
                slf.updateWithCurrentSnapshot()
            }
        }
    }
    
    func cellInfo(at index: Int) -> SyncTableViewCell.Info {
        let item = items[index]
        var str = "\(item.id) {"
        str += "\n    displayText: \(item.displayText)"
        str += "\n    lastUsed: \(item.lastUsed)"
        str += "\n    uri: \(item.uri)"
        str += "\n}"
        
        return SyncTableViewCell.Info(title: str)
    }
    
    // MARK: Private
    
    private func updateWithCurrentSnapshot() {
        items = snapshot?.items ?? []
        onDidUpdate?()
    }
    
}
