//
//  Created by Timur Turaev on 12.05.2021.
//  Copyright © 2021 Yandex. All rights reserved.
//

import Foundation
import XCTest
import Utils

internal final class OnceTokenTest: XCTestCase {
    func testExecute() {
        var callsCounter = 0

        let token = OnceToken()
        token.execute {
            callsCounter += 1
        }
        token.execute {
            callsCounter += 1
        }
        token.execute {
            callsCounter += 1
        }

        XCTAssertEqual(callsCounter, 1)
    }

    func testConcurrentCalls() {
        @Atomic var onceCallsCounter = 0
        let once = OnceToken()

        let opQueue = OperationQueue()
        opQueue.qualityOfService = .userInitiated

        opQueue.isSuspended = true
        100.times {
            opQueue.addOperation {
                once.execute { $onceCallsCounter.change { $0 += 1 } }
            }
        }
        opQueue.isSuspended = false

        self.wait(for: { opQueue.operationCount == 0 }, timeout: 1)
        XCTAssertEqual(onceCallsCounter, 1)
    }
}
