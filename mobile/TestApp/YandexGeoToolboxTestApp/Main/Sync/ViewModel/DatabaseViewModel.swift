//
//  DatabaseViewModel.swift
//  YandexGeoSync
//
//  Created by Ilya Lobanov on 28/01/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation


protocol DatabaseViewModel {
    var onDidUpdate: (() -> Void)? { get set }
    var numberOfItems: Int { get }
    var headerTitle: String { get }
    
    func update()
    func cellInfo(at index: Int) -> SyncTableViewCell.Info
}


class DefaultDatabaseViewModel: DatabaseViewModel {
    
    private let database: Database
    private let collectionID: String
    private var snapshot: Snapshot? = nil
    private var collection: Collection? = nil
    private var infos: [SyncTableViewCell.Info] = []
    
    init(database: Database, collectionID: String) {
        self.database = database
        self.collectionID = collectionID
    }
    
    // MARK: DatabaseViewModel 
    
    var onDidUpdate: (() -> Void)?
    var numberOfItems: Int { return infos.count }
    var headerTitle: String { return database.id }
    
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
        return infos[index]
    }
    
    // MARK: Private
    
    private func updateWithCurrentSnapshot() {
        collection = snapshot?.collection(id: collectionID)
        infos = (collection?.records.map { SyncTableViewCell.Info(title: $0.id) }) ?? []
        onDidUpdate?()
    }
    
}
