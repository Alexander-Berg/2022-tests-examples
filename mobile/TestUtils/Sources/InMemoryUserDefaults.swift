//
//  InMemoryUserDefaults.swift
//  TestUtils
//
//  Created by Timur Turaev on 31.05.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import Utils

public class InMemoryUserDefaults: NSObject, YOUserDefaults {
    public private(set) var memory: [String: Any] = [:]

    public func object(forKey key: String) -> Any? {
        return self.memory[key]
    }

    public func double(forKey key: String) -> Double {
        return (self.memory[key] as? Double) ?? 0
    }

    public func string(forKey key: String) -> String? {
        return (self.memory[key] as? String) ?? ""
    }

    public func integer(forKey key: String) -> Int {
        return (self.memory[key] as? Int) ?? 0
    }

    public func yo_bool(forKey key: String, defaultValue: Bool) -> Bool {
        return (self.memory[key] as? Bool) ?? defaultValue
    }

    public func bool(forKey key: String) -> Bool {
        self.yo_bool(forKey: key, defaultValue: false)
    }

    public func setObject(_ value: Any?, forKey key: String) {
        self.memory[key] = value
    }

    public func setInteger(_ value: Int, forKey key: String) {
        self.memory[key] = value
    }

    public func setBool(_ value: Bool, forKey key: String) {
        self.memory[key] = value
    }

    public func removeObject(forKey key: String) {
        self.memory.removeValue(forKey: key)
    }

    public func dictionaryRepresentation() -> [String: Any] {
        return self.memory
    }

    public func yo_synchronize() -> Bool {
        // do nothing
        return true
    }
}
