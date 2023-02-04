import XCTest
@testable import AutoRuCallsCore
@testable import AutoRuCalls
@testable import AutoRuCallsUI
import AutoRuUtils
import AutoRuFetchableImage
import Snapshots
import SnapshotTesting
import CoreGraphics
import Dispatch
import Foundation

@MainActor
final class CallScreenTests: XCTestCase {

    private var callOptions: VoxImplantStartCallOptions!
    private var callUUID: UUID!
    private var callDelegateMock: CallDelegateMock!
    private var queue = DispatchQueue.main
    private var callModuleContentProvider: CallModuleContentProviderMock!
    private var audioDeviceSource: CallAudioDeviceSourceMock!

    @MainActor
    override func setUp() async throws {
        try await super.setUp()

        callOptions = VoxImplantStartCallOptions(voxUsername: "test", payload: [:], isVideo: false, customData: nil)
        callOptions.displayName = "Евгений"
        callOptions.avatar = .testImage(withFixedSize: CGSize(width: 100, height: 100))
        callUUID = UUID()

        callDelegateMock = CallDelegateMock()
        callModuleContentProvider = CallModuleContentProviderMock(content: bigSquareContent)
        audioDeviceSource = CallAudioDeviceSourceMock()
    }

    func testActiveCallScreen() async {
        let call = await makeCall(.outgoing, with: .connected)
        call.attachedValues.displayedCallStatus = .init(
            connectionStatus: .connected,
            direction: .outgoing,
            hasConnected: true,
            connectedTimeInterval: .init(value: 2, string: "00:02")
        )

        for (name, content) in contents() {
            callModuleContentProvider.content = content

            await validateSnapshots("\(#function.dropLast(2)) \(name)") {
                await makeCallModule(call)
            }
        }
    }

    /*
    func testNoMicrophonePermissionScreen() {
        // снэпшоты не совсем правильные – не работает размытие.

        permissionService._askPermissionForMicrophone = { callback in
            callback((false, false))
        }

        let call = makeCall(.outgoing, with: .starting)
        call.attachedValues.displayedCallStatus = .init(
            connectionStatus: .starting(isVideo: false),
            direction: .outgoing
        )

        validateSnapshots {
            makeCallModule(call)
        }
    }

    func testNoMicrophonePermissionScreenWithPhoneFallback() {
        // снэпшоты не совсем правильные – не работает размытие.

        permissionService._askPermissionForMicrophone = { callback in
            callback((false, false))
        }

        callOptions.fallbackInfo = .init(
            callOptions: PSTNStartCallOptions(phoneNumber: "+7 (915) 890-39-23"),
            fallbackCallback: { _ in },
            permissionScreenPresented: { }
        )

        let call = makeCall(.outgoing, with: .starting)

        call.attachedValues.displayedCallStatus = .init(
            connectionStatus: .starting(isVideo: false),
            direction: .outgoing
        )

        validateSnapshots {
            makeCallModule(call)
        }
    }
     */

    func testSelectedAudioButton() async {
        let call = await makeCall(.outgoing, with: .connected)
        call.attachedValues.displayedCallStatus = .init(
            connectionStatus: .connected,
            direction: .outgoing,
            hasConnected: true,
            connectedTimeInterval: .init(value: 2, string: "00:02")
        )

        await validateSnapshot {
            let module = await makeCallModule(call)
            module.viewController.loadViewIfNeeded()
            module.store.send(.toggleAudioDevice)
            return module
        }
    }

    func testMuteButton() async {
        let call = await makeCall(.outgoing, with: .connected)

        call.attachedValues.displayedCallStatus = .init(
            connectionStatus: .connected,
            direction: .outgoing,
            hasConnected: true,
            connectedTimeInterval: .init(value: 2, string: "00:02")
        )

        await validateSnapshot {
            let module = await makeCallModule(call)
            module.viewController.loadViewIfNeeded()
            module.store.send(.muteCallButtonTap)
            return module
        }
    }

    func testSingleLongTextLine() async {
        let call = await makeCall(.outgoing, with: .starting)
        call.attachedValues.displayedCallStatus = .init(
            connectionStatus: .starting(isVideo: false),
            direction: .outgoing
        )

        callModuleContentProvider.content.textLine1 = "Lorem ipsum, dolor sit amet consectetur adipisicing elit. Quos in sint culpa voluptate impedit ab nesciunt numquam temporibus eveniet distinctio!"
        callModuleContentProvider.content.textLine2 = nil

        await validateSnapshot {
            await makeCallModule(call)
        }
    }

    func testSingleTextLine() async {
        let call = await makeCall(.outgoing, with: .starting)
        call.attachedValues.displayedCallStatus = .init(
            connectionStatus: .starting(isVideo: false),
            direction: .outgoing
        )

        callModuleContentProvider.content.textLine1 = "One line text"
        callModuleContentProvider.content.textLine2 = nil

        await validateSnapshot {
            await makeCallModule(call)
        }
    }

    func testIncomingCall() async {
        let call = await makeCall(.incoming, with: .starting)
        call.attachedValues.displayedCallStatus = .init(
            connectionStatus: .starting(isVideo: false),
            direction: .incoming
        )

        await validateSnapshot {
            await makeCallModule(call)
        }
    }

    func testRecall() async {
        let call = await makeCall(.outgoing, with: .ended(.busy))
        call.attachedValues.displayedCallStatus = .init(
            connectionStatus: .ended(.busy),
            direction: .outgoing
        )

        await validateSnapshot {
            await makeCallModule(call)
        }
    }

    func testLoadingImageContent() async {
        callModuleContentProvider.content = makeContent(withImage: .infineLoadingImage)

        let call = await makeCall(.incoming, with: .starting)
        call.attachedValues.displayedCallStatus = .init(
            connectionStatus: .starting(isVideo: false),
            direction: .incoming
        )

        await validateSnapshot {
            await makeCallModule(call)
        }
    }

    func testLoadingAvatar() async {
        callOptions.avatar = .infineLoadingImage
        let call = await makeCall(.incoming, with: .starting)
        call.attachedValues.displayedCallStatus = .init(
            connectionStatus: .starting(isVideo: false),
            direction: .incoming
        )

        await validateSnapshot {
            await makeCallModule(call)
        }
    }

    func testNoAvatar() async {
        callOptions.avatar = FetchableImage()
        let call = await makeCall(.incoming, with: .starting)
        call.attachedValues.displayedCallStatus = .init(
            connectionStatus: .starting(isVideo: false),
            direction: .incoming
        )

        await validateSnapshot {
            await makeCallModule(call)
        }
    }

    func testEndedCall() async {
        let call = await makeCall(.outgoing, with: .ended(.busy))
        call.attachedValues.displayedCallStatus = .init(
            connectionStatus: .ended(.busy),
            direction: .outgoing
        )

        await validateSnapshot {
            let module = await makeCallModule(call)
            module.addDimmingAndDismiss({ })
            return module
        }
    }

    private func makeCallModule(_ call: Call) async -> CallModule {
        let module = CallModule(
            call: call,
            microphonePermissionWriter: MicrophonePermissionWriterStub(),
            cameraPermissionWriter: CameraPermissionWriterStub(),
            contentProvider: callModuleContentProvider,
            reusedVideoContainers: ReusedVideoViewContainers(),
            reuseModule: nil
        )

        let background = FetchableImage
            .testImage(withFixedSize: CGSize(width: 1000, height: 1000))
            .makeCallScreenBackground()
            .blocking()

        module.updateBackground(background, animated: false)

        return module
    }

    private func makeCall(_ direction: Call.Direction, with status: Call.Status) async -> Call {
        let call = Call(direction: direction, uuid: callUUID, options: .voxImplant(callOptions))
        call.setDelegate(callDelegateMock)

        call.attachedValues.audioDeviceSource = AnyAudioDeviceSource(audioDeviceSource)

        await call.changeStatus(status)

        return call
    }

    private func validateSnapshot(
        _ identifier: String = #function,
        _ config: ViewImageConfig = .iPhoneX,
        _ makeModule: @MainActor () async -> CallModule
    ) async {
        let module = await makeModule()

        try? await Task.sleep(nanoseconds: 1_000_000)

        Snapshot.compareWithSnapshot(
            module.viewController,
            interfaceStyles: [.light],
            config: config,
            identifier: identifier,
            options: []
        )
    }

    private func validateSnapshots(
        _ identifier: String = #function,
        _ makeModule: () async -> CallModule
    ) async {
        for (name, config) in configurations() {
            await validateSnapshot("\(identifier) \(name)", config, makeModule)
        }
    }

    private func configurations() -> [(name: String, ViewImageConfig)] {
        [
            ("iPhone X", ViewImageConfig.iPhoneX),
            ("iPhone SE", ViewImageConfig.iPhoneSe),
            ("iPhone XS Max", ViewImageConfig.iPhoneXsMax),
            ("iPad mini portrait", ViewImageConfig.iPadMini(.portrait)),
            ("iPad mini landscape", ViewImageConfig.iPadMini(.landscape)),
            ("iPad Pro 11 portrait", ViewImageConfig.iPadPro11(.portrait)),
            ("iPad Pro 11 landscape", ViewImageConfig.iPadPro11(.landscape))
        ]
    }

    private func contents() -> [(name: String, CallModuleContent)] {
        [
            ("small tall", smallTallContent),
            ("small wide", smallWideContent),
            ("small square", smallSquareContent),
            ("big tall", bigTallContent),
            ("big wide", bigWideContent),
            ("big square", bigSquareContent)
        ]
    }

    private var smallTallContent: CallModuleContent {
        makeContent(withSize: CGSize(width: 100, height: 200))
    }

    private var smallWideContent: CallModuleContent {
        makeContent(withSize: CGSize(width: 200, height: 100))
    }

    private var smallSquareContent: CallModuleContent {
        makeContent(withSize: CGSize(width: 100, height: 100))
    }

    private var bigTallContent: CallModuleContent {
        makeContent(withSize: CGSize(width: 3000, height: 6000))
    }

    private var bigWideContent: CallModuleContent {
        makeContent(withSize: CGSize(width: 6000, height: 3000))
    }

    private var bigSquareContent: CallModuleContent {
        makeContent(withSize: CGSize(width: 3000, height: 3000))
    }

    private func makeContent(withSize size: CGSize) -> CallModuleContent {
        makeContent(
            withImage: .testImage(withFixedSize: size)
        )
    }

    private func makeContent(
        withImage image: FetchableImage,
        line1: String? = "Chevrolet Camaro II",
        line2: String? = "3 500 000 ₽"
    ) -> CallModuleContent {
        let controller = ImageContentViewController()
        controller.setImage(image)

        return CallModuleContent(
            contentViewController: controller,
            textLine1: line1,
            textLine2: line2,
            tapHandler: nil
        )
    }
}
