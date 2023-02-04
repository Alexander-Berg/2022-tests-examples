import XCTest
import AutoRuCalls
import AutoRuUtils
import AutoRuFetchableImage
import Snapshots
@testable import AutoRuCallsCore
@testable import AutoRuCallsUI
@testable import AutoRuCallStrategies
import CoreGraphics
import Dispatch
import Foundation
import AutoRuRxSwiftUtils

@MainActor
final class CallMiniModuleTests: XCTestCase {
    private var callOptions: VoxImplantStartCallOptions!
    private var callUUID: UUID!
    private var callDelegateMock: CallDelegateMock!
    private var mainWindow: UIWindow!
    private var rootViewController: UIViewController!

    @MainActor
    override func setUp() async throws {
        try await super.setUp()

        KeyboardLayoutGuide.setUp()

        callOptions = VoxImplantStartCallOptions(voxUsername: "test", payload: [:], isVideo: false, customData: nil)
        callOptions.displayName = "Евгений"
        callOptions.avatar = .testImage(withFixedSize: CGSize(width: 100, height: 100))
        callUUID = UUID()
        mainWindow = UIWindow()
        rootViewController = UIViewController()
        mainWindow.rootViewController = rootViewController

        callDelegateMock = CallDelegateMock()

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

    func testSnapshotsForOutgoingCall() async {
        await validateSnapshots(for: .outgoing)
    }

    func testSnapshotsForIncomingCall() async {
        await validateSnapshots(for: .incoming)
    }

    func testLoadingAvatar() async {
        callOptions.avatar = .infineLoadingImage

        await validateSnapshot(for: .outgoing, status: .connected, suffix: "loadingAvatar")
    }

    func testNoAvatar() async {
        callOptions.avatar = FetchableImage()

        await validateSnapshot(for: .outgoing, status: .connected, suffix: "noAvatar")
    }

    func testEndedVideoCall() async throws {
        let call = await makeCall(.outgoing)

        let mockRemoteVideoProvider = MockVideoProvider(image: nil, backgroundColor: .lightGray)

        guard let module = try await getModuleFromCallCoordinator(call: call, changeCallStatus: .connected) else {
            XCTFail("callMiniModule is nil")
            return
        }
        await call.remoteVideoProviderChanged([AnyVideoProvider(wrapped: mockRemoteVideoProvider)])
        await call.ended(with: .remoteEnded, at: Date())

        try await Task.sleep(nanoseconds: 1_000_000)

        Snapshot.compareWithSnapshot(
            view: module.view,
            interfaceStyle: [.light]
        )
    }

    private func validateSnapshots(for callDirection: Call.Direction) async {
        func validate(status: Call.Status, suffix: String? = nil) async {
            await validateSnapshot(for: callDirection, status: status, suffix: suffix ?? "\(status)")
        }

        await validate(status: .starting)
        await validate(status: .started)
        await validate(status: .connecting)
        await validate(status: .connected)
        await validate(status: .ended(.busy), suffix: "busy")
        await validate(status: .ended(.declined), suffix: "declined")
    }

    private func validateSnapshot(for callDirection: Call.Direction, status: Call.Status, suffix: String) async {
        let call = await makeCall(callDirection, with: status)

        let callMiniModule = CallMiniModule(call: call, reusedVideoContainers: ReusedVideoViewContainers(), tapHandler: {})

        defer { withExtendedLifetime(callMiniModule, {}) }

        callMiniModule.updateBackground(call.attachedValues.background)

        let floatingView = makeFloatingView(window: mainWindow)
        floatingView.showView(callMiniModule.view, animated: false)

        try? await Task.sleep(nanoseconds: 1_000_000)

        Snapshot.compareWithSnapshot(
            view: callMiniModule.view,
            interfaceStyle: [.light],
            identifier: "CallMiniView_\(callDirection)_\(suffix)"
        )
    }

    private func makeCall(_ direction: Call.Direction, with status: Call.Status = .notInitialized) async -> Call {
        let call = Call(direction: direction, uuid: callUUID, options: .voxImplant(callOptions))
        call.setDelegate(callDelegateMock)
        call.attachedValues.background = FetchableImage
            .testImage(withFixedSize: CGSize(width: 100, height: 100))
            .makeCallScreenBackground()
            .blocking()

        call.attachedValues.connectedTimeInterval = .init(value: 2, string: "00:02")

        call.addObserver(CallStatusDescriptionUpdater.shared)

        if status != .notInitialized {
            await call.changeStatus(status)
        }

        return call
    }

    private func makeFloatingView(window: UIWindow) -> FloatingView {
        let view = FloatingView(frame: CGRect(origin: .zero, size: CGSize(width: 80, height: 128)))
        window.addSubview(view)
        return view
    }

    private func getModuleFromCallCoordinator(
        call: Call,
        changeCallStatus status: Call.Status
    ) async throws -> CallMiniModule? {
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

        coordinator.callPresenter?._showFloatingViewForTests()

        return coordinator.callPresenter?.presentedCallMiniModule
    }
}
