import XCTest
import AutoRuAppearance
import AutoRuYogaLayout
import Snapshots
@testable import AutoRuChat
import AutoRuColorSchema
import Foundation

final class ChatMessagesAppearanceTests: BaseUnitTest {
    func test_antifraud() {
        let model = AntifraudLayoutModel(message: "Сообщение", name: "Имя", dateText: "01.01.1970", onClose: { })
        let spec = AntifraudLayoutSpec(model: model)

        Snapshot.compareWithSnapshot(
            layoutSpec: spec,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_buyReport() {
        let model = BuyReportLayoutModel(
            message: "Сообщение",
            name: "Имя",
            dateText: "01.01.1970",
            price: 123,
            originalPrice: 130,
            onTap: { },
            onClose: { }
        )
        let spec = BuyReportLayoutSpec(model: model)

        Snapshot.compareWithSnapshot(
            layoutSpec: spec,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_preset() {
        let spec = ChatPresetLayoutSpec(model: "Текст пресета")

        Snapshot.compareWithSnapshot(
            layoutSpec: spec,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_incomingCall_missed() {
        let model = IncomingCallInfoMessageLayoutModel(
            message: "Пропущенный звонок",
            durationString: "10 сек.",
            isSuccessfulCall: false,
            date: Date(timeIntervalSinceReferenceDate: 0),
            color: ColorSchema.Tertiary.red,
            onCallInfoTap: { }
        )
        let spec = IncomingCallInfoMessageLayoutSpec(model: model)

        Snapshot.compareWithSnapshot(
            layoutSpec: spec,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_incomingCall_successful() {
        let model = IncomingCallInfoMessageLayoutModel(
            message: "Входящий звонок",
            durationString: "10 сек.",
            isSuccessfulCall: true,
            date: Date(timeIntervalSinceReferenceDate: 0),
            color: ColorSchema.Background.secondarySurface,
            onCallInfoTap: { }
        )
        let spec = IncomingCallInfoMessageLayoutSpec(model: model)

        Snapshot.compareWithSnapshot(
            layoutSpec: spec,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_outgoingCall_successful() {
        let model = OutgoingCallInfoMessageLayoutModel(
            message: "Исходящий звонок",
            durationString: "10 сек.",
            isSuccessfulCall: true,
            date: Date(timeIntervalSinceReferenceDate: 0),
            color: ColorSchema.Tertiary.blue,
            onCallInfoTap: { }
        )
        let spec = OutgoingCallInfoMessageLayoutSpec(model: model)

        Snapshot.compareWithSnapshot(
            layoutSpec: spec,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_outgoingCall_missed() {
        let model = OutgoingCallInfoMessageLayoutModel(
            message: "Исходящий звонок",
            durationString: "10 сек.",
            isSuccessfulCall: false,
            date: Date(timeIntervalSinceReferenceDate: 0),
            color: ColorSchema.Tertiary.blue,
            onCallInfoTap: { }
        )
        let spec = OutgoingCallInfoMessageLayoutSpec(model: model)

        Snapshot.compareWithSnapshot(
            layoutSpec: spec,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_incomingTextMessage() {
        let model = IncomingTextMessageLayoutModel(
            message: "Входящее сообщение",
            filterLinks: false,
            date: Date(timeIntervalSinceReferenceDate: 0),
            color: ColorSchema.Background.secondarySurface,
            openURL: nil
        )

        let spec = IncomingTextMessageLayoutSpec(model: model)

        Snapshot.compareWithSnapshot(
            layoutSpec: spec,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_outcomingTextMessage_error() {
        let model = Self.makeOutcomingMessageModel(state: .failed)

        let spec = OutgoingTextMessageLayoutSpec(model: model)

        Snapshot.compareWithSnapshot(
            layoutSpec: spec,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_outcomingTextMessage_read() {
        let model = Self.makeOutcomingMessageModel(state: .read)

        let spec = OutgoingTextMessageLayoutSpec(model: model)

        Snapshot.compareWithSnapshot(
            layoutSpec: spec,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_outcomingTextMessage_sending() {
        let model = Self.makeOutcomingMessageModel(state: .sending)

        let spec = OutgoingTextMessageLayoutSpec(model: model)

        Snapshot.compareWithSnapshot(
            layoutSpec: spec,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    func test_outcomingTextMessage_sent() {
        let model = Self.makeOutcomingMessageModel(state: .sent)

        let spec = OutgoingTextMessageLayoutSpec(model: model)

        Snapshot.compareWithSnapshot(
            layoutSpec: spec,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }

    private static func makeOutcomingMessageModel(state: MessageUIState) -> OutgoingTextMessageLayoutModel {
        OutgoingTextMessageLayoutModel(
            message: "Исходящее сообщение",
            filterLinks: false,
            date: Date(timeIntervalSinceReferenceDate: 0),
            color: ColorSchema.Tertiary.blue,
            state: state,
            openURL: nil,
            onFailTap: nil
        )
    }
}
