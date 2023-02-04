@testable import AutoRuCallsCore

final class CallDelegateMock: CallDelegate, @unchecked Sendable {
    @ThreadSafe
    nonisolated var _callRequestedStart: (Call) async throws -> Void = { _ in }

    @ThreadSafe
    nonisolated var _callRequestedEnd: (Call) async throws -> Void = { _ in }

    @ThreadSafe
    nonisolated var _callRequestedMute: (Call, Bool) async throws -> Void = { _, _ in }

    @ThreadSafe
    nonisolated var _callRequestedSendMessage: (Call, String) async throws -> Void = { _, _ in }

    @ThreadSafe
    nonisolated var _callRequestedRecall: (Call, StartCallOptions?) async -> Call? = { _, _ in nil }

    @ThreadSafe
    nonisolated var _callRequestedEnableLocalVideo: (Call, Bool, CameraPosition) async throws -> Void = { _, _, _ in }

    nonisolated init() {
    }

    func callRequestedStart(_ call: Call) async throws {
        try await _callRequestedStart(call)
    }

    func callRequestedEnd(_ call: Call) async throws {
        try await  _callRequestedEnd(call)
    }

    func callRequestedMute(_ call: Call, mute: Bool) async throws {
        try await _callRequestedMute(call, mute)
    }

    func callRequestedSendMessage(_ call: Call, message: String) async throws {
        try await _callRequestedSendMessage(call, message)
    }

    func callRequestedRecall(_ call: Call, newOptions: StartCallOptions?) async -> Call? {
        await _callRequestedRecall(call, newOptions)
    }

    func callRequestedEnableLocalVideo(_ call: Call, enable: Bool, cameraPosition: CameraPosition) async throws {
        try await _callRequestedEnableLocalVideo(call, enable, cameraPosition)
    }

    func extendLifetime(for call: Call) -> Any? {
        nil
    }
}
