//
//  DatabaseAdapterTestImpl.swift
//  YandexGeoToolboxTestApp
//
//  Created by Ilya Lobanov on 05/05/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation


class DatabaseAdapterTestImpl: DatabaseAdapter {
 
    private(set) var snapshotImpl: [String: CollectionAdapterTestImpl]
    let snapshotAdapters: WeakObjectCollection<SnapshotAdapterTestImpl>
    
    init(id: String) {
        self.id = id
        self.snapshotImpl = [:]
        self.snapshotAdapters = WeakObjectCollection<SnapshotAdapterTestImpl>()
    }
    
    func syncSnapshot(snapshotAadapter: SnapshotAdapterTestImpl) {
        snapshotImpl = snapshotAadapter.impl.map { ($0, $1.makeCopy()) }
        for adapter in snapshotAdapters.array() where adapter !== snapshotAadapter {
            adapter.outdate()
        }
    }
    
    func reset() {
        self.snapshotImpl.removeAll()
        for adapter in self.snapshotAdapters.array() {
            adapter.invalidate()
        }
    }
    
    // MARK: DatabaseAdapter
    
    private(set) var id: String
    private(set) var valid: Bool = true
    private(set) var lastSync: Date? = nil
    var syncInterval: TimeInterval = 0.0
    
    func openSnapshot(_ completion: @escaping DatabaseOpenSnapshotCompletion) {
        DispatchQueue.main.async {
            let impl: [String: CollectionAdapterTestImpl] = self.snapshotImpl.map { ($0, $1.makeCopy()) }
            let adapter = SnapshotAdapterTestImpl(datbaseAdapter: self, impl: impl)
            self.snapshotAdapters.insert(adapter)
            completion(Snapshot(adapter: adapter), nil)
        }
    }
    
    func sync(_ completion: @escaping DatabaseSyncCompletion) {
        DispatchQueue.main.async {
            completion(nil)
        }
    }
    
    func reset(_ completion: @escaping DatabaseResetCompletion) {
        DispatchQueue.main.async {
            self.reset()
            completion(nil)
        }
    }
    
    func setResolutionRule(_ rule: DatabaseFieldResolutionRule, forCollectionWithId collectionId: String, fieldName: String) {
        assert(false, "method is not implemented in test realization")
    }
    
}
