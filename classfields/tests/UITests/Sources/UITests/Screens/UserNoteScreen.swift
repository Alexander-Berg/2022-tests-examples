import XCTest

final class UserNoteSсreen: BaseSteps, UIRootedElementProvider {
    enum Element {
        case cancel
        case save
        case note
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .cancel: return "cancel_button"
        case .save: return "save_button"
        case .note: return "note"
        }
    }

    func name(of element: Element) -> String {
        switch element {
        case .cancel: return "Кнопка Отменить"
        case .save: return "Кнопка Сохранить"
        case .note: return "Поле ввода заметки"
        }
    }

    static let rootElementID = "userNoteScreen"
    static let rootElementName = "Экран ввода заметки"

    static func findRoot(in app: XCUIApplication, parent: XCUIElement) -> XCUIElement {
        assertRootExists(in: app)
        return app
    }
}

final class UserNoteAlert: BaseSteps, UIRootedElementProvider {
    enum Element {
        case ok
    }

    func identifier(of element: Element) -> String {
        switch element {
        case .ok: return "ok_button"
        }
    }

    func name(of element: Element) -> String {
        switch element {
        case .ok: return "Кнопка ОК"
        }
    }

    static let rootElementID = "userNoteAlert"
    static let rootElementName = "Алерт после нажатия на сохранить"
}
