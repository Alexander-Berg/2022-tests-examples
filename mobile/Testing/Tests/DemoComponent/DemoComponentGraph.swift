//  Created by Denis Malykh on 06.09.2021.
//  Copyright © 2021 Yandex. All rights reserved.

import Foundation

final class DemoComponentGraph {

    private let logger: YxLogger
    private let queueProvider: SkQueueProvider
    private let dispatcher: SkDispatcher

    // NOTE: this initializer is supposed to receive external deps, but because
    //       of this demo it is empty (kinda root)
    init(/* here be dragons */) {
        logger = YxDummyLogger()
        queueProvider = SkOpCodeBasedQueueProvider(logger: logger)
        dispatcher = SkBasicDispatcher(
            logger: logger,
            queueProvider: queueProvider
        )
    }

    func makeViewController() -> DemoComponentViewController {
        DemoComponentViewController(
            demoSDK: makeDemoSDK(logger: logger, dispatcher: dispatcher),
            blockOperationFactory: makeBlockOperation(name:opcode:block:) // NOTE: strong reference to self here
        )
    }


    private func makeBlockOperation(name: String, opcode: OpCode, block: @escaping () -> Void) -> SkBlockOperation {
        SkBlockOperation(
            name: name,
            opcode: opcode,
            configuration: SkOperationConfiguration(
                logger: logger,
                dispatcher: dispatcher,
                writeOperationsLog: true
            ),
            block: block
        )
    }

}


private class YxDummyLogger: YxLogger {
    func clean() {
    }

    func _debug(_ message: String, newlines: Bool, file: String, line: UInt, column: Int, function: String, params: [String : String]) {

    }

    func _info(_ message: String, newlines: Bool, file: String, line: UInt, column: Int, function: String, params: [String : String]) {

    }

    func _warning(_ message: String, newlines: Bool, file: String, line: UInt, column: Int, function: String, params: [String : String]) {

    }

    func _error(_ message: String, newlines: Bool, file: String, line: UInt, column: Int, function: String, params: [String : String]) {

    }

    func _error(_ message: String, newlines: Bool, file: String, line: UInt, column: Int, function: String, error: Error) {

    }

    func _error(_ message: String, newlines: Bool, file: String, line: UInt, column: Int, function: String, params: [String : String], crash: Bool) {

    }

    func _error(_ message: String, newlines: Bool, file: String, line: UInt, column: Int, function: String, error: Error, crash: Bool) {

    }


}
