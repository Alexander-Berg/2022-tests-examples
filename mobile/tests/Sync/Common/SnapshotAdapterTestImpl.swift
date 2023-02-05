//
//  SnapshotAdapterTestImpl.swift
//  YandexGeoToolboxTestApp
//
//  Created by Ilya Lobanov on 05/05/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation


class SnapshotAdapterTestImpl: SnapshotAdapter {
    
    private(set) var impl: [String: CollectionAdapterTestImpl]
    weak var datbaseAdapter: DatabaseAdapterTestImpl?
    
    init(datbaseAdapter: DatabaseAdapterTestImpl, impl: [String: CollectionAdapterTestImpl] = [:]) {
        self.datbaseAdapter = datbaseAdapter
        self.impl = impl
    }
    
    func outdate() {
        outdated = true
        delegate?.snapshotAdapterDidBecomeOutdated(self)
    }
    
    func invalidate() {
        valid = false
        delegate?.snapshotAdapterDidInvalidate(self)
    }
    
    // MARK: SnapshotAdapter
    
    private(set) var valid: Bool = true
    private(set) var outdated: Bool = false
    private(set) var editing: Bool = false
    weak var delegate: SnapshotAdapterDelegate?
    
    func collection(id: String) -> Collection {
        if let collection = impl[id] {
            return Collection(adapter: collection)
        } else {
            let adapter = CollectionAdapterTestImpl(id: id)
            impl[id] = adapter
            let collection = Collection(adapter: adapter)
            return collection
        }
    }
    
    func beginEditing() {
        editing = true
    }
    
    func endEditing() {
        editing = false
    }
    
    func sync() {
        datbaseAdapter?.syncSnapshot(snapshotAadapter: self)
        impl = datbaseAdapter?.snapshotImpl ?? [:]
        outdated = false
    }
    
}
