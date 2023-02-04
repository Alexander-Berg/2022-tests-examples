import Foundation
import XCTest
import AutoRuProtoModels

final class ChatMessagesTests: BaseTest {
    private static let roomID = "903f0a55b5e2538fef687a332e1a2cf1"
    private static let opponentID = "fbaad471886772bb"

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_openChatRequest() {
        let openExpectaition = api.chat.room.id(Self.roomID).open
            .put
            .expect()

        openChat()
            .wait(for: [openExpectaition], timeout: 2)
    }

    func test_openChatOnUnreadMessages() {
        var mockMessages: [Vertis_Chat_Message] = []
        for i in 0..<30 {
            mockMessages.append(Self.makeChatMessage(time: 0 + Double(i), text: "прочитанное сообщение"))
        }

        let firstUnreadMessage = Self.makeChatMessage(
            time: 101,
            text: "первое непрочитанное сообщение"
        )
        mockMessages.append(firstUnreadMessage)

        for i in 0..<30 {
            mockMessages.append(Self.makeChatMessage(time: 102 + Double(i), text: "ещё непрочитанное сообщение"))
        }

        api.chat.room
            .get
            .ok(
                mock: .file("chat_rooms") { response in
                    guard var unreadRoom = try? XCTUnwrap(response.rooms.first(where: { $0.id == Self.roomID })),
                          let lastMessage = try? XCTUnwrap(mockMessages.last) else { return }

                    unreadRoom.hasUnreadMessages_p = true
                    unreadRoom.users[0].roomLastRead = .init(date: Date(timeIntervalSince1970: 100))
                    unreadRoom.users[1].roomLastRead = .init(date: Date(timeIntervalSince1970: 100))
                    unreadRoom.lastMessage = lastMessage

                    response.rooms = [unreadRoom]
                }
            )

        api.chat.message
            .get(parameters: .wildcard)
            .ok(mock: .model(.init()) { $0.messages = mockMessages })

        let makeUnread = api.chat.message.unread
            .delete(parameters: [.roomId(Self.roomID)])
            .expect()

        openChat()
            .focus(on: .message(.byID(firstUnreadMessage.providedID)), ofType: .chatIncomingMessageCell) { message in
                message.validateSnapshot()
            }
            .wait(for: [makeUnread])
    }

    func test_filterSpamMessages() {
        let message = Self.makeChatMessage(
            time: Date(timeIntervalSinceReferenceDate: 0).timeIntervalSince1970,
            text: "spam"
        )

        api.chat.message
            .get(parameters: .wildcard)
            .ok(mock: .model(.init()) { $0.messages = [message] })

        api.chat.message.spam
            .get(parameters: .wildcard)
            .ok(mock: .model(.init()) { $0.messages = [message] })

        let checkSpam = api.chat.message.spam
            .get(parameters: [.roomId(Self.roomID)])
            .expect()

        openChat()
            .wait(for: [checkSpam])
            .should(.message(.byIndexFromTop(1)), .be(.hidden))
    }

    func test_messagesPagination() {
        // Пояснения для будущих поколений. Этот тест был жертвой неудачного перепиливания после инверсии порядка сообщений. Начальное сообщение в чате имеет фиксированную дату из мока, поэтому для проверки подгрузки более ранних нужны соответствующие даты в них. wait требуется, чтобы подгруженная первая порция сообщений успела отобразиться, после этого можно свайпать для загрузки ещё более ранних.
        let initialDate: Double = 1000
        let firstBatch = (0..<10).map {
            Self.makeChatMessage(time: initialDate - Double($0), text: "test")
        }

        let firstBatchEarliestMessage = firstBatch.last!

        let secondBatch = (0..<10).map {
            Self.makeChatMessage(time: initialDate - Double($0) - 200, text: "test")
        }

        api.chat.message
            .get(parameters: .wildcard)
            .ok(mock: .model(.init()) { $0.messages = firstBatch })

        let initialLoading = api.chat.message
            .get(parameters: [.roomId(Self.roomID), .count(200), .asc(false)])
            .expect()

        openChat()
            .log("Проверяем начальный запрос на 200 сообщений")
            .wait(for: [initialLoading])
            .step("Свайпаем дальше и проверяем новый запрос на 200") { screen in
                api.chat.message
                    .get(parameters: .wildcard)
                    .ok(mock: .model(.init()) { $0.messages = secondBatch })

                let nextLoading = api.chat.message
                    .get(
                        parameters: [
                            .roomId(Self.roomID),
                            .count(200),
                            .asc(false),
                            .from(firstBatchEarliestMessage.id)
                        ]
                    )
                    .expect()

                screen
                    .wait(for: 2)
                    .swipe(.down)
                    .swipe(.down)
                    .wait(for: [nextLoading])
            }
    }

    func test_buyReportWidget_showAndClose() {
        api.chat.message
            .get(parameters: .wildcard)
            .ok(
                mock: .model(.init()) { response in
                    response.messages = [
                        Self.makeChatMessage(
                            time: Date().timeIntervalSince1970 - 10,
                            text: "госномер а111аа77"
                        )
                    ]
                }
            )

        openChat()
            .focus(on: .message(.byIndexFromTop(2)), ofType: .chatReportWidgetCell) { message in
                message
                    .should(.text, .match("Купите отчет по этому автомобилю, чтобы узнать о нем всё"))
                    .tap(.moreButton)
            }
            .should(provider: .systemAlert, .exist)
            .focus { alert in
                alert.tap(.button("Удалить"))
            }
            .should(provider: .chatScreen, .exist)
            .log("Проверяем, что удалилось сообщение и в чате теперь их только 2")
            .should(.message(.byIndexFromTop(2)), .be(.hidden))
    }

    func test_antifraudWidget_showAndClose() {
        api.chat.message
            .get(parameters: .wildcard)
            .ok(
                mock: .model(.init()) { response in
                    response.messages = [
                        Self.makeChatMessage(
                            time: Date().timeIntervalSince1970 - 10,
                            text: "переведи денег"
                        )
                    ]
                }
            )

        openChat()
            .focus(on: .message(.byIndexFromTop(2)), ofType: .chatAntifraudWidgetCell) { message in
                let text = "Будьте внимательны, приобретая транспортное средство, никогда не отправляйте предоплату"

                message
                    .should(.text, .contain(text))
                    .tap(.moreButton)
            }
            .should(provider: .systemAlert, .exist)
            .focus { alert in
                alert.tap(.button("Удалить"))
            }
            .should(provider: .chatScreen, .exist)
            .log("Проверяем, что удалилось сообщение и в чате теперь их только 2")
            .should(.message(.byIndexFromTop(2)), .be(.hidden))
    }

    func test_copyTextByLongTap() {
        api.chat.message
            .get(parameters: .wildcard)
            .ok(
                mock: .model(.init()) { response in
                    response.messages = [
                        Self.makeChatMessage(
                            time: Date().timeIntervalSince1970 - 10,
                            text: "Текст сообщения в чате"
                        )
                    ]
                }
            )

        openChat()
            .focus(on: .message(.byIndexFromTop(1)), ofType: .chatIncomingMessageCell) { message in
                message.longTap(.bubble)
            }
            .wait(for: 1)
            .should(provider: .contextMenuTooltip, .exist)
            .focus { menu in
                menu.tap(.copy)
            }
            .wait(for: 1)
            .step("Проверяем, что в буфере обмена лежит нужный текст") { _ in
                XCTAssertEqual(UIPasteboard.general.string ?? "", "Текст сообщения в чате")
            }
    }

    @discardableResult
    private func openChat() -> ChatScreen_ {
        launch(on: .mainScreen) { screen in
            screen.toggle(to: TabBarItem.chats)

            return screen
                .should(provider: .chatsScreen, .exist)
                .focus { screen in
                    screen.tap(.chatRoom(id: Self.roomID))
                }
                .should(provider: .chatScreen, .exist)
        }
    }

    private func setupServer() {
        mocker
            .setForceLoginMode(.forceLoggedIn)
            .mock_base()
            .mock_user()
            .startMock()

        api.carfax.offer.category(.cars).offerID("1097424592-d15ee386").raw
            .get(parameters: .wildcard)
            .ok(
                mock: .model(.init()) { report in
                    report.report.reportType = .freePreview
                    report.billing.servicePrices = [
                        .with { price in
                            price.service = "offers-history-reports"
                            price.counter = 1
                            price.originalPrice = 100
                            price.price = 100
                        }
                    ]
                }
            )

        api.offer.category(.cars).offerID("1097424592-d15ee386").get
            .ok(
                mock: .model(.init()) { response in
                    response.offer.sellerType = .private
                }
            )

        api.chat.room
            .get
            .ok(mock: .file("chat_rooms"))

        api.chat.message.unread
            .delete(parameters: .wildcard)
            .ok(mock: .model())
    }

    private static func makeChatMessage(time: TimeInterval, text: String) -> Vertis_Chat_Message {
        .with { message in
            message.roomID = Self.roomID
            message.id = UUID().uuidString
            message.providedID = UUID().uuidString
            message.author = Self.opponentID
            message.created = .init(timeIntervalSince1970: time)
            message.payload = .with { payload in
                payload.contentType = .textPlain
                payload.value = text
            }
        }
    }
}
