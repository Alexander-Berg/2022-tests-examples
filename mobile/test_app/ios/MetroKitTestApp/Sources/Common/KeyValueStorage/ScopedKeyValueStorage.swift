//
//  ScopedKeyValueStorage.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 02/02/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation

public final class ScopedKeyValueStorage: KeyValueStorage {
    
    public init(_ storage: KeyValueStorage, scope: String) {
        self.storage = storage
        self.scope = scope
    }
    
    public subscript<T>(key: String) -> T? {
        get { return storage[scopedKey(key)] }
        set { storage[scopedKey(key)] = newValue }
    }

    // MARK: - Private
    
    private let storage: KeyValueStorage
    private let scope: String
    
    private func scopedKey(_ key: String) -> String {
        return scope + "." + key
    }
    
}

public extension KeyValueStorage {
    
    public func makeScoped(_ scope: String) -> ScopedKeyValueStorage {
        return ScopedKeyValueStorage(self, scope: scope)
    }
    
}
