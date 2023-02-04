import Foundation
import XCTest
import AutoRuProtoModels

final class ChatUserStatusTests: BaseTest {
    private static let roomID = "903f0a55b5e2538fef687a332e1a2cf1"

    override func setUp() {
        super.setUp()
        setupServer()
    }

    func test_checkUserPanel() {
        openChat()
            .focus(on: .userPanel, ofType: .chatUserPanel) { element in
                element.should(.username, .match("Автосалон в  Антарктиде"))
            }
            .should(.callButton, .exist)
            .should(.moreButton, .exist)
    }

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

        api.chat.room.id(Self.roomID).typing
            .put
            .ok(mock: .model())

        api.chat.room
            .get
            .ok(mock: .file("chat_rooms"))
    }
}
