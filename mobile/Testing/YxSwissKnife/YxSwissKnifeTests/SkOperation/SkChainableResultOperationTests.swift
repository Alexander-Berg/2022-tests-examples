//  Created by Denis Malykh on 13.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import Foundation
import XCTest

@testable import YxSwissKnife

final class SkChainableResultOperationTests: XCTestCase {

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

    func testConnection() {
        var op1ConnectorCalled = false
        var op2ConnectorCalled = false
        let op1 = TestOperation(configuration: configuration)
        let op2 = TestOperation(configuration: configuration)
        let op3 = TestOperation(configuration: configuration)
        op3.addDependency(op1) { to, from in
            if case let .failure(error) = from.result {
                to.finish(with: .failure(error))
            }
            op1ConnectorCalled = true
        }
        op3.addDependency(op2) { to, from in
            if case let .failure(error) = from.result {
                to.finish(with: .failure(error))
            }
            op2ConnectorCalled = true
        }
        op1.start()
        op1.finish(with: .success("OK"))
        op2.start()
        op2.finish(with: .failure(.someError))
        op3.start()
        XCTAssertEqual(op3.result, .failure(.someError))
        XCTAssertTrue(op1ConnectorCalled && op2ConnectorCalled)
    }
}

private enum TestErrors: Error {
    case someError
}

private final class TestOperation: SkChainableResultOperation<String, TestErrors> {
    private(set) var mainCalled = false

    override func main() {
        mainCalled = true
    }
}

