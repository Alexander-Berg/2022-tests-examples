import Foundation
import XCTest
import Snapshots
import AutoRuAppearance
@testable import AutoRuChat
import AutoRuColorSchema

final class ChatDateSeparatorTests: BaseUnitTest {
    private static let oneDay: TimeInterval = 24 * 60 * 60

    func test_dateSeparatorBetweenMessages() {
        let factory = MessagesListItemsFactory()

        let date = Date(timeIntervalSinceReferenceDate: 0)

        let messages = [
            ChatMessageEntity(
                uid: "-1",
                serverId: UUID().uuidString,
                chatId: "0",
                userId: "1",
                date: date.addingTimeInterval(-Self.oneDay),
                status: .success,
                contentType: .plainText,
                content: "Test",
                properties: nil
            ),
            ChatMessageEntity(
                uid: "-2",
                serverId: UUID().uuidString,
                chatId: "0",
                userId: "1",
                date: date.addingTimeInterval(-2 * Self.oneDay),
                status: .success,
                contentType: .plainText,
                content: "Test",
                properties: nil
            ),
            ChatMessageEntity(
                uid: "-3",
                serverId: UUID().uuidString,
                chatId: "0",
                userId: "1",
                date: date.addingTimeInterval(-3 * Self.oneDay),
                status: .success,
                contentType: .plainText,
                content: "Test",
                properties: nil
            ),
        ]

        let items = factory.makeListItemsFrom(messages: .init(messages), roomReadDates: nil).0

        let givenIDs = Array(items.map { $0.diffId }.reversed().prefix(6))
        let testIDs = [
            "29 декабря 2000",
            "message_cell_-3",
            "30 декабря 2000",
            "message_cell_-2",
            "31 декабря 2000",
            "message_cell_-1"
        ]
        XCTAssertEqual(givenIDs, testIDs)
    }

    func test_noDateSeparatorBetweenMessages() {
        let factory = MessagesListItemsFactory()

        let date = Date(timeIntervalSinceReferenceDate: 0)

        let messages = [
            ChatMessageEntity(
                uid: "2",
                serverId: UUID().uuidString,
                chatId: "0",
                userId: "1",
                date: date.addingTimeInterval(10),
                status: .success,
                contentType: .plainText,
                content: "Test",
                properties: nil
            ),
            ChatMessageEntity(
                uid: "1",
                serverId: UUID().uuidString,
                chatId: "0",
                userId: "1",
                date: date,
                status: .success,
                contentType: .plainText,
                content: "Test",
                properties: nil
            )
        ]

        let items = factory.makeListItemsFrom(messages: .init(messages), roomReadDates: nil).0

        let givenIDs = Array(items.map { $0.diffId }.reversed().prefix(3))
        let testIDs = [
            "1 января 2001",
            "message_cell_1",
            "message_cell_2",
        ]
        XCTAssertEqual(givenIDs, testIDs)
    }

    func test_dateToday() {
        let factory = MessagesListItemsFactory()

        let messages = [
            ChatMessageEntity(
                uid: "0",
                serverId: UUID().uuidString,
                chatId: "0",
                userId: "1",
                date: Date(),
                status: .success,
                contentType: .plainText,
                content: "Test",
                properties: nil
            )
        ]

        let items = factory.makeListItemsFrom(messages: .init(messages), roomReadDates: nil).0

        let date = Array(items.map { $0.diffId }.reversed())[0]
        XCTAssertEqual(date, "Сегодня")
    }

    func test_dateYesterday() {
        let factory = MessagesListItemsFactory()

        let messages = [
            ChatMessageEntity(
                uid: "0",
                serverId: UUID().uuidString,
                chatId: "0",
                userId: "1",
                date: Date().addingTimeInterval(-Self.oneDay),
                status: .success,
                contentType: .plainText,
                content: "Test",
                properties: nil
            )
        ]

        let items = factory.makeListItemsFrom(messages: .init(messages), roomReadDates: nil).0

        let date = Array(items.map { $0.diffId }.reversed())[0]
        XCTAssertEqual(date, "Вчера")
    }

    func test_separatorAppearance() {
        let spec = DialogDateSeparatorLayoutSpec(model: "27 января 2022")

        Snapshot.compareWithSnapshot(
            layoutSpec: spec,
            maxWidth: DeviceWidth.iPhone11,
            backgroundColor: ColorSchema.Background.surface
        )
    }
}
