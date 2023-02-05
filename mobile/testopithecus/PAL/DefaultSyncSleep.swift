//
// Created by Fedor Amosov on 11/12/2019.
// Copyright (c) 2019 Yandex. All rights reserved.
//

import Foundation
import testopithecus

public class DefaultSyncSleep: SyncSleep {
    public func sleepMs(_ milliseconds: Int32) {
        sleep(UInt32(milliseconds / 1000))
    }
}
