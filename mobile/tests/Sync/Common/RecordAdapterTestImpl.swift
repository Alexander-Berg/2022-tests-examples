//
//  RecordAdapterTestImpl.swift
//  YandexGeoToolboxTestApp
//
//  Created by Ilya Lobanov on 05/05/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation


class RecordAdapterTestImpl: RecordAdapter {
    
    enum ValueImpl: Equatable {
        case wrappedImpl(WrappedValue)
        case listImpl(ListAdapterTestImpl)
        
        func makeCopy() -> ValueImpl {
            switch self {
            case .wrappedImpl(let value):
                return .wrappedImpl(value)
            case .listImpl(let list):
                return .listImpl(list)
            }
        }
    }
    
    private(set) var impl: [String: ValueImpl]
    
    init(id: String, impl: [String: ValueImpl] = [:]) {
        self.id = id
        self.impl = impl
    }
    
    func makeCopy() -> RecordAdapterTestImpl {
        return RecordAdapterTestImpl(id: id, impl: impl.map { ($0, $1.makeCopy()) })
    }
    
    // MARK: RecordAdapter
    
    private(set) var id: String
    private(set) var valid: Bool = true
    private(set) var deleted: Bool = false
    
    func value(_ key: String) -> WrappedValue? {
        if let value = impl[key] {
            if case let .wrappedImpl(wrapped) = value {
                return wrapped
            }
        }
        return nil
    }
    
    func set(_ value: WrappedValue, forKey key: String) {
        impl[key] = .wrappedImpl(value)
    }
    
    func list(_ key: String) -> List? {
        if let value = impl[key] {
            if case let .listImpl(adapter) = value {
                return List(adapter: adapter)
            }
        }
        return nil
    }
    
    func listModifier(_ key: String) -> ListModifier? {
        if let value = impl[key] {
            if case let .listImpl(adapter) = value {
                return ListModifier(adapter: adapter)
            }
        }
        return nil
    }
    
    func createList(_ key: String) -> ListModifier {
        let adapter = ListAdapterTestImpl()
        impl[key] = .listImpl(adapter)
        return ListModifier(adapter: adapter)
    }
    
    func deleteField(_ key: String) {
        impl.removeValue(forKey: key)
    }
    
    func isEqual(_ to: RecordAdapter) -> Bool {
        if let to = to as? RecordAdapterTestImpl {
            for key in impl.keys {
                if impl[key] != to.impl[key] {
                    return false
                }
            }
            return true
        }
        return false
    }
    
}


func ==(lhs: RecordAdapterTestImpl.ValueImpl, rhs: RecordAdapterTestImpl.ValueImpl) -> Bool {
    if case let .wrappedImpl(lhsValue) = lhs, case let .wrappedImpl(rhsValue) = rhs {
        return lhsValue == rhsValue
    }
    if case let .listImpl(lhsValue) = lhs, case let .listImpl(rhsValue) = rhs {
        return List(adapter: lhsValue) == List(adapter: rhsValue)
    }
    return false
}


fileprivate func ==(lhs: List, rhs: List) -> Bool {
    if lhs.count != rhs.count {
        return false
    }
    
    for i in lhs.indices where lhs[i] != rhs[i] {
        return false
    }
    
    return true
}


