//
//  Created by Alexey Aleshkov on 11/02/2019.
//  Copyright © 2019 Alexey Aleshkov. All rights reserved.
//

import XCTest
import YRECoreUtils

final class DataTaskShareTests: XCTestCase {
    func testIdleProducerComplete() {
        let producerTask: DataTask<Void, Error> = .idle()
        let consumerTask1 = producerTask.share()
        let consumerTask2 = producerTask.share()

        producerTask.start(progress: nil)
        producerTask.complete(with: .succeeded(()))

        let consumer1Result: Bool
        if case .completed(.succeeded) = consumerTask1.state {
            consumer1Result = true
        }
        else {
            consumer1Result = false
        }
        XCTAssert(consumer1Result)

        let consumer2Result: Bool
        if case .completed(.succeeded) = consumerTask2.state {
            consumer2Result = true
        }
        else {
            consumer2Result = false
        }
        XCTAssert(consumer2Result)
    }

    func testCompletedProducerComplete() {
        let producerTask: DataTask<Void, Error> = .completed(with: .succeeded(()))
        let consumerTask1 = producerTask.share()
        let consumerTask2 = producerTask.share()

        let consumer1Result: Bool
        if case .completed(.succeeded) = consumerTask1.state {
            consumer1Result = true
        }
        else {
            consumer1Result = false
        }
        XCTAssert(consumer1Result)

        let consumer2Result: Bool
        if case .completed(.succeeded) = consumerTask2.state {
            consumer2Result = true
        }
        else {
            consumer2Result = false
        }
        XCTAssert(consumer2Result)
    }

    func testIdleProducerCancel() {
        let producerTask: DataTask<Void, Error> = .idle()
        let consumerTask1 = producerTask.share()
        let consumerTask2 = producerTask.share()

        producerTask.start(progress: nil)
        producerTask.cancel()

        let consumer1Result: Bool
        if case .completed(.cancelled) = consumerTask1.state {
            consumer1Result = true
        }
        else {
            consumer1Result = false
        }
        XCTAssert(consumer1Result)

        let consumer2Result: Bool
        if case .completed(.cancelled) = consumerTask2.state {
            consumer2Result = true
        }
        else {
            consumer2Result = false
        }
        XCTAssert(consumer2Result)
    }

    func testIdleProducerCompleteSharedConsumerCancel() {
        let producerTask: DataTask<Void, Error> = .idle()
        let consumerTask1 = producerTask.share()
        let consumerTask2 = producerTask.share()

        producerTask.start(progress: nil)
        consumerTask1.cancel()
        producerTask.complete(with: .succeeded(()))

        let consumer1Result: Bool
        if case .completed(.cancelled) = consumerTask1.state {
            consumer1Result = true
        }
        else {
            consumer1Result = false
        }
        XCTAssert(consumer1Result)

        let consumer2Result: Bool
        if case .completed(.succeeded) = consumerTask2.state {
            consumer2Result = true
        }
        else {
            consumer2Result = false
        }
        XCTAssert(consumer2Result)
    }

    func testIdleProducerInProgressLinkedConsumerCancel() {
        let producerTask: DataTask<Void, Error> = .idle()
        let consumerTask1 = producerTask.share()
        let consumerTask2 = producerTask.share()

        consumerTask1.addCancellationHandler(producerTask.cancel)

        producerTask.start(progress: nil)
        consumerTask1.cancel()

        let producerResult: Bool
        if case .completed(.cancelled) = producerTask.state {
            producerResult = true
        }
        else {
            producerResult = false
        }
        XCTAssert(producerResult)

        let consumer1Result: Bool
        if case .completed(.cancelled) = consumerTask1.state {
            consumer1Result = true
        }
        else {
            consumer1Result = false
        }
        XCTAssert(consumer1Result)

        let consumer2Result: Bool
        if case .completed(.cancelled) = consumerTask2.state {
            consumer2Result = true
        }
        else {
            consumer2Result = false
        }
        XCTAssert(consumer2Result)
    }
}
