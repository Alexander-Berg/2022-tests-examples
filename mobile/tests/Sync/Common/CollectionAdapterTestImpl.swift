//
//  CollectionAdapterTestImpl.swift
//  YandexGeoToolboxTestApp
//
//  Created by Ilya Lobanov on 05/05/16.
//  Copyright © 2016 Yandex LLC. All rights reserved.
//

import Foundation


class CollectionAdapterTestImpl: CollectionAdapter {
    
    private(set) var impl: [String: RecordAdapterTestImpl]
    
    init(id: String, impl: [String: RecordAdapterTestImpl] = [:]) {
        self.id = id
        self.impl = impl
    }
    
    func makeCopy() -> CollectionAdapterTestImpl {
        return CollectionAdapterTestImpl(id: id, impl: impl.map { ($0, $1.makeCopy()) })
    }
    
    // MARK: CollectionAdapter
    
    private(set) var id: String
    private(set) var valid: Bool = true
    
    var records: RecordSequence {
        return RecordSequence(RecordSequenceTypeTestImpl(impl))
    }
    
    func record(_ id: String) -> Record? {
        return impl[id] >>= Record.init
    }
    
    func insertRecord() -> RecordModifier {
        return insertRecord(NSUUID().uuidString)
    }
    
    func insertRecord(_ id: String) -> RecordModifier {
        let adapter = RecordAdapterTestImpl(id: id)
        impl[id] = adapter
        return RecordModifier(adapter: adapter)
    }
    
    func recordModifier(_ id: String) -> RecordModifier? {
        return impl[id] >>= RecordModifier.init
    }
    
    func deleteRecord(_ id: String) {
        impl[id] = nil
    }
    
    func clear() {
        impl.removeAll()
    }
    
}


fileprivate class RecordSequenceTypeTestImpl: RecordSequenceType {
    
    fileprivate let impl: [String: RecordAdapterTestImpl]
    
    init(_ impl: [String: RecordAdapterTestImpl]) {
        self.impl = impl
    }
    
    func generate() -> RecordGenerator {
        return RecordGenerator(RecordGeneratorTypeTestImpl(impl.makeIterator()))
    }
    
}


fileprivate class RecordGeneratorTypeTestImpl: RecordGeneratorType {
    
    fileprivate var impl: DictionaryIterator<String, RecordAdapterTestImpl>
    
    init(_ impl: DictionaryIterator<String, RecordAdapterTestImpl>) {
        self.impl = impl
    }
    
    func next() -> Record? {
        return impl.next()?.1 >>= Record.init
    }
    
}
