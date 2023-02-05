//
//  RoutingHistoryViewModel.swift
//  YandexGeoSync
//
//  Created by Ilya Lobanov on 03/02/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation


class RoutingHistoryViewModel: DatabaseViewModel {
    
    private let database: RoutingHistoryDatabase
    private var snapshot: RoutingHistorySnapshot? = nil
    private var items: [RoutingHistoryItem] = []
    
    init(database: RoutingHistoryDatabase) {
        self.database = database
    }
    
    // MARK: DatabaseViewModel

    var onDidUpdate: (() -> Void)? = nil
    var numberOfItems: Int { return items.count }
    var headerTitle: String { return "Routing history (\(numberOfItems))" }
    
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
                
                snapshot?.beginEditing()
                snapshot?.clear()
                snapshot?.endEditing()
                snapshot?.sync()
                
                slf.updateWithCurrentSnapshot()
            }
        }
    }
    
    func cellInfo(at index: Int) -> SyncTableViewCell.Info {
        let item = items[index]
        var str = "\(item.id) {"
        str += "\n    title: \(item.title)"
        str += "\n    description: \(item.description)"
        str += "\n    latitude: \(item.latitude)"
        str += "\n    longitude: \(item.longitude)"
        str += "\n    lastUsed: \(item.lastUsed)"
        if let uri = item.uri {
            str += "\n    uri: \(uri)"
        }
        str += "\n}"
        
        return SyncTableViewCell.Info(title: str)
    }
    
    // MARK: Private
    
    private func updateWithCurrentSnapshot() {
        items = snapshot?.items ?? []
        onDidUpdate?()
    }
    
}
