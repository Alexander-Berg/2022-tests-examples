import XCTest
@testable import AutoRuCallsCore
@testable import AutoRuCalls
@testable import AutoRuCallsUI
@testable import AutoRuCallStrategies
import AutoRuUtils
import AutoRuModels
import AutoRuAppConfig
import Snapshots
import SnapshotTesting
import Dispatch
import Foundation

@MainActor
final class CallModuleTests: XCTestCase {

    static let connectedEvent = """
    {
        "event": "connected",
        "data": { "features": ["video"] }
    }
    """

    private var callOptions: VoxImplantStartCallOptions!
    private var callUUID: UUID!
    private var callDelegateMock: CallDelegateMock!
    private var mainWindow: UIWindow!
    private var audioDeviceSource: CallAudioDeviceSourceMock!

    @MainActor
    override func setUp() async throws {
        try await super.setUp()

        callOptions = VoxImplantStartCallOptions(voxUsername: "test", payload: [:], isVideo: false, customData: nil)
        callUUID = UUID()

        callDelegateMock = CallDelegateMock()
        mainWindow = UIWindow(frame: CGRect(origin: .zero, size: ViewImageConfig.iPhoneX.size!))
        audioDeviceSource = CallAudioDeviceSourceMock()

        await Task { @CallServiceActor in
            CallTimeUpdater.shared.constantTimeForTests = FormattedTimeInterval(value: 30, string: "00:30")
        }.value
    }

    @MainActor
    override func tearDown() async throws {
        try await super.tearDown()

        await Task { @CallServiceActor in
            CallTimeUpdater.shared.constantTimeForTests = nil
        }.value
    }

    func testRecall() async {
        let call = await makeCall(.outgoing, withStatus: .ended(.busy))
        let module = makeCallModule(call)

        defer { withExtendedLifetime(module, {}) }

        return await withCheckedContinuation { continuation in
            callDelegateMock._callRequestedRecall = { _, _ in
                continuation.resume()
                return nil
            }

            module.store.send(.recallButtonTap)
        }
    }

    func testShowCameraButtonForVideoCall() async {
        callOptions.isVideo = true

        let call = await makeCall(.outgoing)

        guard let (module, coordinator) = await getModuleFromCallCoordinator(call: call, changeCallStatus: .started) else {
            XCTFail("module is nil")
            return
        }

        defer { withExtendedLifetime(coordinator, {}) }

        await makeSnapshot(module.viewController)
    }

    func testShowCameraButtonWhenCalleeDeviceSupportsVideo() async {
        callOptions.isVideo = false

        let call = await makeCall(.outgoing)

        guard let (module, coordinator) = await getModuleFromCallCoordinator(call: call, changeCallStatus: .started) else {
            XCTFail("module is nil")
            return
        }

        defer { withExtendedLifetime(coordinator, {}) }

        await call.messageReceived(Self.connectedEvent)
        await makeSnapshot(module.viewController)
    }

    func testToggleCameraButtonShouldBeVisibleWhenLocalVideoEnabled() async {
        let call = await makeCall(.outgoing)

        let mockVideoProvider = MockVideoProvider(image: nil, backgroundColor: .blue)

        guard let (module, coordinator) = await getModuleFromCallCoordinator(call: call, changeCallStatus: .connected) else {
            XCTFail("module is nil")
            return
        }

        defer { withExtendedLifetime(coordinator, {}) }

        await call.messageReceived(Self.connectedEvent)
        await call.localVideoProviderChanged(AnyVideoProvider(wrapped: mockVideoProvider))
        await makeSnapshot(module.viewController)
    }

    func testAudioPickerIOS() async {
        audioDeviceSource.availableDevices.insert(CallAudioDeviceSourceMock.Device(deviceType: .bluetooth))
        let call = await makeCall(.outgoing)

        let mockVideoProvider = MockVideoProvider(image: nil, backgroundColor: .blue)

        guard let (module, coordinator) = await getModuleFromCallCoordinator(call: call, changeCallStatus: .connected) else {
            XCTFail("module is nil")
            return
        }

        defer { withExtendedLifetime(coordinator, {}) }

        await call.messageReceived(Self.connectedEvent)
        await call.localVideoProviderChanged(AnyVideoProvider(wrapped: mockVideoProvider))

        let audioButton = module.viewController.view.viewWithTag(AudioDeviceCallButton.tag) as! UIButton

        let menuOptions = audioButton.menu?.children.map { $0.title } ?? []

        XCTAssertEqual(menuOptions, ["Динамик", "iPhone", "bluetooth", "откл. звук"].reversed())
    }

    func testMessages() async {
        audioDeviceSource.availableDevices.insert(CallAudioDeviceSourceMock.Device(deviceType: .bluetooth))
        let call = await makeCall(.outgoing)

        let mockLocalVideoProvider = MockVideoProvider(image: nil, backgroundColor: .blue)
        let mockRemoteVideoProvider = MockVideoProvider(image: nil, backgroundColor: .orange)

        guard let (module, coordinator) = await getModuleFromCallCoordinator(call: call, changeCallStatus: .connected) else {
            XCTFail("module is nil")
            return
        }

        defer { withExtendedLifetime(coordinator, {}) }

        await Task { @CallServiceActor in
            call.messageReceived(Self.connectedEvent)
            call.localVideoProviderChanged(AnyVideoProvider(wrapped: mockLocalVideoProvider))
            call.remoteVideoProviderChanged([AnyVideoProvider(wrapped: mockRemoteVideoProvider)])

            call.remoteCallerIsMuted(true)
            call.muted(true)

            try? await Task.sleep(nanoseconds: 1_000_000_000)

            call.localVideoProviderChanged(nil)
        }.value

        await makeSnapshot(module.viewController)
    }

    func testHideUIOnTap() async {
        let call = await makeCall(.outgoing)

        let mockLocalVideoProvider = MockVideoProvider(image: nil, backgroundColor: .blue)
        let mockRemoteVideoProvider = MockVideoProvider(image: nil, backgroundColor: .orange)

        guard let (module, coordinator) = await getModuleFromCallCoordinator(call: call, changeCallStatus: .connected) else {
            XCTFail("module is nil")
            return
        }

        defer { withExtendedLifetime(coordinator, {}) }

        await Task { @CallServiceActor in
            call.messageReceived(Self.connectedEvent)
            call.localVideoProviderChanged(AnyVideoProvider(wrapped: mockLocalVideoProvider))
            call.remoteVideoProviderChanged([AnyVideoProvider(wrapped: mockRemoteVideoProvider)])
        }.value

        module.store.send(.callScreenTapped)

        await makeSnapshot(module.viewController)
    }

    @MainActor
    private func getModuleFromCallCoordinator(
        call: Call,
        changeCallStatus status: Call.Status
    ) async -> (CallModule, CommonOutgoingCallStrategy.Coordinator)? {
        let coordinator = CommonOutgoingCallStrategy.makeCoordinatorForOutgoingCall(
            call,
            environment: CommonOutgoingCallStrategy.Environment(
                dialingSource: nil,
                microphonePermissionWriter: MicrophonePermissionWriterStub(),
                cameraPermissionWriter: CameraPermissionWriterStub(),
                flowObserver: nil,
                output: nil,
                window: mainWindow,
                viewController: nil
            )
        )!

        await call.changeStatus(status)

        try? await Task.sleep(nanoseconds: 1_000_000)

        return coordinator.callPresenter?.presentedCallModule.map {
            ($0, coordinator)
        }
    }

    private func makeSnapshot(_ viewController: UIViewController, _ identifier: String = #function) async {
        try? await Task.sleep(nanoseconds: 1_000_000)

        Snapshot.compareWithSnapshot(
            viewController,
            interfaceStyles: [.light],
            config: .iPhoneX,
            identifier: identifier,
            options: []
        )
    }

    @MainActor
    private func makeCallModule(_ call: Call) -> CallModule {
        let module = CallModule(
            call: call,
            microphonePermissionWriter: MicrophonePermissionWriterStub(),
            cameraPermissionWriter: CameraPermissionWriterStub(),
            contentProvider: nil,
            reusedVideoContainers: ReusedVideoViewContainers(),
            reuseModule: nil
        )

        return module
    }

    private func makeCall(_ direction: Call.Direction, withStatus status: Call.Status = .notInitialized) async -> Call {
        let call = Call(direction: direction, uuid: callUUID, options: .voxImplant(callOptions))
        call.setDelegate(callDelegateMock)

        call.attachedValues.audioDeviceSource = AnyAudioDeviceSource(audioDeviceSource)

        if status != .notInitialized {
            await call.changeStatus(status)
        }

        return call
    }
}
