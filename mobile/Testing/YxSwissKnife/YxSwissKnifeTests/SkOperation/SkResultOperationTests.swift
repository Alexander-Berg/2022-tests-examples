//  Created by Denis Malykh on 13.08.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import Foundation
import XCTest

@testable import YxSwissKnife

final class SkResultOperationTests: XCTestCase {

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

    func testFinishWithResult() {
        var op = TestOperation(configuration: configuration)
        op.start()
        XCTAssertTrue(op.mainCalled)
        op.finish(with: .success("OK"))
        XCTAssertEqual(op.result, .success("OK"))
        op = TestOperation(configuration: configuration)
        op.start()
        op.finish(with: .failure(.someError))
        XCTAssertEqual(op.result, .failure(.someError))
        op = TestOperation(configuration: configuration)
        op.start()
        op.cancel(with: .someError)
        XCTAssertEqual(op.result, .failure(.someError))
    }
}

private enum TestErrors: Error {
    case someError
}

private final class TestOperation: SkResultOperation<String, TestErrors> {
    private(set) var mainCalled = false

    override func main() {
        mainCalled = true
    }
}
