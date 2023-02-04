import XCTest
import Snapshots

final class ChatsSteps: BaseSteps {
    func onChatsScreen() -> ChatsScreen {
        return self.baseScreen.on(screen: ChatsScreen.self)
    }

    @discardableResult
    func tapOnChatRoom(id: String) -> ChatSteps {
        Step("Тапаем на чат c id \(id) списка чатов") {
            self.onChatsScreen().chatCell(id: id).tap()
        }

        return ChatSteps(context: self.context)
    }

    @discardableResult
    func tapOnChatRoom(index: Int) -> ChatSteps {
        Step("Тапаем на чат в позиции \(index) списка чатов") {
            self.onChatsScreen().chatCell(index: index).tap()
        }

        return ChatSteps(context: self.context)
    }
}
