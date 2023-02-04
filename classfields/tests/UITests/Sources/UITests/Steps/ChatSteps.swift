import XCTest
import Snapshots

final class ChatSteps: BaseSteps {
    func onChatScreen() -> ChatScreen {
        return self.baseScreen.on(screen: ChatScreen.self)
    }

    @discardableResult
    func tapBackButton() -> Self {
        self.onChatScreen().backButton.tap()
        return self
    }
}
