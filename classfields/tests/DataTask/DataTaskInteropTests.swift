//
//  Created by Alexey Aleshkov on 11/02/2019.
//  Copyright © 2019 Alexey Aleshkov. All rights reserved.
//

import XCTest
import YRECoreUtils
import YREServiceLayer

final class DataTaskInteropTests: XCTestCase {
    func testProducerDoComplete() {
        let producerTask: DataTask<Void, Error> = .idle()

        var interopResult: Bool = false
        _ = producerTask.dataLayerTask(completion: { _, result in
            if case .succeeded = result {
                interopResult = true
            }
            else {
                interopResult = false
            }
        })

        producerTask.start(progress: nil)
        producerTask.complete(with: .succeeded(()))

        XCTAssert(interopResult)
    }

    func testProducerDoCancel() {
        let producerTask: DataTask<Void, Error> = .idle()

        var interopResult: Bool = false
        _ = producerTask.dataLayerTask(completion: { _, result in
            if case .cancelled = result {
                interopResult = true
            }
            else {
                interopResult = false
            }
        })

        producerTask.start(progress: nil)
        producerTask.cancel()

        XCTAssert(interopResult)
    }
}
