//
//  StateMocks.swift
//  26/03/2019
//

@testable import YandexDisk

final class SimpleStateMock: YXState {
    private(set) var startCalled = false
    override func start() {
        super.start()
        startCalled = true
        delegate?.stateFinished(self, eventName: "finished", error: nil)
    }
}

final class HoldStateMock: YXState {
    override func start() {
        super.start()
        // just do nothing. hold!
    }
}

final class SimpleFailureStateMock: YXState {
    static let eventName = "TestFailureEventName"

    override func start() {
        super.start()
        performDelegateMethodCompleted(
            withEventName: type(of: self).eventName,
            error: NSError(domain: "test", code: 0, userInfo: nil)
        )
    }
}

final class SimpleCancelStateMock: YXState {
    override func start() {
        super.start()
        cancel()
    }

    override func cancel() {
        super.cancel()
        performDelegateMethodCancel()
    }
}

final class HoldCancellableStateMock: YXState {
    override func start() {
        super.start()
    }

    override func cancel() {
        super.cancel()
        performDelegateMethodCancel()
    }
}

final class SimpleStateWithEventMock: YXState {
    static let eventName = "TestYESEventName"

    override func start() {
        super.start()
        performDelegateMethodCompleted(withEventName: type(of: self).eventName, error: nil)
    }
}
