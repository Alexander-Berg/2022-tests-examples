//
//  Created by Alexey Aleshkov on 11/02/2019.
//  Copyright © 2019 Alexey Aleshkov. All rights reserved.
//

import XCTest
import YRECoreUtils

final class DataTaskSelfTests: XCTestCase {
    func testCancelOnDeinit() {
        var producerResult: Bool = false

        // hold state subscription to make it's lifetime longer than task lifetime
        let token: DataTask<Void, Error>.CancellationToken
        do {
            // scope task to `deinit` it
            let producerTask: DataTask<Void, Error> = .idle()

            producerTask.start(progress: nil)

            token = producerTask.addStateObserver({ _, state in
                if case .completed(let taskResult) = state {
                    if case .cancelled = taskResult {
                        producerResult = true
                    }
                    else {
                        producerResult = false
                    }
                }
            })
        }
        // suppress "Immutable value was never used" warning
        _ = token

        XCTAssert(producerResult)
    }
}
