import XCTest
@testable import AutoRuCallsCore
@testable import AutoRuCalls
@testable import AutoRuCallsUI
import AutoRuUtils
import Foundation

final class CallDisplayedStatusTests: BaseUnitTest {

    private var callUUID: UUID!
    private var callOptions: VoxImplantStartCallOptions!

    override func setUp() {
        super.setUp()

        callUUID = UUID()
        callOptions = VoxImplantStartCallOptions(voxUsername: "test", payload: [:], isVideo: false, customData: nil)
    }

    @CallServiceActor
    func testOutgoingCallStatuses() {
        let call = makeCall(.outgoing)

        let now = Date()

        call.initialized(at: now)
        XCTAssertEqual("Ожидание...", call.attachedValues.displayedCallStatus.description)

        call.starting(at: now)
        XCTAssertEqual("Звоним...", call.attachedValues.displayedCallStatus.description)

        call.started(at: now)
        XCTAssertEqual("Звоним...", call.attachedValues.displayedCallStatus.description)

        call.startedConnecting(at: now)
        XCTAssertEqual("Соединяем...", call.attachedValues.displayedCallStatus.description)

        call.attachedValues.connectedTimeInterval = FormattedTimeInterval(value: 2, string: "00:02")
        call.connected(at: Date())
        XCTAssertEqual("00:02", call.attachedValues.displayedCallStatus.description)
    }

    @CallServiceActor
    func testEndOutgoingCallStatuses() {
        var call = makeCall(.outgoing, withStatus: .started)

        let now = Date()

        call.ended(with: .busy, at: now)
        XCTAssertEqual("Занято", call.attachedValues.displayedCallStatus.description)

        call = makeCall(.outgoing, withStatus: .started)
        call.ended(with: .remoteEnded, at: now)
        XCTAssertEqual("Вызов отклонён", call.attachedValues.displayedCallStatus.description)

        call = makeCall(.outgoing, withStatus: .connected)
        call.ended(with: .remoteEnded, at: now)
        XCTAssertEqual("Звонок завершён", call.attachedValues.displayedCallStatus.description)

        call = makeCall(.outgoing, withStatus: .connected)
        call.ended(with: .declined, at: now)
        XCTAssertEqual("Звонок завершён", call.attachedValues.displayedCallStatus.description)

        call = makeCall(.outgoing, withStatus: .connected)
        call.ended(with: .answeredElsewhere, at: now)
        XCTAssertEqual("Звонок завершён", call.attachedValues.displayedCallStatus.description)

        call = makeCall(.outgoing, withStatus: .connected)
        call.ended(with: .failed(NSError(domain: "", code: 42, userInfo: nil)), at: now)
        XCTAssertEqual("Звонок завершён", call.attachedValues.displayedCallStatus.description)
    }

    @CallServiceActor
    func testIncomingCallStatuses() {
        let call = makeCall(.incoming)

        let now = Date()

        call.initialized(at: now)
        XCTAssertEqual("Ожидание...", call.attachedValues.displayedCallStatus.description)

        call.starting(at: now)
        XCTAssertEqual("Входящий аудио-вызов", call.attachedValues.displayedCallStatus.description)

        call.started(at: now)
        XCTAssertEqual("Входящий аудио-вызов", call.attachedValues.displayedCallStatus.description)

        call.startedConnecting(at: now)
        XCTAssertEqual("Соединяем...", call.attachedValues.displayedCallStatus.description)

        call.attachedValues.connectedTimeInterval = FormattedTimeInterval(value: 2, string: "00:02")
        call.connected(at: Date())
        XCTAssertEqual("00:02", call.attachedValues.displayedCallStatus.description)
    }

    @CallServiceActor
    func testEndIncomingCallStatuses() {
        var call = makeCall(.incoming, withStatus: .started)

        let now = Date()

        call = makeCall(.outgoing, withStatus: .started)
        call.ended(with: .declined, at: now)
        XCTAssertEqual("Звонок завершён", call.attachedValues.displayedCallStatus.description)

        call = makeCall(.outgoing, withStatus: .connected)
        call.ended(with: .remoteEnded, at: now)
        XCTAssertEqual("Звонок завершён", call.attachedValues.displayedCallStatus.description)

        call = makeCall(.outgoing, withStatus: .connected)
        call.ended(with: .declined, at: now)
        XCTAssertEqual("Звонок завершён", call.attachedValues.displayedCallStatus.description)

        call = makeCall(.outgoing, withStatus: .connected)
        call.ended(with: .answeredElsewhere, at: now)
        XCTAssertEqual("Звонок завершён", call.attachedValues.displayedCallStatus.description)

        call = makeCall(.outgoing, withStatus: .connected)
        call.ended(with: .failed(NSError(domain: "", code: 42, userInfo: nil)), at: now)
        XCTAssertEqual("Звонок завершён", call.attachedValues.displayedCallStatus.description)
    }

    @CallServiceActor
    private func makeCall(_ direction: Call.Direction, withStatus status: Call.Status? = nil) -> Call {
        let call = Call(direction: direction, uuid: callUUID, options: .voxImplant(callOptions))
        call.addObserver(CallStatusDescriptionUpdater.shared)

        if let status = status {
            call.changeStatus(status)
        }
        return call
    }
}
