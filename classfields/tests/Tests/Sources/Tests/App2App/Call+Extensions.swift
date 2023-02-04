import Foundation
@testable import AutoRuCallsCore

extension Call {
    @CallServiceActor
    func changeStatus(_ status: Call.Status) {
        assert(self.status == .notInitialized)

        let now = Date()

        if status > .initialized {
            initialized(at: now)
        }

        if status > .starting {
            starting(at: now)
        }

        if status > .started {
            started(at: now)
        }

        if status > .connecting {
            startedConnecting(at: now)
        }

        if status > .connected {
            connected(at: Date())
        }

        switch status {
        case .notInitialized:
            preconditionFailure("status must be > .notInitialized")

        case .initialized:
            initialized(at: now)

        case .starting:
            starting(at: now)

        case .started:
            started(at: now)

        case .connecting:
            startedConnecting(at: now)

        case .connected:
            connected(at: Date())

        case let .ended(reason):
            ended(with: reason, at: now)
        }
    }
}
