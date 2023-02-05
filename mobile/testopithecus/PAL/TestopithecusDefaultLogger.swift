//
// Created by Fedor Amosov on 2019-08-02.
// Copyright (c) 2019 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public final class TestopithecusDefaultLogger: testopithecus.Logger {
    public static let instance = TestopithecusDefaultLogger()

    public func warn(_ message: String) {
        print(message)
    }
    
    public func info(_ message: String) {
        print(message)
    }
    
    public func error(_ message: String) {
        print(message)
    }
}
