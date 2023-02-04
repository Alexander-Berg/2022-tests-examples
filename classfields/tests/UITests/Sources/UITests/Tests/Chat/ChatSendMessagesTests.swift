import Foundation
import XCTest
import AutoRuProtoModels

final class ChatSendMessagesTests: BaseTest {
    private static let roomID = "903f0a55b5e2538fef687a332e1a2cf1"
    private static let opponentID = "fbaad471886772bb"
    private static let authorID = "9a9b7b0ca3c4d5a5"

    private static let notificationCenterID = "testChat-28199386-22600910"

    private static let sentMessage = "Тест"

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_canNotSendToNotificationCenter() {
        openChat(roomID: Self.notificationCenterID)
            .should(.inputBar, .be(.hidden))
    }

    func test_sendTypingStatus() {
        let typingRequest = api.chat.room.id(Self.roomID).typing
            .put
            .expect()

        openChat()
            .log("Пишем текст и проверяем, что уходит запрос на обновление статуса")
            .focus(on: .inputBar, ofType: .chatInputBar) { input in
                input.type("Test", in: .text)
            }
            .wait(for: [typingRequest])
    }

    func test_sendTextMessage() {
        mockMessageResponse()

        let sentExpectation = api.chat.message
            .post
            .expect { req, _ in
                req.roomID == Self.roomID && req.payload.value == Self.sentMessage ? .ok : .fail(reason: nil)
            }

        openChat()
            .should(.message(.byIndexFromTop(1)), .be(.hidden))
            .focus(on: .inputBar, ofType: .chatInputBar) { input in
                input
                    .validateSnapshot(snapshotId: "input_bar_empty")
                    .type(Self.sentMessage)
                    .validateSnapshot(snapshotId: "input_bar_text")
                    .tap(.send)
            }
            .wait(for: [sentExpectation])
            .focus(on: .message(.byIndexFromTop(1)), ofType: .chatOutcomingMessageCell) { message in
                message
                    .should(.text, .match(Self.sentMessage))
                    .should(.status(.sent), .exist)
                    .should(.status(.read), .be(.hidden))
                    .should(.status(.sending), .be(.hidden))
            }
    }

    func test_sendMessage_failedCanResend() {
        api.chat.message
            .post
            .error(status: ._500, mock: .model(Auto_Api_SendMessageRequest()))

        openChat()
            .step("Проверяем, что в чате только 1 сообщение") { screen in
                screen.should(.message(.byIndexFromTop(1)), .be(.hidden))
            }
            .focus(on: .inputBar, ofType: .chatInputBar) { input in
                input
                    .type(Self.sentMessage)
                    .tap(.send)
            }
            .focus(on: .message(.byIndexFromTop(1)), ofType: .chatOutcomingMessageCell) { message in
                message.tap(.errorButton)
            }
            .do { mockMessageResponse() }
            .should(provider: .systemAlert, .exist)
            .focus { alert in
                alert.step("Проверяем, что перезапросили отправку") {
                    let expectation = api.chat.message.post.expect()

                    alert
                        .tap(.button("Переслать"))
                        .wait(for: [expectation])
                }
            }
            .should(provider: .systemAlert, .be(.hidden))
            .focus(on: .message(.byIndexFromTop(1)), ofType: .chatOutcomingMessageCell) { message in
                message
                    .should(.text, .match(Self.sentMessage))
                    .should(.status(.sent), .exist)
                    .should(.status(.read), .be(.hidden))
                    .should(.status(.sending), .be(.hidden))
            }
    }

    func test_sendMessage_failedCanDelete() {
        api.chat.message
            .post
            .error(status: ._500, mock: .model(Auto_Api_SendMessageRequest()))

        openChat()
            .step("Проверяем, что в чате только 1 сообщение") { screen in
                screen.should(.message(.byIndexFromTop(1)), .be(.hidden))
            }
            .focus(on: .inputBar, ofType: .chatInputBar) { input in
                input
                    .type(Self.sentMessage)
                    .tap(.send)
            }
            .step("Проверяем, что появилось второе и то, какое нужно") { screen in
                screen.focus(on: .message(.byIndexFromTop(1)), ofType: .chatOutcomingMessageCell) { message in
                    message
                        .should(.text, .match(Self.sentMessage))
                        .should(.status(.read), .be(.hidden))
                        .should(.status(.sending), .be(.hidden))
                        .should(.status(.sent), .be(.hidden))
                        .tap(.errorButton)
                }
            }
            .should(provider: .systemAlert, .exist)
            .focus { alert in
                alert.tap(.button("Удалить"))
            }
            .should(provider: .systemAlert, .be(.hidden))
            .step("Проверяем, что в чате снова только 1 сообщение") { screen in
                screen.should(.message(.byIndexFromTop(1)), .be(.hidden))
            }
    }

    private func openChat(roomID: String = ChatSendMessagesTests.roomID) -> ChatScreen_ {
        launch(on: .mainScreen) { screen in
            screen.toggle(to: TabBarItem.chats)

            return screen
                .should(provider: .chatsScreen, .exist)
                .focus { screen in
                    screen.tap(.chatRoom(id: roomID))
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

        api.chat.room.id(Self.roomID).typing
            .put
            .ok(mock: .model())

        api.chat.room
            .get
            .ok(mock: .file("chat_rooms"))
    }

    private func mockMessageResponse() {
        api.chat.message
            .post
            .ok(
                mock: .dynamic { _, req in
                    Self.makeMessageResponse(
                        uid: req.providedID,
                        author: Self.authorID,
                        room: Self.roomID,
                        text: "Тест"
                    )
                }
            )
    }

    private static func makeMessageResponse(uid: String, author: String, room: String, text: String) -> Auto_Api_MessageResponse {
        .with { model in
            model.status = .success
            model.message = .with { message in
                message.id = UUID().uuidString
                message.providedID = uid
                message.roomID = room
                message.author = author
                message.created = .init(date: Date())

                message.payload = .with {
                    $0.contentType = .textPlain
                    $0.value = text
                }
            }
        }
    }
}
