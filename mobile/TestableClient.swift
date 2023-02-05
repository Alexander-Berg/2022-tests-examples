//
//  TestableClient.swift
//  YandexTransport
//
//  Created by Yury Potapov on 26.04.17.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import Foundation

protocol TestableClientType {
    associatedtype FetchOpReturnType
    
    var fetchOp: FetchOperation<FetchOpReturnType, Error?> { get set }
}

class TestableClient<T>: TestableClientType {
    internal typealias FetchOpReturnType = T
    var fetchOp: FetchOperation<FetchOpReturnType, Error?>
    
    init() { fetchOp = FetchOperation<FetchOpReturnType, Error?>(fetch: { _ in return nil }, cancel: nil) }
}
