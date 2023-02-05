//
//  ImportantPlacesViewModel.swift
//  YandexGeoSync
//
//  Created by Ilya Lobanov on 29/02/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation


class ImportantPlacesViewModel: DatabaseViewModel {
    
    private let database: ImportantPlacesDatabase
    private var snapshot: ImportantPlacesSnapshot? = nil
    private var items: [ImportantPlace] = []
    
    init(database: ImportantPlacesDatabase) {
        self.database = database
    }
    
    // MARK: DatabaseViewModel
    
    var onDidUpdate: (() -> Void)? = nil
    var numberOfItems: Int { return items.count }
    var headerTitle: String { return "Important places" }
    
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
        var str = "\(item.kind) {"
        str += "\n    title: \(item.title)"
        str += "\n    latitude: \(item.latitude)"
        str += "\n    longitude: \(item.longitude)"
        str += "\n    lastUsed: \(item.created)"
        str += "\n    lastUsed: \(item.modified)"
        str += "\n    lastUsed: \(item.lastUsed)"
        if let tags = item.tags {
            str += "\n    tags: \(tags)"
        }
        if let address = item.address {
            str += "\n    address: \(address)"
        }
        if let shortAddress = item.shortAddress {
            str += "\n    shortAddress: \(shortAddress)"
        }
        str += "\n}"
        
        return SyncTableViewCell.Info(title: str)
    }
    
    // MARK: Private
    
    private func updateWithCurrentSnapshot() {
        items = (snapshot?.importantPlaces.map { $0.1 }) ?? []
        onDidUpdate?()
    }
    
}
