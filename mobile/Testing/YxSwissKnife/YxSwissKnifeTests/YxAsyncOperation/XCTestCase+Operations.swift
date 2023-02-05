import Foundation
import XCTest
@testable import YxSwissKnife

extension XCTestCase {
    func executeAndWait(operation: Operation, with dispatcher: YxRootDispatcher) {
        executeAndWait(operations: [operation], with: dispatcher)
    }

    func executeAndWait(operations: [Operation], with dispatcher: YxRootDispatcher) {
        dispatcher.add(all: operations)
        let exp = expectation(description: "Wait for operations complete")
        let expOperation = BlockOperation {
            exp.fulfill()
        }
        for operation in operations {
            expOperation.addDependency(operation)
        }
        dispatcher.add(one: expOperation)
        waitForExpectations(timeout: 2.0)
    }
}
