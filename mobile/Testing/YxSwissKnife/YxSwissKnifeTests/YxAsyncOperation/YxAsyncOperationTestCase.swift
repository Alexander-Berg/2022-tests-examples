import XCTest
@testable import YxSwissKnife

class AsyncOperationTests: XCTestCase {
    var dispatcher: YxRootDispatcher!

    override func setUp() {
        super.setUp()
        dispatcher = YxRootDispatcher(
            logger: nil,
            queueProvider: YxOpcodeBasedQueueProvider(logger: nil),
            exclusivityController: YxExclusivityController())
    }

    override func tearDown() {
        super.tearDown()
    }

    func testOperationExecutes() {
        let op = BaseTestOperation(dispatcher: dispatcher)
        provision(operation: op)
        op.exp = expectation(description: "Operation executed")
        op.block = { $0.exp?.fulfill() }
        dispatcher.add(one: op)
        waitForExpectations(timeout: 10, handler: nil)
    }

    func testMultipleOperationsExecutes() {
        let exp = expectation(description: "Operation executed")
        let target = 99
        for i in 0...target {
            let op = BaseTestOperation(dispatcher: dispatcher)
            provision(operation: op)
            op.block = { _ in if i == target { exp.fulfill() } }
            dispatcher.add(one: op)
        }
        waitForExpectations(timeout: 10, handler: nil)
    }

    func testMultipleOperationsExecutesWithDependencies() {
        let exp = expectation(description: "Operation executed")
        let target = 99
        let subtarget = 4
        for i in 0...target {
            let op = BaseTestOperation(dispatcher: dispatcher)
            provision(operation: op)
            op.block = { _ in if i == target { exp.fulfill() } }
            for _ in 0...subtarget {
                let subop = BaseTestOperation(dispatcher: dispatcher)
                provision(operation: subop)
                op.addDependency(subop)
                dispatcher.add(one: subop)
            }
            dispatcher.add(one: op)
        }
        waitForExpectations(timeout: 10, handler: nil)
    }

    func testMultipleOperationsExecutesWithDependenciesAndCancellation() {
        let exp = expectation(description: "Operation executed")
        let target = 99
        let subtarget = 4
        var operation: BaseTestOperation?
        for i in 0...target {
            if i % 2 == 0 {
                operation?.cancel()
            }
            let op = BaseTestOperation(dispatcher: dispatcher)
            provision(operation: op)
            op.block = { _ in if i == target { exp.fulfill() } }
            for _ in 0...subtarget {
                let subop = BaseTestOperation(dispatcher: dispatcher)
                provision(operation: subop)
                op.addDependency(subop)
                dispatcher.add(one: subop)
            }
            dispatcher.add(one: op)
            operation = op
        }
        waitForExpectations(timeout: 10, handler: nil)
    }

    class BaseTestOperation: YxAsyncOperation {
        var exp: XCTestExpectation?
        var block: ((BaseTestOperation) -> Void)?

        init(dispatcher: YxDispatcher) {
            super.init(logger: nil, dispatcher: dispatcher)
            name = "Test Operation"
        }

        override func execute() {
            block?(self)
            finish()
        }
    }

    private func provision(operation: YxAsyncOperation) {
        operation.dispatcher = dispatcher
    }
}
