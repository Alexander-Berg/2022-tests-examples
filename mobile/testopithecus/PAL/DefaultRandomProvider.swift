//
// Created by Fedor Amosov on 2019-08-02.
// Copyright (c) 2019 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public class DefaultRandomProvider: RandomProvider {
    public func generate(_ n: Int32) -> Int32 {
        Int32.random(in: 0..<n)
    }
}
