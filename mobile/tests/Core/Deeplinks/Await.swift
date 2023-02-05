import XCTest

func awaitFor(
    fulfillmentOf predicate: NSPredicate,
    withFailingMessage message: String = "Не дождались выполнения предиката %@ в течение %.2f секунд.",
    timeout: TimeInterval = 10.0,
    file: StaticString = #file,
    line: UInt = #line
) {
    var failed = false

    defer {
        if failed {
            let failingMessage = String(format: message, arguments: [predicate, timeout])
            XCTFail(failingMessage, file: file, line: line)
        }
    }

    let iterations = max(1, Int(ceil(timeout / awaitPeriod)))

    return XCTContext.runActivity(named: "Ждем выполнения \(predicate)") { _ in
        for _ in 0 ..< iterations {
            if predicate.evaluate(with: nil) {
                return
            }

            usleep(useconds_t(awaitPeriod * 1_000_000))
        }
        failed = true
    }
}

func awaitFor(
    fulfillmentOf block: @escaping () -> Bool,
    withFailingMessage message: String = "Условие не выполнилось в течение %.2f секунд.",
    timeout: TimeInterval = 10.0,
    file: StaticString = #file,
    line: UInt = #line
) {
    awaitFor(
        fulfillmentOf: NSPredicate(block: { _, _ in block() }),
        withFailingMessage: message,
        timeout: timeout,
        file: file,
        line: line
    )
}

private let awaitPeriod: TimeInterval = 0.1
