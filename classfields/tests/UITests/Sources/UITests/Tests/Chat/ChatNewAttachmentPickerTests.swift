final class ChatAttachmentPickerTests: BaseTest {
    private let roomID = "410c6572cdd68ac9f2778a4ae57924b5"
    override func setUp() {
        super.setUp()
        setupServer()
    }

    private func setupServer() {
        mocker
            .mock_base()
            .mock_user()
            .setForceLoginMode(.forceLoggedIn)
            .startMock()
    }

    func test_openAttachmentPicker() {
        launchWithChatScreen()
            .focus(on: .inputBar, ofType: .chatInputBar) { element in
                element.tap(.attach)
            }
            .handleSystemAlertIfNeeded()
            .should(provider: .attachmentPicker, .exist)
            .focus { screen in
                screen.should(.location, .exist)
                screen.should(.photo, .exist)
                screen.tap(.photo)
            }
            .should(provider: .phPicker, .exist)
    }

    func test_sendPhotoToTechChatFromGallery() {
        let expectation = api.chat.message.bootstrap
            .get(
                parameters: [
                    .providedId("localID"),
                    .roomId(roomID),
                    .withTechSupportUpload(false)
                ]
            )
            .expect()

        launchWithChatScreen()
            .focus(on: .inputBar, ofType: .chatInputBar) { element in
                element.tap(.attach)
            }
            .handleSystemAlertIfNeeded()
            .should(provider: .attachmentPicker, .exist)
            .focus { screen in
                screen
                    .tap(.systemImage(0))
                    .tap(.send)
                    .wait(for: [expectation])
            }
    }

    func test_sendPhotoToTechChatFromCamera() {
        let expectation = api.chat.message.bootstrap
            .get(
                parameters: [
                    .providedId("localID"),
                    .roomId(roomID),
                    .withTechSupportUpload(true)
                ]
            )
            .expect()

        launchWithChatScreen()
            .focus(on: .inputBar, ofType: .chatInputBar) { element in
                element.tap(.attach)
            }
            .handleSystemAlertIfNeeded()
            .should(provider: .attachmentPicker, .exist)
            .focus { screen in
                screen
                    .tap(.camera)
                    .wait(for: [expectation])
            }
    }

    private func launchWithChatScreen() -> ChatScreen_ {
        let options = AppLaunchOptions(
            launchType: .deeplink("https://auto.ru/chat/"),
            overrideAppSettings: ["sendImageChatTestEnable": true]
        )
        mocker
            .mock_getChatRoom()

        return launch(on: .chatsScreen, options: options) { screen in
            screen
                .tap(.chatRoom(id: roomID))
                .should(provider: .chatScreen, .exist)
        }
    }
}
