//
//  ListAdapterTestImpl.swift
//  YandexGeoToolboxTestApp
//
//  Created by Ilya Lobanov on 05/05/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation


class ListAdapterTestImpl: ListAdapter {
    
    private(set) var impl: [WrappedValue] = []
    
    init(_ impl: [WrappedValue] = []) {
        self.impl = impl
    }
    
    func makeCopy() -> ListAdapterTestImpl {
        return ListAdapterTestImpl(impl)
    }
    
    // MARK: ListAdapter
    
    private(set) var valid: Bool = true
    var count: Int {
        return impl.count
    }
    
    func value(_ index: Int) -> WrappedValue {
        return impl[index]
    }
    
    func set(_ value: WrappedValue, atIndex index: Int) {
        impl[index] = value
    }
    
    func generate() -> ListGenerator {
        return ListGenerator(ListGeneratorTypeTestImpl(impl.makeIterator()))
    }
    
    func append(_ value: WrappedValue) {
        impl.append(value)
    }
    
    func insert(_ value: WrappedValue, atIndex index: Int) {
        impl.insert(value, at: index)
    }
    
    func delete(_ index: Int) {
        impl.remove(at: index)
    }
    
    func move(fromIndex: Int, toIndex: Int) {
        let elem = impl.remove(at: fromIndex)
        impl.insert(elem, at: toIndex)
    }
    
}


fileprivate class ListGeneratorTypeTestImpl: ListGeneratorType {
    
    fileprivate var impl: IndexingIterator<[WrappedValue]>
    
    init(_ impl: IndexingIterator<[WrappedValue]>) {
        self.impl = impl
    }
    
    func next() -> WrappedValue? {
        return impl.next()
    }
    
}
