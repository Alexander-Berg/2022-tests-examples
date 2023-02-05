//
//  Helpers.swift
//  MetroKitTestApp
//
//  Created by Konstantin Kiselev on 19/10/2017.
//  Copyright © 2017 Yandex LLC. All rights reserved.
//

import Foundation

@discardableResult func apply<T>(_ obj: T, _ block: (T) -> Void) -> T {
    block(obj)
    return obj
}

func applyBlock<T>(_ obj: T, _ block: (T) -> Void) -> T {
    return apply(obj, block)
}

#if swift(>=4.1)
#else
extension Sequence {

    func compactMap<ElementOfResult>(_ transform: (Self.Element) throws -> ElementOfResult?) rethrows -> [ElementOfResult] {
        return try flatMap(transform)
    }

}
#endif
