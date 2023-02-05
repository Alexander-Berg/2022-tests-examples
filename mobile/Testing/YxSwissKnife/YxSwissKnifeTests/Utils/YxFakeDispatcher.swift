//
//  YxFakeDispatcher.swift
//  YxSwissKnifeTests
//
//  Created by Denis Malykh on 23.08.2018.
//  Copyright © 2018 Yandex. All rights reserved.
//

import Foundation
import YxSwissKnife

final class YxFakeDispatcher : YxDispatching {

    typealias Block = () -> Void

    private var asyncBlocks = [Block]()

    func sync<T>(flags: DispatchWorkItemFlags, execute: () throws -> T) rethrows -> T {
        return try execute()
    }

    func async(flags: DispatchWorkItemFlags, execute: @escaping () -> Void) {
        asyncBlocks.append(execute)
    }

    func executeAsync() {
        asyncBlocks.forEach { $0() }
        asyncBlocks.removeAll()
    }

    func executeFirst() {
        let block = asyncBlocks.remove(at: 0)
        block()
    }

}
