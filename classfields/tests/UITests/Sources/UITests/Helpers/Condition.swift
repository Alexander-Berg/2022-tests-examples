import Foundation

final class Condition<Value> {
    private(set) var value: Value
    private let condition = NSCondition()

    init(_ value: Value) {
        self.value = value
    }

    func updateValueAndSignal(_ value: Value) {
        condition.lock()
        self.value = value
        condition.signal()
        condition.unlock()
    }

    @discardableResult
    func wait(_ condition: (Value) -> Bool) -> Value {
        self.condition.lock()

        defer { self.condition.unlock() }

        while !condition(value) {
            self.condition.wait()
        }

        return value
    }
}
