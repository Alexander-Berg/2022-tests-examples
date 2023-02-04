import XCTest

final class UserNoteCell: BaseSteps, UIElementProvider {
    enum Element {
        case userNoteText
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .userNoteText: return "user_note_text"
        }
    }

    func name(of element: Element) -> String {
        switch element {
        case .userNoteText: return "Текст заметки пользователя"
        }
    }
}
