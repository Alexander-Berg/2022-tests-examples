import AutoRuCallsCore
import Dispatch

#if DEBUG

public final class TestPstnManagedOutgoingCall: PstnManagedOutgoingCall {
    public override func startCall() async throws {
        Task { @CallServiceActor in
            try await Task.sleep(nanoseconds: 100_000_000)

            callProvider.reportOutgoingCall(with: uuid, connectedAt: nil)

            try await Task.sleep(nanoseconds: 400_000_000)

            callProvider.reportCall(with: uuid, endedAt: nil, reason: .remoteEnded)
        }
    }
}

#endif
