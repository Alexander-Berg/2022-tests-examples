//  Created by Denis Malykh on 13.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import Foundation
import XCTest

@testable import YxSwissKnife

final class SkOperationTests: XCTestCase {

    private var logger: FakeLogger!
    private var dispatcher: FakeDispatcher!
    private var configuration: SkOperationConfiguration!

    override func setUp() {
        super.setUp()
        logger = FakeLogger()
        dispatcher = FakeDispatcher()
        configuration = SkOperationConfiguration(
            logger: logger,
            dispatcher: dispatcher,
            writeOperationsLog: true
        )
    }

    func testBasicExecution() {
        let op = TestOperation(configuration: configuration)
        dispatcher.add(one: op)
        XCTAssertEqual(dispatcher.operations.count, 1)
        XCTAssertTrue(dispatcher.operations[0] is TestOperation)
        dispatcher.operations[0].start()
        XCTAssertTrue(op.mainCalled)
        XCTAssertTrue(logger.checkOperationTransition(id: op.debugId))
    }

    func testWatchdog() {
        let queue = FakeDispatching()
        let op = TestWatchdogOperation(configuration: configuration, watchdogQueue: queue)
        op.start()
        XCTAssertTrue(op.mainCalled)
        XCTAssertEqual(queue.timedWorks.count, 1)
        queue.executeTimedAsyncWorks()
        XCTAssertTrue(op.watchdogCalled)
        XCTAssertTrue(logger.checkWatchdog(id: op.debugId))
    }

    func testDependencies() {
        let op1 = TestOperation(configuration: configuration)
        let op2 = TestOperation(configuration: configuration)
        op2.addDependency(op1)
        XCTAssertEqual(op2.debugId, op1.groupId)
        XCTAssertTrue(logger.checkAddDependency(id: op2.debugId, child: op1.debugId))
        op2.removeDependency(op1)
        XCTAssertEqual(op2.debugId, op1.groupId) // group preserved
        XCTAssertTrue(logger.checkRemoveDependency(id: op2.debugId, child: op1.debugId))
    }

    func testCancellation() {
        let op1 = TestOperation(configuration: configuration)
        let op2 = TestOperation(configuration: configuration)
        op2.addDependency(op1)
        op2.cancel()
        XCTAssertTrue(op1.isCancelled)
        XCTAssertTrue(op2.isCancelled)
        XCTAssertTrue(logger.checkCancellation(id1: op1.debugId, id2: op2.debugId))
    }

}

private final class TestOperation: SkOperation {

    private(set) var mainCalled = false

    override func main() {
        mainCalled = true
        finish()
    }
}

private final class TestWatchdogOperation: SkOperation {
    private(set) var mainCalled = false
    private(set) var watchdogCalled = false

    override var watchdogTimeout: TimeInterval {
        return 10.0
    }

    override func main() {
        mainCalled = true
    }

    override func watchdog() {
        watchdogCalled = true
    }
}

private extension FakeLogger {
    func checkOperationTransition(id: String) -> Bool {
        var initialToPending = false
        var pendingToReady = false
        var readyToExecuting = false
        var executingToFinishing = false
        var finishingToFinish = false
        for item in infoEntries where item.message == "OPLOG" {
            guard let type = item.params?["type"], type == "ss" else {
                continue
            }
            guard let op = item.params?["op"], op == id else {
                continue
            }
            guard let from = item.params?["from"].flatMap(SkOperationLogItem.State.init(rawValue:)),
                  let to = item.params?["to"].flatMap(SkOperationLogItem.State.init(rawValue:)) else {
                continue
            }
            switch (from, to) {
            case (.initial, .pending): initialToPending = true
            case (.pending, .ready): pendingToReady = true
            case (.ready, .executing): readyToExecuting = true
            case (.executing, .finishing): executingToFinishing = true
            case (.finishing, .finished): finishingToFinish = true
            default: continue
            }
        }
        return initialToPending && pendingToReady && readyToExecuting && executingToFinishing && finishingToFinish
    }

    func checkWatchdog(id: String) -> Bool {
        for item in infoEntries where item.message == "OPLOG" {
            guard let type = item.params?["type"], type == "wd" else {
                continue
            }
            guard let op = item.params?["op"], op == id else {
                continue
            }
            return true
        }
        return false
    }

    func checkAddDependency(id: String, child: String) -> Bool {
        for item in infoEntries where item.message == "OPLOG" {
            guard let type = item.params?["type"], type == "ad" else {
                continue
            }
            guard let op = item.params?["op"], op == id else {
                continue
            }
            guard let dep = item.params?["dep"], dep == child else {
                continue
            }
            return true
        }
        return false
    }

    func checkRemoveDependency(id: String, child: String) -> Bool {
        for item in infoEntries where item.message == "OPLOG" {
            guard let type = item.params?["type"], type == "rd" else {
                continue
            }
            guard let op = item.params?["op"], op == id else {
                continue
            }
            guard let dep = item.params?["dep"], dep == child else {
                continue
            }
            return true
        }
        return false
    }

    func checkCancellation(id1: String, id2: String) -> Bool {
        var id1Match = false
        var id2Match = false
        for item in infoEntries where item.message == "OPLOG" {
            guard let type = item.params?["type"], type == "ucanc" else {
                continue
            }
            guard let op = item.params?["op"] else {
                continue
            }
            if op == id1 {
                id1Match = true
            }
            if op == id2 {
                id2Match = true
            }
        }
        return id1Match && id2Match
    }
}
