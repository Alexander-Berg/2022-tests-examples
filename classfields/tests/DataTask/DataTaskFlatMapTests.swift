//
//  Created by Alexey Aleshkov on 11/02/2019.
//  Copyright © 2019 Alexey Aleshkov. All rights reserved.
//

import XCTest
import YRECoreUtils

final class DataTaskFlatMapTests: XCTestCase {
    func testWhenDoCompleteThenDoComplete() {
        let whenTask: DataTask<Void, Error> = .idle()
        let thenTask: DataTask<Void, Error> = .idle()
        let consumerTask = whenTask.flatMap({ _ in thenTask })

        whenTask.start(progress: nil)
        whenTask.complete(with: .succeeded(()))

        thenTask.start(progress: nil)
        thenTask.complete(with: .succeeded(()))

        let consumerResult: Bool
        if case .completed(.succeeded) = consumerTask.state {
            consumerResult = true
        }
        else {
            consumerResult = false
        }
        XCTAssert(consumerResult)
    }

    func testThenDoCompleteWhenDoComplete() {
        let whenTask: DataTask<Void, Error> = .idle()
        let thenTask: DataTask<Void, Error> = .idle()
        let consumerTask = whenTask.flatMap({ _ in thenTask })

        thenTask.start(progress: nil)
        thenTask.complete(with: .succeeded(()))

        whenTask.start(progress: nil)
        whenTask.complete(with: .succeeded(()))

        let consumerResult: Bool
        if case .completed(.succeeded) = consumerTask.state {
            consumerResult = true
        }
        else {
            consumerResult = false
        }
        XCTAssert(consumerResult)
    }

    func testWhenDoCompleteThenDoCancell() {
        let whenTask: DataTask<Void, Error> = .idle()
        let thenTask: DataTask<Void, Error> = .idle()
        let consumerTask = whenTask.flatMap({ _ in thenTask })

        whenTask.start(progress: nil)
        whenTask.complete(with: .succeeded(()))

        thenTask.start(progress: nil)
        thenTask.cancel()

        let consumerResult: Bool
        if case .completed(.cancelled) = consumerTask.state {
            consumerResult = true
        }
        else {
            consumerResult = false
        }
        XCTAssert(consumerResult)
    }

    func testWhenDoCancellThenDoComplete() {
        let whenTask: DataTask<Void, Error> = .idle()
        let thenTask: DataTask<Void, Error> = .idle()
        let consumerTask = whenTask.flatMap({ _ in thenTask })

        whenTask.start(progress: nil)
        whenTask.cancel()

        thenTask.start(progress: nil)
        thenTask.complete(with: .succeeded(()))

        let consumerResult: Bool
        if case .completed(.cancelled) = consumerTask.state {
            consumerResult = true
        }
        else {
            consumerResult = false
        }
        XCTAssert(consumerResult)
    }

    func testWhenDoProgressThenInIdle() {
        let whenTask: DataTask<Void, Error> = .idle()
        let thenTask: DataTask<Void, Error> = .idle()
        let consumerTask = whenTask.flatMap({ _ in thenTask })

        whenTask.start(progress: nil)

        let consumerResult: Bool
        if case .running = consumerTask.state {
            consumerResult = true
        }
        else {
            consumerResult = false
        }
        XCTAssert(consumerResult)
    }

    func testWhenDoCompleteThenInIdle() {
        let whenTask: DataTask<Void, Error> = .idle()
        let thenTask: DataTask<Void, Error> = .idle()
        let consumerTask = whenTask.flatMap({ _ in thenTask })

        whenTask.start(progress: nil)
        whenTask.complete(with: .succeeded(()))

        let consumerResult: Bool
        if case .running = consumerTask.state {
            consumerResult = true
        }
        else {
            consumerResult = false
        }
        XCTAssert(consumerResult)
    }

    func testWhenInCompleteThenInComplete() {
        let whenTask: DataTask<Void, Error> = .completed(with: .succeeded(()))
        let thenTask: DataTask<Void, Error> = .completed(with: .succeeded(()))
        let consumerTask = whenTask.flatMap({ _ in thenTask })

        let consumerResult: Bool
        if case .completed(.succeeded) = consumerTask.state {
            consumerResult = true
        }
        else {
            consumerResult = false
        }
        XCTAssert(consumerResult)
    }

    func testWhenInIdleThenInIdle() {
        let whenTask: DataTask<Void, Error> = .idle()
        let thenTask: DataTask<Void, Error> = .idle()
        let consumerTask = whenTask.flatMap({ _ in thenTask })

        let consumerResult: Bool
        if case .idle = consumerTask.state {
            consumerResult = true
        }
        else {
            consumerResult = false
        }
        XCTAssert(consumerResult)
    }

    func testWhenDoProgressThenInIdleConsumerDoCancel() {
        let whenTask: DataTask<Void, Error> = .idle()
        let thenTask: DataTask<Void, Error> = .idle()
        let consumerTask = whenTask.flatMap({ _ in thenTask })

        whenTask.start(progress: nil)

        consumerTask.cancel()

        let whenResult: Bool
        if case .completed(.cancelled) = whenTask.state {
            whenResult = true
        }
        else {
            whenResult = false
        }
        XCTAssert(whenResult)

        let thenResult: Bool
        if case .idle = thenTask.state {
            thenResult = true
        }
        else {
            thenResult = false
        }
        XCTAssert(thenResult)

        let consumerResult: Bool
        if case .completed(.cancelled) = consumerTask.state {
            consumerResult = true
        }
        else {
            consumerResult = false
        }
        XCTAssert(consumerResult)
    }

    func testWhenDoCompleteThenDoProgressConsumerDoCancel() {
        let whenTask: DataTask<Void, Error> = .idle()
        let thenTask: DataTask<Void, Error> = .idle()
        let consumerTask = whenTask.flatMap({ _ in thenTask })

        whenTask.start(progress: nil)
        whenTask.complete(with: .succeeded(()))

        thenTask.start(progress: nil)

        consumerTask.cancel()

        let whenResult: Bool
        if case .completed(.cancelled) = whenTask.state {
            whenResult = true
        }
        else {
            whenResult = false
        }
        XCTAssert(whenResult)

        let thenResult: Bool
        if case .completed(.cancelled) = thenTask.state {
            thenResult = true
        }
        else {
            thenResult = false
        }
        XCTAssert(thenResult)

        let consumerResult: Bool
        if case .completed(.cancelled) = consumerTask.state {
            consumerResult = true
        }
        else {
            consumerResult = false
        }
        XCTAssert(consumerResult)
    }
}
