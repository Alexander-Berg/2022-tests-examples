import Foundation
import XCTest
@testable import YxSwissKnife

class YxDispatcherExclusivityTestCase: XCTestCase {
    var dispatcher: YxRootDispatcher!
    var exclusivityController: YxExclusivityController!

    override func setUp() {
        super.setUp()
        exclusivityController = YxExclusivityController()
        dispatcher = YxRootDispatcher(
            logger: nil,
            queueProvider: YxOpcodeBasedQueueProvider(logger: nil),
            exclusivityController: exclusivityController)
    }

    func test() {
        var ops = [Operation]()
        var actualLog = ""
        var expectedLog = ""
        for i in 0..<100 {
            let element = "-\(i)"
            expectedLog += element

            let op = TestOperation(dispatcher: dispatcher) {
                actualLog += element
            }
            ops.append(op)
        }
        executeAndWait(operations: ops, with: dispatcher)
        XCTAssertEqual(actualLog, expectedLog)

    }

    class TestOperation: YxAsyncOperation {
        var block: () -> Void
        init(dispatcher: YxDispatcher, block: @escaping () -> Void) {
            self.block = block
            super.init(logger: nil, dispatcher: dispatcher)
            addCondition(YxMutuallyExclusive<TestOperation>(exclusivityKey: ""))
        }

        override func execute() {
            block()
            finish()
        }
    }
}
