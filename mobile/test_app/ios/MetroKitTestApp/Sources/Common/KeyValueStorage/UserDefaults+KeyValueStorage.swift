//
//  UserDefaults+KeyValueStorage.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 02/02/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation

extension UserDefaults: KeyValueStorage {
    
    public subscript<T>(key: String) -> T? {
        get {
            return object(forKey: key) as? T
        }
        set(newValue) {
            if let v = newValue {
                set(v, forKey: key)
            } else {
                removeObject(forKey: key)
            }
        }
    }
    
}
