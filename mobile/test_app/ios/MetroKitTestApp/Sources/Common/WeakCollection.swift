//
//  WeakCollection.swift
//  MetroToolbox
//
//  Created by Ilya Lobanov on 24/01/2018.
//  Copyright © 2018 Yandex LLC. All rights reserved.
//

import Foundation

public struct WeakCollection<T> {

    public init() {
        elems = []
    }

    public func forEach(_ block: (T) -> Void) {
        for elem in elems {
            if let obj = elem.object as? T {
                block(obj)
            }
        }
    }
    
    public mutating func insert(_ elem: T) {
        removeNilElems()
        if index(of: elem) == nil {
            elems.append(Box(elem as AnyObject))
        }
    }
    
    public mutating func remove(_ elem: T) {
        removeNilElems()
        if let index = index(of: elem) {
            elems.remove(at: index)
        }
    }

    public mutating func isEmpty() -> Bool {
        removeNilElems()
        return elems.isEmpty
    }

    // MARK: - Private
    
    private class Box {
        weak var object: AnyObject?
        
        init(_ object: AnyObject) {
            self.object = object
        }
    }

    private var elems: ContiguousArray<Box>
    
    private mutating func removeNilElems() {
        elems = elems.filter { $0.object != nil }
    }
    
    private func index(of elem: T) -> Int? {
        return elems.index(where: { $0.object === elem as AnyObject })
    }
    
}

