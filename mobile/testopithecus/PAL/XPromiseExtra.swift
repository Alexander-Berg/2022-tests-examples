//
// Created by Fedor Amosov on 17/02/2020.
// Copyright (c) 2020 Yandex. All rights reserved.
//

import Foundation
import testopithecus
import Utils

extension XPromise {
    public var syncGet: Utils.Result<T> {
        assert(Thread.isMainThread)

        let sema = DispatchSemaphore(value: 0)
        var result: T?
        var promiseErrror: YSError?
        self.then { promiseResult in
            result = promiseResult
            sema.signal()
        }.failed { err in
            promiseErrror = err
            sema.signal()
        }
        sema.wait()

        if let err = promiseErrror {
            return Result.failure(err)
        }
        return Result.success(result!)
    }
}
